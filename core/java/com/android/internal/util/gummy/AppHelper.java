/*
* Copyright (C) 2013 Gummy
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.internal.util.gummy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import java.net.URISyntaxException;

public class AppHelper {

    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    public static String getProperSummary(PackageManager pm,
            Resources settingsResources, String action, String values, String entries) {

        if (pm == null || settingsResources == null || action == null) {
            return null;
        }

        if (values != null && entries != null) {
            int resIdEntries = -1;
            int resIdValues = -1;

            resIdEntries = settingsResources.getIdentifier(
                        SETTINGS_METADATA_NAME + ":array/" + entries, null, null);

            resIdValues = settingsResources.getIdentifier(
                        SETTINGS_METADATA_NAME + ":array/" + values, null, null);

            if (resIdEntries > 0 && resIdValues > 0) {
                try {
                    String[] entriesArray = settingsResources.getStringArray(resIdEntries);
                    String[] valuesArray = settingsResources.getStringArray(resIdValues);
                    for (int i = 0; i < valuesArray.length; i++) {
                        if (action.equals(valuesArray[i])) {
                            return entriesArray[i];
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return getFriendlyNameForUri(pm, action);
    }

    private static String getFriendlyActivityName(
            PackageManager pm, Intent intent, boolean labelOnly) {
        ActivityInfo ai = intent.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
        String friendlyName = null;

        if (ai != null) {
            friendlyName = ai.loadLabel(pm).toString();
            if (friendlyName == null && !labelOnly) {
                friendlyName = ai.name;
            }
        }

        return friendlyName != null || labelOnly ? friendlyName : intent.toUri(0);
    }

    private static String getFriendlyShortcutName(PackageManager pm, Intent intent) {
        String activityName = getFriendlyActivityName(pm, intent, true);
        String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (activityName != null && name != null) {
            return activityName + ": " + name;
        }
        return name != null ? name : intent.toUri(0);
    }

    private static String getFriendlyNameForUri(PackageManager pm, String uri) {
        if (uri == null || uri.startsWith("**")) {
            return null;
        }

        try {
            Intent intent = Intent.parseUri(uri, 0);
            if (Intent.ACTION_MAIN.equals(intent.getAction())) {
                return getFriendlyActivityName(pm, intent, false);
            }
            return getFriendlyShortcutName(pm, intent);
        } catch (URISyntaxException e) {
        }

        return uri;
    }

}
