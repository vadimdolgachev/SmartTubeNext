package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import android.annotation.SuppressLint;
import android.util.Pair;

import com.liskovsoft.mediaserviceinterfaces.yt.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.yt.ServiceManager;
import com.liskovsoft.mediaserviceinterfaces.yt.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.Analytics;
import com.liskovsoft.mediaserviceinterfaces.yt.data.MediaItemMetadata;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SampleMediaItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.PlayerEventListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerEngineConstants;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.VideoActionPresenter;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.common.DataChangeBase.OnDataChange;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;
import com.liskovsoft.smartyoutubetv2.common.utils.UniqueRandom;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.disposables.Disposable;

import java.util.Collections;
import java.util.List;

public class VideoLoaderController extends BasePlayerController implements OnDataChange {
    private static final String TAG = VideoLoaderController.class.getSimpleName();
    private static final long STREAM_END_THRESHOLD_MS = 180_000;
    private static final long LONG_BUFFERING_THRESHOLD_MS = 20_000;
    private final Playlist mPlaylist;
    private final UniqueRandom mRandom;
    private Video mLastVideo;
    private int mLastErrorType = -1;
    private SuggestionsController mSuggestionsController;
    private PlayerData mPlayerData;
    private PlayerTweaksData mPlayerTweaksData;
    private long mSleepTimerStartMs;
    private Disposable mFormatInfoAction;
    private Disposable mMpdStreamAction;
    private final Runnable mReloadVideo = () -> loadVideo(mLastVideo);
    private final Runnable mLoadNext = this::loadNext;
    private final Runnable mMetadataSync = () -> {
        if (getPlayer() != null) {
            waitMetadataSync(getPlayer().getVideo(), false);
        }
    };
    private final Runnable mRestartEngine = () -> {
        if (getPlayer() != null) {
            getPlayer().restartEngine(); // properly save position of the current track
        }
    };
    private final Runnable mLoadRandomNext = this::loadRandomNext;
    private final Runnable mOnLongBuffering = this::onLongBuffering;
    private Pair<Integer, Long> mBufferingCount;
    private boolean mIsWasVideoStartError;
    private boolean mIsWasStarted;

    public VideoLoaderController() {
        mPlaylist = Playlist.instance();
        mRandom = new UniqueRandom();
    }

    @Override
    public void onInit() {
        mSuggestionsController = getController(SuggestionsController.class);
        mPlayerData = PlayerData.instance(getContext());
        mPlayerData.setOnChange(this);
        mPlayerTweaksData = PlayerTweaksData.instance(getContext());
        mSleepTimerStartMs = System.currentTimeMillis();
    }

    @Override
    public void openVideo(Video item) {
        if (item == null) {
            return;
        }

        boolean isVideoChanged = !item.equals(mLastVideo);
        mLastVideo = item; // save for later

        if (!item.fromQueue) {
            mPlaylist.add(item);
        } else {
            item.fromQueue = false;
        }

        if (getPlayer() != null && getPlayer().isEngineInitialized()) { // player is initialized
            if (isVideoChanged) {
                loadVideo(item); // force play immediately
            } else {
                loadSuggestions(item); // update suggestions only
            }
        }
        mIsWasVideoStartError = false;
        mIsWasStarted = false;
        Analytics.sendVideoStarting(item.videoId, item.title);
    }

    @Override
    public void onBuffering() {
        Utils.postDelayed(mOnLongBuffering, LONG_BUFFERING_THRESHOLD_MS);
    }

    private void onLongBuffering() {
        //updateBufferingCount();

        if (mLastVideo == null) {
            return;
        }

        // Stream end check (hangs on buffering)
        if ((!mLastVideo.isLive || mLastVideo.isLiveEnd) &&
                getPlayer().getDurationMs() - getPlayer().getPositionMs() < STREAM_END_THRESHOLD_MS) {
            getMainController().onPlayEnd();
        }

        // NOTE: useless fixes. Won't fix the buffering actually.
        //else if (isBufferingRecurrent()) {
        //    MessageHelpers.showLongMessage(getContext(), R.string.applying_fix);
        //
        //    // Switch between network engines in hope that one of them fixes the error
        //    // Cronet engine do less buffering
        //    //mPlayerTweaksData.setPlayerDataSource(PlayerTweaksData.PLAYER_DATA_SOURCE_CRONET);
        //    mPlayerTweaksData.setPlayerDataSource(getNextEngine());
        //
        //    //YouTubeServiceManager.instance().applyNoPlaybackFix();
        //
        //    restartEngine();
        //}
    }

