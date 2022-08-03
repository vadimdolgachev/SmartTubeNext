package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import com.liskovsoft.sharedutils.helpers.Helpers;

public class SearchData {
    public static final int SPEECH_RECOGNIZER_SYSTEM = 0;
    public static final int SPEECH_RECOGNIZER_EXTERNAL_1 = 1;
    public static final int SPEECH_RECOGNIZER_EXTERNAL_2 = 2;
    private static final String SEARCH_DATA = "search_data";
    @SuppressLint("StaticFieldLeak")
    private static SearchData sInstance;
    private final AppPrefs mAppPrefs;
    private boolean mIsInstantVoiceSearchEnabled;
    private int mSearchOptions;
    private boolean mIsFocusOnResultsEnabled;
    private boolean mIsKeyboardAutoShowEnabled;
    private boolean mIsBackgroundPlaybackEnabled;
    private int mSpeechRecognizerType;

    private SearchData(Context context) {
        mAppPrefs = AppPrefs.instance(context);
        restoreData();
    }

    public static SearchData instance(Context context) {
        if (sInstance == null) {
            sInstance = new SearchData(context.getApplicationContext());
        }

        return sInstance;
    }

    public void enableInstantVoiceSearch(boolean enabled) {
        mIsInstantVoiceSearchEnabled = enabled;
        persistData();
    }

    public boolean isInstantVoiceSearchEnabled() {
        return mIsInstantVoiceSearchEnabled;
    }

    public void enableFocusOnResults(boolean enabled) {
        mIsFocusOnResultsEnabled = enabled;
        persistData();
    }

    public boolean isFocusOnResultsEnabled() {
        return mIsFocusOnResultsEnabled;
    }

    public void setSearchOptions(int searchOptions) {
        mSearchOptions = searchOptions;
        persistData();
    }

    public int getSearchOptions() {
        return mSearchOptions;
    }

    public void enableKeyboardAutoShow(boolean enabled) {
        mIsKeyboardAutoShowEnabled = enabled;
        persistData();
    }

    public boolean isKeyboardAutoShowEnabled() {
        return mIsKeyboardAutoShowEnabled;
    }

    public void enableBackgroundPlayback(boolean enabled) {
        mIsBackgroundPlaybackEnabled = enabled;
        persistData();
    }

    public boolean isBackgroundPlaybackEnabled() {
        return mIsBackgroundPlaybackEnabled;
    }

    public void setSpeechRecognizerType(int type) {
        mSpeechRecognizerType = type;
        persistData();
    }

    public int getSpeechRecognizerType() {
        return mSpeechRecognizerType;
    }

    private void restoreData() {
        String data = mAppPrefs.getData(SEARCH_DATA);

        String[] split = Helpers.splitObjectLegacy(data);

        // WARN: Don't enable Instant Voice Search
        // Serious bug on Nvidia Shield. Can't type anything with soft keyboard.
        // Other devices probably affected too.
        mIsInstantVoiceSearchEnabled = Helpers.parseBoolean(split, 0, false);
        mSearchOptions = Helpers.parseInt(split, 1, 0);
        mIsFocusOnResultsEnabled = Helpers.parseBoolean(split, 2, true);
        mIsKeyboardAutoShowEnabled = Helpers.parseBoolean(split, 3, false);
        mIsBackgroundPlaybackEnabled = Helpers.parseBoolean(split, 4, false);
        //mIsAltSpeechRecognizerEnabled
        mSpeechRecognizerType = Helpers.parseInt(split, 6, SPEECH_RECOGNIZER_SYSTEM);
    }

    private void persistData() {
        mAppPrefs.setData(SEARCH_DATA,
                Helpers.mergeObject(mIsInstantVoiceSearchEnabled, mSearchOptions, mIsFocusOnResultsEnabled,
                        mIsKeyboardAutoShowEnabled, mIsBackgroundPlaybackEnabled, null, mSpeechRecognizerType));
    }
}
