package com.liskovsoft.smartyoutubetv2.common.app.amplitude;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amplitude.api.Amplitude;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

public class Analytics {
    private static final String TAG = Analytics.class.getSimpleName();
    private static FirebaseAnalytics sFA;

    public static void init(Context context) {
        sFA = FirebaseAnalytics.getInstance(context);
    }

    public static void initAmplitude(Context context, Application application) {
        Amplitude.getInstance()
                .initialize(context, "906c33b6848bea9eecbd50b30acae03c")
                .enableForegroundTracking(application)
                .setDeviceId(getSerialNumber());
    }

    public static String getSerialNumber() {
        // UHDX
        if (Build.SERIAL.isEmpty() || Build.SERIAL.equals("unknown")) {
            return getSystemProperty("persist.sys.sen5.serialno");
        }
        // Eltex, UHD
        return Build.SERIAL;
    }

    private static String getSystemProperty(String key) {
        String value = null;

        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }
    //----------------------------ОБЩИЕ СОБЫТИЯ-----------------------------------------------------

    //Офферскрин
    private static final String YOUTUBE_LAUNCH_VIDEO = "YOUTUBE_LAUNCH_VIDEO";
    private static final String YOUTUBE_SEARCH_VIDEO = "YOUTUBE_SEARCH_VIDEO";

    //---------------------------PARAM--------------------------------------------------------------

    private static final String PARAM_APP = "app";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_STUDIO = "studio";
    private static final String PARAM_IS_LIVE = "isLive";
    private static final String PARAM_VIDEO_URL = "videoUrl";
    private static final String PARAM_BADGE = "badge";

    private static final String PARAM_QUERY = "query";


    public static void launchVideo(@Nullable String appInitiator,
                                   @Nullable String title,
                                   @Nullable String studio,
                                   @Nullable Boolean isLive,
                                   @Nullable String videoUrl,
                                   @Nullable String badge) {
        JSONObject jsonObject = new JSONObject();
        try {
            Log.d(TAG, "YOUTUBE_LAUNCH_VIDEO: \n" +
                    "\n [" + PARAM_APP + "]        = [" + appInitiator + "]" +
                    "\n [" + PARAM_TITLE + "]      = [" + title + "]" +
                    "\n [" + PARAM_STUDIO + "]     = [" + studio + "]" +
                    "\n [" + PARAM_IS_LIVE + "]    = [" + isLive + "]" +
                    "\n [" + PARAM_VIDEO_URL + "]  = [" + videoUrl + "]" +
                    "\n [" + PARAM_BADGE + "]      = [" + badge + "]"
            );

            jsonObject.put(PARAM_APP, appInitiator);
            jsonObject.put(PARAM_TITLE, title);
            jsonObject.put(PARAM_STUDIO, studio);
            jsonObject.put(PARAM_IS_LIVE, isLive);
            jsonObject.put(PARAM_VIDEO_URL, videoUrl);
            jsonObject.put(PARAM_BADGE, badge);
        } catch (JSONException e) {
            Log.d(TAG, "YOUTUBE_LAUNCH_VIDEO: error " + e.getMessage());
            e.printStackTrace();
        }

        Amplitude.getInstance().logEvent(YOUTUBE_LAUNCH_VIDEO, jsonObject);
    }

    public static void searchVideos(@Nullable String appInitiator,
                                    @Nullable String searchQuery) {
        JSONObject jsonObject = new JSONObject();
        try {
            Log.d(TAG, "YOUTUBE_LAUNCH_VIDEO: \n" +
                    "\n [" + PARAM_APP + "]        = [" + appInitiator + "]" +
                    "\n [" + PARAM_QUERY + "]      = [" + searchQuery + "]"
            );

            jsonObject.put(PARAM_APP, appInitiator);
            jsonObject.put(PARAM_QUERY, searchQuery);
        } catch (JSONException e) {
            Log.d(TAG, "YOUTUBE_LAUNCH_VIDEO: error " + e.getMessage());
            e.printStackTrace();
        }

        Amplitude.getInstance().logEvent(YOUTUBE_SEARCH_VIDEO, jsonObject);
    }
}