    @Override
    public void onEngineInitialized() {
        loadVideo(mLastVideo);
        getPlayer().setButtonState(R.id.action_repeat, mPlayerData.getRepeatMode());
        mSleepTimerStartMs = System.currentTimeMillis();
    }

    @Override
    public void onEngineReleased() {
        disposeActions();
    }

    @Override
    public void onEngineError(int type, int rendererIndex, Throwable error) {
        Log.e(TAG, "Player error occurred: %s. Trying to fix…", type);

        mLastErrorType = type;
        runErrorAction(type, rendererIndex, error);
        if (!mIsWasVideoStartError && mLastVideo != null) {
            Analytics.sendVideoStartError(mLastVideo.videoId,
                    mLastVideo.title,
                    error.getMessage());
            mIsWasVideoStartError = true;
        }
    }

    @Override
    public void onVideoLoaded(Video video) {
        mLastErrorType = -1;
        Utils.removeCallbacks(mOnLongBuffering);
        getPlayer().setButtonState(R.id.action_repeat, video.finishOnEnded ? PlayerEngineConstants.REPEAT_MODE_CLOSE : mPlayerData.getRepeatMode());
    }

    @Override
    public boolean onPreviousClicked() {
        loadPrevious();

        return true;
    }

    @Override
    public boolean onNextClicked() {
        if (GeneralData.instance(getContext()).isChildModeEnabled()) {
            onPlayEnd();
        } else {
            loadNext();
        }

        return true;
    }

    @Override
    public void onFinish() {
        mPlaylist.clearPosition();
    }

    public void loadPrevious() {
        openVideoInt(mSuggestionsController.getPrevious());
    }

    public void loadNext() {
        Video next = mSuggestionsController.getNext();
        mLastVideo = null; // in case next video is the same as previous

        if (next != null) {
            openVideoInt(next);
        } else {
            waitMetadataSync(getPlayer().getVideo(), true);
        }

        if (mPlayerTweaksData.isPlayerUiOnNextEnabled()) {
            getPlayer().showOverlay(true);
        }
    }

    @Override
    public void onPlayEnd() {
        int repeatMode = mPlayerData.getRepeatMode();

        Video video = getPlayer().getVideo();
        if (video != null && video.finishOnEnded) {
            repeatMode = PlayerEngineConstants.REPEAT_MODE_CLOSE;
        } else if (video != null && video.isShorts && mPlayerTweaksData.isLoopShortsEnabled()) {
            repeatMode = PlayerEngineConstants.REPEAT_MODE_ONE;
        }

        applyRepeatMode(repeatMode);
    }

    @Override
    public void onSuggestionItemClicked(Video item) {
        openVideoInt(item);

        getPlayer().showControls(false);
    }

    @Override
    public void onPlaybackQueueClicked() {
        AppDialogUtil.showPlaybackQueueDialog(getContext(), this::openVideoInt);
    }

    @Override
    public boolean onKeyDown(int keyCode) {
        mSleepTimerStartMs = System.currentTimeMillis();

        // Remove error msg if needed
        if (mPlayerData.isSonyTimerFixEnabled()) {
            getPlayer().setVideo(mLastVideo);
        }

        Utils.removeCallbacks(mRestartEngine);
        Utils.removeCallbacks(mOnLongBuffering);

        return false;
    }

    @Override
    public void onTickle() {
        checkSleepTimer();
    }

    private void checkSleepTimer() {
        if (mPlayerData.isSonyTimerFixEnabled() && System.currentTimeMillis() - mSleepTimerStartMs > 60 * 60 * 1_000) {
            getPlayer().setPlayWhenReady(false);
            getPlayer().setTitle(getContext().getString(R.string.sleep_timer));
            getPlayer().showOverlay(true);
        }
    }

