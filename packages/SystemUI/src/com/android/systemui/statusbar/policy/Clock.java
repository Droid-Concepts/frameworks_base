/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.Handler;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import libcore.icu.LocaleData;

import com.android.internal.R;

/**
 * Digital clock for the status bar.
 */
public class Clock extends TextView {
    private boolean mAttached;
    private Calendar mCalendar;
    private String mClockFormatString;
    private SimpleDateFormat mClockFormat;
    private Locale mLocale;

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    protected int mAmPmStyle = AM_PM_STYLE_GONE;

    public static final int WEEKDAY_STYLE_GONE   = 0;
    public static final int WEEKDAY_STYLE_SMALL  = 1;
    public static final int WEEKDAY_STYLE_NORMAL = 2;

    protected int mWeekdayStyle = WEEKDAY_STYLE_GONE;

    public static final int STYLE_HIDE_CLOCK     = 0;
    public static final int STYLE_CLOCK_RIGHT    = 1;
    public static final int STYLE_CLOCK_CENTER   = 2;

    protected int mClockStyle = STYLE_CLOCK_RIGHT;

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Clock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_USER_SWITCHED);

            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = Calendar.getInstance(TimeZone.getDefault());

        // Make sure we update to the current time
        //no need to updateClock here, since we call updateSettings() which has updateClock();
        //updateClock();
        
        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
                if (mClockFormat != null) {
                    mClockFormat.setTimeZone(mCalendar.getTimeZone());
                }
            } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                final Locale newLocale = getResources().getConfiguration().locale;
                if (! newLocale.equals(mLocale)) {
                    mLocale = newLocale;
                    mClockFormatString = ""; // force refresh
                }
            }
            updateClock();
        }
    };

    final void updateClock() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        setText(getSmallTime());
    }

    private final CharSequence getSmallTime() {
        Context context = getContext();
        boolean b24 = DateFormat.is24HourFormat(context);
        LocaleData d = LocaleData.get(context.getResources().getConfiguration().locale);

        final char MAGIC1 = '\uEF00';
        final char MAGIC2 = '\uEF01';

        SimpleDateFormat sdf;
        String format = b24 ? d.timeFormat24 : d.timeFormat12;
        if (!format.equals(mClockFormatString)) {
            mClockFormat = sdf = new SimpleDateFormat(format);
            mClockFormatString = format;
        } else {
            sdf = mClockFormat;
        }

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        String todayIs = null;
        String result = sdf.format(mCalendar.getTime());

        if (mWeekdayStyle != WEEKDAY_STYLE_GONE) {
            todayIs = whatDay(day);
            result = todayIs + result;
        }

        SpannableStringBuilder formatted = new SpannableStringBuilder(result);

        if (!b24) {
            if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
                if (mAmPmStyle == AM_PM_STYLE_GONE) {
                    formatted.delete(result.length() - 3, result.length());
                } else {
                    if (mAmPmStyle == AM_PM_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, result.length() - 3, result.length(),
                                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                }
            }
        }
        if (mWeekdayStyle != WEEKDAY_STYLE_NORMAL) {
            if (todayIs != null) {
                if (mWeekdayStyle == WEEKDAY_STYLE_GONE) {
                    formatted.delete(0, 4);
                } else {
                    if (mWeekdayStyle == WEEKDAY_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, 0, 4,
                                          Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                }
            }
        }
        return formatted;
    }

    /**
     * pull the int given by DAY_OF_WEEK into a day string
     */
    private String whatDay(int today) {
    	String todayIs = null;
    	switch (today) {
    	case 1:
			todayIs = "SUN ";
			break;
		case 2:
			todayIs = "MON ";
			break;
		case 3:
			todayIs = "TUE ";
			break;
		case 4:
			todayIs = "WED ";
			break;
		case 5:
			todayIs = "THU ";
			break;
		case 6:
			todayIs = "FRI ";
			break;
		case 7:
			todayIs = "SAT ";
			break;
    	}
    		
    	return todayIs;
    }
    
    protected class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE),
                    false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUSBAR_CLOCK_STYLE), false,
                    this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUSBAR_CLOCK_WEEKDAY), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mAmPmStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE, AM_PM_STYLE_GONE);   
        mClockStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_STYLE, STYLE_CLOCK_RIGHT);
        mWeekdayStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_WEEKDAY, WEEKDAY_STYLE_GONE);
        
        updateClockVisibility();
        updateClock();
    }

    protected void updateClockVisibility() {
        if (mClockStyle == STYLE_CLOCK_RIGHT)
            setVisibility(View.VISIBLE);
        else
            setVisibility(View.GONE);
    }

    private void collapseStartActivity(Intent what) {
        // collapse status bar
        StatusBarManager statusBarManager = (StatusBarManager) getContext().getSystemService(
                Context.STATUS_BAR_SERVICE);
        statusBarManager.collapsePanels();

        // dismiss keyguard in case it was active and no passcode set
        try {
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (Exception ex) {
            // no action needed here
        }

        // start activity
        what.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(what);
    }

}

