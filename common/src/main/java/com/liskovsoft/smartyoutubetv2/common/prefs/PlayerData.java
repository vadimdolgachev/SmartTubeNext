package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controller.PlaybackEngineController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.AutoFrameRateManager.AfrData;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem;

public class PlayerData {
    public static final int ONLY_UI = 0;
    public static final int UI_AND_PAUSE = 1;
    public static final int ONLY_PAUSE = 2;
    public static final int AUTO_HIDE_NEVER = 0;
    public static final int BACKGROUND_PLAYBACK_NONE = 0;
    public static final int BACKGROUND_PLAYBACK_AUDIO = 1;
    public static final int BACKGROUND_PLAYBACK_PIP = 2;
    public static final int BACKGROUND_PLAYBACK_BEHIND = 3;
    @SuppressLint("StaticFieldLeak")
    private static PlayerData sInstance;
    private final AppPrefs mPrefs;
    private int mOKButtonBehavior;
    private int mUIHideTimeoutSec;
    private boolean mIsShowFullDateEnabled;
    private boolean mIsSeekPreviewEnabled;
    private boolean mIsPauseOnSeekEnabled;
    private boolean mIsClockEnabled;
    private boolean mIsRemainingTimeEnabled;
    private int mBackgroundPlaybackType;
    private AfrData mAfrData;
    private FormatItem mVideoFormat;
    private FormatItem mAudioFormat;
    private FormatItem mSubtitleFormat;
    private int mVideoBufferType;

    public PlayerData(Context context) {
        mPrefs = AppPrefs.instance(context);
        restoreData();
    }

    public static PlayerData instance(Context context) {
        if (sInstance == null) {
            sInstance = new PlayerData(context.getApplicationContext());
        }

        return sInstance;
    }

    public void setOKButtonBehavior(int option) {
        mOKButtonBehavior = option;
        persistData();
    }

    public int getOKButtonBehavior() {
        return mOKButtonBehavior;
    }

    public void setUIHideTimoutSec(int timoutSec) {
        mUIHideTimeoutSec = timoutSec;
        persistData();
    }

    public int getUIHideTimoutSec() {
        return mUIHideTimeoutSec;
    }

    public void showFullDate(boolean show) {
        mIsShowFullDateEnabled = show;
        persistData();
    }

    public boolean isShowFullDateEnabled() {
        return mIsShowFullDateEnabled;
    }

    public void enableSeekPreview(boolean show) {
        mIsSeekPreviewEnabled = show;
        persistData();
    }

    public boolean isSeekPreviewEnabled() {
        return mIsSeekPreviewEnabled;
    }

    public void enablePauseOnSeek(boolean enable) {
        mIsPauseOnSeekEnabled = enable;
        persistData();
    }

    public boolean isClockEnabled() {
        return mIsClockEnabled;
    }

    public void enableClock(boolean enable) {
        mIsClockEnabled = enable;
        persistData();
    }

    public boolean isRemainingTimeEnabled() {
        return mIsRemainingTimeEnabled;
    }

    public void enableRemainingTime(boolean enable) {
        mIsRemainingTimeEnabled = enable;
        persistData();
    }

    public boolean isPauseOnSeekEnabled() {
        return mIsPauseOnSeekEnabled;
    }

    public void setBackgroundPlaybackType(int type) {
        mBackgroundPlaybackType = type;
        persistData();
    }

    public int getBackgroundPlaybackType() {
        return mBackgroundPlaybackType;
    }

    public AfrData getAfrData() {
        return mAfrData;
    }

    public void setAfrData(AfrData afrData) {
        mAfrData = afrData;
        persistData();
    }

    public FormatItem getVideoFormat() {
        return mVideoFormat;
    }

    public void setVideoFormat(FormatItem format) {
        mVideoFormat = format;
        persistData();
    }

    public FormatItem getAudioFormat() {
        return mAudioFormat;
    }

    public void setAudioFormat(FormatItem format) {
        mAudioFormat = format;
        persistData();
    }

    public FormatItem getSubtitleFormat() {
        return mSubtitleFormat;
    }

    public void setSubtitleFormat(FormatItem format) {
        mSubtitleFormat = format;
        persistData();
    }

    public void setVideoBufferType(int type) {
        mVideoBufferType = type;
        persistData();
    }

    public int getVideoBufferType() {
        return mVideoBufferType;
    }

    private void restoreData() {
        String data = mPrefs.getPlayerData();

        String[] split = Helpers.splitObject(data);

        mOKButtonBehavior = Helpers.parseInt(split, 0, ONLY_UI);
        mUIHideTimeoutSec = Helpers.parseInt(split, 1, 3);
        mIsShowFullDateEnabled = Helpers.parseBoolean(split, 2, false);
        mIsSeekPreviewEnabled = Helpers.parseBoolean(split, 3, true);
        mIsPauseOnSeekEnabled = Helpers.parseBoolean(split, 4, false);
        mIsClockEnabled = Helpers.parseBoolean(split, 5, true);
        mIsRemainingTimeEnabled = Helpers.parseBoolean(split, 6, true);
        mBackgroundPlaybackType = Helpers.parseInt(split, 7, BACKGROUND_PLAYBACK_NONE);
        mAfrData = AfrData.from(Helpers.parseStr(split, 8));
        mVideoFormat = ExoFormatItem.from(Helpers.parseStr(split, 9));
        mAudioFormat = ExoFormatItem.from(Helpers.parseStr(split, 10));
        mSubtitleFormat = ExoFormatItem.from(Helpers.parseStr(split, 11));
        mVideoBufferType = Helpers.parseInt(split, 12, PlaybackEngineController.BUFFER_LOW);
    }

    private void persistData() {
        mPrefs.setPlayerData(Helpers.mergeObject(mOKButtonBehavior, mUIHideTimeoutSec,
                mIsShowFullDateEnabled, mIsSeekPreviewEnabled, mIsPauseOnSeekEnabled,
                mIsClockEnabled, mIsRemainingTimeEnabled, mBackgroundPlaybackType, Helpers.toString(mAfrData),
                Helpers.toString(mVideoFormat), Helpers.toString(mAudioFormat), Helpers.toString(mVideoFormat),
                mVideoBufferType));
    }
}