    /**
     * Force load and play!
     */
    private void loadVideo(Video item) {
        if (item != null) {
            mPlaylist.setCurrent(item);
            getPlayer().setVideo(item);
            getPlayer().resetPlayerState();
            loadFormatInfo(item);
        }
    }

    /**
     * Force load suggestions.
     */
    private void loadSuggestions(Video item) {
        if (item != null) {
            mPlaylist.setCurrent(item);
            getPlayer().setVideo(item);
            mSuggestionsController.loadSuggestions(item);
        }
    }

    private void waitMetadataSync(Video current, boolean showLoadingMsg) {
        if (current == null) {
            return;
        }

        if (current.nextMediaItem != null) {
            openVideoInt(Video.from(current.nextMediaItem));
        } else if (!current.isSynced) { // Maybe there's nothing left. E.g. when casting from phone
            // Wait in a loop while suggestions have been loaded...
            if (showLoadingMsg) {
                MessageHelpers.showMessageThrottled(getContext(), R.string.wait_data_loading);
            }
            // Short videos next fix (suggestions aren't loaded yet)
            boolean isEnded = getPlayer() != null && Math.abs(getPlayer().getDurationMs() - getPlayer().getPositionMs()) < 100;
            if (isEnded) {
                Utils.postDelayed(mMetadataSync, 1_000);
            }
        }
    }

    private void loadFormatInfo(Video video) {
        getPlayer().showProgressBar(true);
        disposeActions();

        ServiceManager service = YouTubeServiceManager.instance();
        MediaItemService mediaItemManager = service.getMediaItemService();
        mFormatInfoAction = mediaItemManager.getFormatInfoObserve(video.videoId)
                .subscribe(this::processFormatInfo,
                           error -> {
                               getPlayer().showProgressBar(false);
                               String message = error.getMessage();
                               Log.e(TAG, "loadFormatInfo error: %s", message);
                               if (!Helpers.containsAny(message, "fromNullable result is null")) {
                                   MessageHelpers.showLongMessage(getContext(), message);
                               }

                               if (Helpers.containsAny(message, "Unexpected token", "Syntax error", "invalid argument")) { // temporal fix
                                   YouTubeServiceManager.instance().applyNoPlaybackFix();
                                   restartEngine();
                               } else {
                                   Log.e(TAG, "Probably no internet connection");
                                   scheduleReloadVideoTimer(1_000);
                               }
                               if (!mIsWasVideoStartError) {
                                   Analytics.sendVideoStartError(video.videoId,
                                           video.title,
                                           error.getMessage());
                                   mIsWasVideoStartError = true;
                               }
                           });
    }

