
package com.android.systemui.statusbar.toggles;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import static com.android.internal.util.gummy.GummyConstants.*;
import com.android.systemui.R;
import com.android.systemui.gummy.GummyAction;

public class TorchToggle extends StatefulToggle {
    TorchObserver mObserver = null;

    @Override
    public void init(Context c, int style) {
        super.init(c, style);
        mObserver = new TorchObserver(mHandler);
        mObserver.observe();
    }

    @Override
    protected void cleanup() {
        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
        super.cleanup();
    }

    @Override
    protected void doEnable() {
        GummyAction.launchAction(mContext, GummyConstant.ACTION_TORCH.value());
    }

    @Override
    protected void doDisable() {
        GummyAction.launchAction(mContext, GummyConstant.ACTION_TORCH.value());
    }

    @Override
    protected void updateView() {
        boolean enabled = Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.TORCH_STATE, false);
        setIcon(enabled
                ? R.drawable.ic_qs_torch_on
                : R.drawable.ic_qs_torch_off);
        setLabel(enabled
                ? R.string.quick_settings_torch_on_label
                : R.string.quick_settings_torch_off_label);
        updateCurrentState(enabled ? State.ENABLED : State.DISABLED);
        super.updateView();
    }

    protected class TorchObserver extends ContentObserver {
        TorchObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.TORCH_STATE), false, this);
            onChange(false);
        }

        @Override
        public void onChange(boolean selfChange) {
            scheduleViewUpdate();
        }
    }

}
