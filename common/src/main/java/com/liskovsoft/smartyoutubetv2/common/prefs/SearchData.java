package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import com.liskovsoft.sharedutils.helpers.Helpers;

public class SearchData {
    private static final String SEARCH_DATA = "search_data";
    @SuppressLint("StaticFieldLeak")
    private static SearchData sInstance;
    private final Context mContext;
    private final AppPrefs mAppPrefs;
    private boolean mIsInstantVoiceSearchEnabled;

    private SearchData(Context context) {
        mContext = context;
        mAppPrefs = AppPrefs.instance(mContext);
        restoreData();
    }

    public static SearchData instance(Context context) {
        if (sInstance == null) {
            sInstance = new SearchData(context.getApplicationContext());
        }

        return sInstance;
    }

    public void setInstantVoiceSearchEnabled(boolean enabled) {
        mIsInstantVoiceSearchEnabled = enabled;
        persistData();
    }

    public boolean isInstantVoiceSearchEnabled() {
        return mIsInstantVoiceSearchEnabled;
    }

    private void restoreData() {
        String data = mAppPrefs.getData(SEARCH_DATA);

        String[] split = Helpers.splitObjectLegacy(data);

        mIsInstantVoiceSearchEnabled = Helpers.parseBoolean(split, 0, false);
    }

    private void persistData() {
        mAppPrefs.setData(SEARCH_DATA, Helpers.mergeObject(mIsInstantVoiceSearchEnabled));
    }
}