    private void processFormatInfo(MediaItemFormatInfo formatInfo) {
        String bgImageUrl = null;

        mLastVideo.sync(formatInfo);

        if (formatInfo.isUnplayable() || formatInfo.isAgeRestricted()) {
            getPlayer().setTitle(formatInfo.getPlayabilityStatus());
            getPlayer().showProgressBar(false);
            mSuggestionsController.loadSuggestions(mLastVideo);
            bgImageUrl = mLastVideo.getBackgroundUrl();
            scheduleNextVideoTimer(5_000);
            if (!mIsWasVideoStartError) {
                Analytics.sendVideoStartError(mLastVideo.videoId,
                        mLastVideo.title,
                        formatInfo.getPlayabilityStatus());
                mIsWasVideoStartError = true;
            }
            if (formatInfo.isAgeRestricted()) {
                SignInPresenter.instance(getActivity()).start();
                getActivity().finish();
            }
        } else if (formatInfo.containsDashVideoInfo() && acceptDashVideoInfo(formatInfo)) {
            Log.d(TAG, "Found regular video in dash format. Loading...");

            mMpdStreamAction = formatInfo.createMpdStreamObservable()
                    .subscribe(
                            dashManifest -> {
                                if (mPlayerTweaksData.isHighBitrateFormatsEnabled() && formatInfo.hasExtendedHlsFormats()) {
                                    getPlayer().openMerged(dashManifest, formatInfo.getHlsManifestUrl());
                                } else {
                                    getPlayer().openDash(dashManifest);
                                }
                            },
                            error -> Log.e(TAG, "createMpdStream error: %s", error.getMessage())
                    );
        } else if (formatInfo.isLive() && formatInfo.containsDashUrl() && acceptDashUrl(formatInfo)) {
            Log.d(TAG, "Found live video (current or past live stream) in dash format. Loading...");
            getPlayer().openDashUrl(formatInfo.getDashManifestUrl());
        } else if (formatInfo.isLive() && formatInfo.containsHlsUrl()) {
            Log.d(TAG, "Found live video (current or past live stream) in hls format. Loading...");
            getPlayer().openHlsUrl(formatInfo.getHlsManifestUrl());
        } else if (formatInfo.containsUrlListInfo()) {
            Log.d(TAG, "Found url list video. This is always LQ. Loading...");
            getPlayer().openUrlList(applyFix(formatInfo.createUrlList()));
        } else {
            Log.d(TAG, "Empty format info received. Seems future live translation. No video data to pass to the player.");
            getPlayer().setTitle(formatInfo.getPlayabilityStatus());
            getPlayer().showProgressBar(false);
            mSuggestionsController.loadSuggestions(mLastVideo);
            bgImageUrl = mLastVideo.getBackgroundUrl();
            scheduleReloadVideoTimer(30 * 1_000);
            if (!mIsWasVideoStartError) {
                Analytics.sendVideoStartError(mLastVideo.videoId,
                        mLastVideo.title,
                        formatInfo.getPlayabilityStatus());
                mIsWasVideoStartError = true;
            }
        }

        getPlayer().showBackground(bgImageUrl); // remove bg (if video playing) or set another bg

        //if (bgImageUrl != null && getPlayer().containsMedia()) {
        //    // Make background visible
        //    getPlayer().restartEngine();
        //}
    }

    private void scheduleReloadVideoTimer(int delayMs) {
        if (getPlayer().isEngineInitialized()) {
            Log.d(TAG, "Starting check for the future stream...");
            getPlayer().showOverlay(true);
            Utils.postDelayed(mReloadVideo, delayMs);
        }
    }

    private void scheduleNextVideoTimer(int delayMs) {
        if (getPlayer().isEngineInitialized()) {
            Log.d(TAG, "Starting next video after delay...");
            getPlayer().showOverlay(true);
            Utils.postDelayed(mLoadNext, delayMs);
        }
    }

    private void openVideoInt(Video item) {
        if (item == null) {
            return;
        }

        disposeActions();

        if (item.hasVideo()) {
            // NOTE: Next clicked: instant playback even a mix
            // NOTE: Bypass PIP fullscreen on next caused by startView
            getMainController().openVideo(item);
            //getPlayer().showOverlay(true);
        } else {
            VideoActionPresenter.instance(getContext()).apply(item);
        }
    }

    private boolean isActionsRunning() {
        return RxHelper.isAnyActionRunning(mFormatInfoAction, mMpdStreamAction);
    }

    private void disposeActions() {
        mBufferingCount = null;
        MediaServiceManager.instance().disposeActions();
        RxHelper.disposeActions(mFormatInfoAction, mMpdStreamAction);
        Utils.removeCallbacks(mReloadVideo, mLoadNext, mRestartEngine, mMetadataSync);
        Utils.removeCallbacks(mOnLongBuffering);
    }

    @SuppressLint("StringFormatMatches")
    private void runErrorAction(int type, int rendererIndex, Throwable error) {
        applyGenericErrorAction(type, rendererIndex, error);

        restartEngine();
    }

