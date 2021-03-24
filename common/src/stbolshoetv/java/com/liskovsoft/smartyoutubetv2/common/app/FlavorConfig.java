package com.liskovsoft.smartyoutubetv2.common.app;

import android.os.Build;

public class FlavorConfig {
    public static class Player {
        public final static int MAX_VIDEO_WIDTH = (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT ? 1280 :
                Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1 ? 1280 : 3840);
    }

    public static class AppPrefs {
        public final static float VIDEO_GRID_SCALE = 1.35f;
        public final static int COLOR_SCHEME_INDEX = 2;
    }
}