    private void applyGenericErrorAction(int type, int rendererIndex, Throwable error) {
        String message = error != null ? error.getMessage() : null;
        String errorTitle = getErrorTitle(type, rendererIndex);

        if (Helpers.startsWithAny(message, "Unable to connect to")) {
            // No internet connection
            MessageHelpers.showLongMessage(getContext(), errorTitle + "\n" + message);
            return;
        }

        if (error instanceof OutOfMemoryError) {
            if (mPlayerData.getVideoBufferType() == PlayerData.BUFFER_NONE) {
                mPlayerTweaksData.enableSectionPlaylist(false);
            } else if (mPlayerData.getVideoBufferType() == PlayerData.BUFFER_LOW) {
                mPlayerData.setVideoBufferType(PlayerData.BUFFER_NONE);
            } else {
                mPlayerData.setVideoBufferType(PlayerData.BUFFER_LOW);
            }
        } else if (Helpers.containsAny(message, "Exception in CronetUrlRequest")) {
            mPlayerTweaksData.setPlayerDataSource(PlayerTweaksData.PLAYER_DATA_SOURCE_DEFAULT);
        } else if (Helpers.startsWithAny(message, "Response code: 403", "Response code: 404")) {
            // "Response code: 403" (url deciphered incorrectly)
            // "Response code: 404" (not sure whether below helps)
            YouTubeServiceManager.instance().applyNoPlaybackFix();
        } else if (type == PlayerEventListener.ERROR_TYPE_SOURCE && rendererIndex == PlayerEventListener.RENDERER_INDEX_UNKNOWN) {
            // NOTE: Fixing too many requests or network issues
            // NOTE: All these errors have unknown renderer (-1)
            // "Unable to connect to", "Invalid NAL length", "Response code: 421",
            // "Response code: 404", "Response code: 429", "Invalid integer size",
            // "Unexpected ArrayIndexOutOfBoundsException", "Unexpected IndexOutOfBoundsException"
            // "Response code: 403" (url deciphered incorrectly)
            mPlayerTweaksData.setPlayerDataSource(getNextEngine());
        } else if (type == PlayerEventListener.ERROR_TYPE_RENDERER && rendererIndex == PlayerEventListener.RENDERER_INDEX_SUBTITLE) {
            // "Response code: 500"
            if (mLastVideo != null) {
                mPlayerData.disableSubtitlesPerChannel(mLastVideo.channelId);
                mPlayerData.setFormat(mPlayerData.getDefaultSubtitleFormat());
            }
        } else if (type == PlayerEventListener.ERROR_TYPE_RENDERER && rendererIndex == PlayerEventListener.RENDERER_INDEX_VIDEO) {
            FormatItem videoFormat = mPlayerData.getFormat(FormatItem.TYPE_VIDEO);
            if (!videoFormat.isPreset()) {
                mPlayerData.setFormat(mPlayerData.getDefaultVideoFormat());
            }
            mPlayerTweaksData.forceSWDecoder(false);
        } else if (type == PlayerEventListener.ERROR_TYPE_RENDERER && rendererIndex == PlayerEventListener.RENDERER_INDEX_AUDIO) {
            mPlayerData.setFormat(mPlayerData.getDefaultAudioFormat());
        }

        // Hide unknown errors on all devices
        if (type != PlayerEventListener.ERROR_TYPE_UNEXPECTED) {
            MessageHelpers.showLongMessage(getContext(), errorTitle + "\n" + message + "\n" + getContext().getString(R.string.applying_fix));
        }
    }

    @SuppressLint("StringFormatMatches")
    private String getErrorTitle(int type, int rendererIndex) {
        String errorTitle;
        int msgResId;

        switch (type) {
            // Some ciphered data could be outdated.
            // Might happen when the app wasn't used quite a long time.
            case PlayerEventListener.ERROR_TYPE_SOURCE:
                switch (rendererIndex) {
                    case PlayerEventListener.RENDERER_INDEX_VIDEO:
                        msgResId = R.string.msg_player_error_video_source;
                        break;
                    case PlayerEventListener.RENDERER_INDEX_AUDIO:
                        msgResId = R.string.msg_player_error_audio_source;
                        break;
                    case PlayerEventListener.RENDERER_INDEX_SUBTITLE:
                        msgResId = R.string.msg_player_error_subtitle_source;
                        break;
                    default:
                        msgResId = R.string.unknown_source_error;
                }
                errorTitle = getContext().getString(msgResId);
                break;
            case PlayerEventListener.ERROR_TYPE_RENDERER:
                switch (rendererIndex) {
                    case PlayerEventListener.RENDERER_INDEX_VIDEO:
                        msgResId = R.string.msg_player_error_video_renderer;
                        break;
                    case PlayerEventListener.RENDERER_INDEX_AUDIO:
                        msgResId = R.string.msg_player_error_audio_renderer;
                        break;
                    case PlayerEventListener.RENDERER_INDEX_SUBTITLE:
                        msgResId = R.string.msg_player_error_subtitle_renderer;
                        break;
                    default:
                        msgResId = R.string.unknown_renderer_error;
                }
                errorTitle = getContext().getString(msgResId);
                break;
            case PlayerEventListener.ERROR_TYPE_UNEXPECTED:
                errorTitle = getContext().getString(R.string.msg_player_error_unexpected);
                break;
            default:
                errorTitle = getContext().getString(R.string.msg_player_error, type);
                break;
        }

        return errorTitle;
    }

    private void restartEngine() {
        // Give a time to user to do something
        Utils.postDelayed(mRestartEngine, 1_000);
    }

    private List<String> applyFix(List<String> urlList) {
        // Sometimes top url cannot be played
        if (mLastErrorType == PlayerEventListener.ERROR_TYPE_SOURCE) {
            Collections.reverse(urlList);
        }

        return urlList;
    }

    private void applyRepeatMode(int repeatMode) {
        Video video = getPlayer().getVideo();
        // Fix simultaneous videos loading (e.g. when playback ends and user opens new video)
        if (isActionsRunning() || video == null) {
            return;
        }

        switch (repeatMode) {
            case PlayerEngineConstants.REPEAT_MODE_REVERSE_LIST:
                if (video.hasPlaylist() || video.belongsToChannelUploads() || video.belongsToChannel()) {
                    VideoGroup group = video.getGroup();
                    if (group != null && group.indexOf(video) != 0) { // stop after first
                        onPreviousClicked();
                    }
                    break;
                }
            case PlayerEngineConstants.REPEAT_MODE_ALL:
            case PlayerEngineConstants.REPEAT_MODE_SHUFFLE:
                loadNext();
                break;
            case PlayerEngineConstants.REPEAT_MODE_ONE:
                getPlayer().setPositionMs(100); // fix frozen image on Android 4?
                break;
            case PlayerEngineConstants.REPEAT_MODE_CLOSE:
                // Close player if suggestions not shown
                // Except when playing from queue
                if (mPlaylist.getNext() != null) {
                    loadNext();
                } else {
                    AppDialogPresenter dialog = AppDialogPresenter.instance(getContext());
                    if (!getPlayer().isSuggestionsShown() && (!dialog.isDialogShown() || dialog.isTransparent())) {
                        dialog.closeDialog();
                        getPlayer().finishReally();
                    }
                }
                break;
            case PlayerEngineConstants.REPEAT_MODE_PAUSE:
                // Stop player after each video.
                // Except when playing from queue
                if (mPlaylist.getNext() != null) {
                    loadNext();
                } else {
                    getPlayer().setPositionMs(getPlayer().getDurationMs());
                    getPlayer().setPlayWhenReady(false);
                    getPlayer().showSuggestions(true);
                }
                break;
            case PlayerEngineConstants.REPEAT_MODE_LIST:
                // stop player (if not playing playlist)
                if (video.hasNextPlaylist() || mPlaylist.getNext() != null) {
                    loadNext();
                } else {
                    getPlayer().setPositionMs(getPlayer().getDurationMs());
                    getPlayer().setPlayWhenReady(false);
                    getPlayer().showSuggestions(true);
                }
                break;
            default:
                Log.e(TAG, "Undetected repeat mode " + repeatMode);
                break;
        }
    }

    private boolean acceptDashVideoInfo(MediaItemFormatInfo formatInfo) {
        // Not enough info for full length live streams
        if (formatInfo.isLive() && formatInfo.getStartTimeMs() == 0) {
            return false;
        }

        // Live dash url doesn't work with None buffer
        //if (formatInfo.isLive() && (mPlayerTweaksData.isDashUrlStreamsForced() || mPlayerData.getVideoBufferType() == PlayerData.BUFFER_NONE)) {
        if (formatInfo.isLive() && mPlayerTweaksData.isDashUrlStreamsForced()) {
            return false;
        }

        if (formatInfo.isLive() && mPlayerTweaksData.isHlsStreamsForced()) {
            return false;
        }

        //if (mPlayerData.isLegacyCodecsForced()) {
        //    return false;
        //}

        return true;
    }

    private boolean acceptDashUrl(MediaItemFormatInfo formatInfo) {
        if (formatInfo.isLive() && mPlayerTweaksData.isHlsStreamsForced() && formatInfo.containsHlsUrl()) {
            return false;
        }

        //if (mPlayerData.isLegacyCodecsForced()) {
        //    return false;
        //}

        return true;
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        loadRandomNext();
    }

    @Override
    public void onPlay() {
        Utils.removeCallbacks(mOnLongBuffering);
        if (!mIsWasStarted && mLastVideo != null) {
            Analytics.sendVideoStarted(mLastVideo.videoId, mLastVideo.title);
            mIsWasStarted = true;
        }
    }

    @Override
    public void onDataChange() {
        Utils.postDelayed(mLoadRandomNext, 3_000);
    }

    private void loadRandomNext() {
        MediaServiceManager.instance().disposeActions();

        if (getPlayer() == null || mPlayerData == null || mLastVideo == null || mLastVideo.playlistInfo == null) {
            return;
        }

        if (mPlayerData.getRepeatMode() == PlayerEngineConstants.REPEAT_MODE_SHUFFLE) {
            Video video = new Video();
            video.playlistId = mLastVideo.playlistId;
            VideoGroup topRow = getPlayer().getSuggestionsByIndex(0);
            video.playlistIndex = mRandom.getPlaylistIndex(mLastVideo.getPlaylistId(),
                    mLastVideo.playlistInfo.getSize() != -1 ? mLastVideo.playlistInfo.getSize() : topRow != null ? topRow.getVideos().size() : -1);

            MediaServiceManager.instance().loadMetadata(video, randomMetadata -> {
                if (randomMetadata.getNextVideo() == null) {
                    return;
                }

                if (mLastVideo.nextMediaItemBackup == null) {
                    mLastVideo.nextMediaItemBackup = mLastVideo.nextMediaItem;
                }

                mLastVideo.nextMediaItem = SampleMediaItem.from(randomMetadata);
                getPlayer().setNextTitle(Video.from(mLastVideo.nextMediaItem));
            });
        } else if (mLastVideo.nextMediaItemBackup != null) {
            mLastVideo.nextMediaItem = mLastVideo.nextMediaItemBackup;
            getPlayer().setNextTitle(Video.from(mLastVideo.nextMediaItem));
        }
    }

    private int getNextEngine() {
        int currentEngine = mPlayerTweaksData.getPlayerDataSource();
        int[] engineList = Utils.skipCronet() ?
                new int[] { PlayerTweaksData.PLAYER_DATA_SOURCE_DEFAULT, PlayerTweaksData.PLAYER_DATA_SOURCE_OKHTTP } :
                new int[] { PlayerTweaksData.PLAYER_DATA_SOURCE_CRONET, PlayerTweaksData.PLAYER_DATA_SOURCE_DEFAULT, PlayerTweaksData.PLAYER_DATA_SOURCE_OKHTTP };
        return Helpers.getNextValue(currentEngine, engineList);
    }

    private void updateBufferingCount() {
        long currentTimeMs = System.currentTimeMillis();
        int bufferingCount = 0;
        long previousTimeMs = 0;

        if (mBufferingCount != null) {
            bufferingCount = mBufferingCount.first;
            previousTimeMs = mBufferingCount.second;
        }

        if (currentTimeMs - previousTimeMs < 30_000) {
            bufferingCount++;
        } else {
            bufferingCount = 1;
        }

        mBufferingCount = new Pair<>(bufferingCount, currentTimeMs);
    }

    private boolean isBufferingRecurrent() {
        return mBufferingCount != null && mBufferingCount.first > 2;
    }
}
