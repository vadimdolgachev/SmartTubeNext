package com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers;

import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxUtils;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.PlayerEventListenerHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.SuggestionsLoaderManager.MetadataListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.misc.ScreensaverManager;
import com.liskovsoft.smartyoutubetv2.common.misc.TickleManager;
import com.liskovsoft.smartyoutubetv2.common.misc.TickleManager.TickleListener;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class VideoStateManager extends PlayerEventListenerHelper implements TickleListener, MetadataListener {
    private static final long MUSIC_VIDEO_MAX_DURATION_MS = 6 * 60 * 1000;
    private static final long LIVE_THRESHOLD_MS = 60_000;
    private static final String TAG = VideoStateManager.class.getSimpleName();
    private static final float RESTORE_POSITION_PERCENTS = 10; // min value for immediately closed videos
    private boolean mIsPlayEnabled;
    private Video mVideo = new Video();
    private FormatItem mTempVideoFormat;
    private Disposable mHistoryAction;
    private PlayerData mPlayerData;
    private PlayerTweaksData mPlayerTweaksData;
    private VideoStateService mStateService;
    private boolean mIsPlayBlocked;
    private int mTickleLeft;

    @Override
    public void onInitDone() { // called each time a video opened from the browser
        mPlayerData = PlayerData.instance(getActivity());
        mPlayerTweaksData = PlayerTweaksData.instance(getActivity());
        mStateService = VideoStateService.instance(getActivity());

        // onInitDone usually called after openVideo (if PlaybackView is destroyed)
        // So, we need to repeat some things again.
        resetPositionIfNeeded(getVideo());
    }

    /**
     * Fired after user clicked on video in browse activity<br/>
     * or video is opened from the intent
     */
    @Override
    public void openVideo(Video item) {
        // Ensure that we aren't running on presenter init stage
        if (getController() != null) {
            if (!item.equals(getVideo())) { // video might be opened twice (when remote connection enabled). Fix for that.
                // Save state of the previous video.
                // In case video opened from phone and other stuff.
                saveState();
            }
        }

        setPlayEnabled(true); // video just added

        mTempVideoFormat = null;

        // Don't do reset on videoLoaded state because this will influences minimized music videos.
        resetPositionIfNeeded(item);
        resetGlobalSpeedIfNeeded();
    }

    @Override
    public boolean onPreviousClicked() {
        boolean isFarFromStart = getController().getPositionMs() > 10_000;

        if (isFarFromStart) {
            saveState(); // in case the user wants to go to previous video
            getController().setPositionMs(0);
            return true;
        }

        return false;
    }

    @Override
    public boolean onNextClicked() {
        setPlayEnabled(true);

        saveState();

        clearStateOfNextVideo();

        return false;
    }

    @Override
    public void onSuggestionItemClicked(Video item) {
        setPlayEnabled(true); // autoplay video from suggestions

        saveState();
    }

    @Override
    public void onEngineInitialized() {
        TickleManager.instance().addListener(this);

        // Restore before video loaded.
        // This way we override auto track selection mechanism.
        //restoreFormats();

        // Show user info instead of black screen.
        if (!getPlayEnabled()) {
            getController().showOverlay(true);
        }
    }

    @Override
    public void onEngineReleased() {
        mTickleLeft = 0;
        TickleManager.instance().removeListener(this);

        // Save previous state
        if (getController().containsMedia()) {
            setPlayEnabled(getController().getPlayWhenReady());
            saveState();
        }
    }

    @Override
    public void onTickle() {
        // Sync history every five minutes
        if (++mTickleLeft > 5) {
            mTickleLeft = 0;
            updateHistory();
        }
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        updateHistory();
    }

    @Override
    public void onEngineError(int type) {
        // Oops. Error happens while playing (network lost etc).
        if (getController().getPositionMs() > 1_000) {
            saveState();
        }
    }

    @Override
    public void onVideoLoaded(Video item) {
        // Actual video that match currently loaded one.
        mVideo = item;

        // Restore formats again.
        // Maybe this could help with Shield format problem.
        // NOTE: produce multi thread exception:
        // Attempt to read from field 'java.util.TreeMap$TreeMapEntry java.util.TreeMap$TreeMapEntry.left' on a null object reference (TrackSelectorManager.java:181)
        //restoreFormats();

        // In this state video length is not undefined.
        restorePosition();
        restorePendingPosition();
        restoreSpeed();
        // Player thinks that subs not enabled if I enable it too early (e.g. on source change event).
        restoreSubtitleFormat();

        restoreVolume();
    }

    @Override
    public void onPlay() {
        setPlayEnabled(true);
        showHideScreensaver(false);
    }

    @Override
    public void onPause() {
        setPlayEnabled(false);
        //saveState();
        showHideScreensaver(true);
    }

    @Override
    public void onTrackSelected(FormatItem track) {
        if (!getController().isInPIPMode()) {
            if (track.getType() == FormatItem.TYPE_VIDEO) {
                if (mPlayerData.getFormat(FormatItem.TYPE_VIDEO).isPreset()) {
                    mTempVideoFormat = track;
                } else {
                    mTempVideoFormat = null;
                    mPlayerData.setFormat(track);
                }
            } else {
                mPlayerData.setFormat(track);
            }
        }
    }

    @Override
    public void onPlayEnd() {
        saveState();

        // Don't enable screensaver here or you'll broke 'screen off' logic.
        showHideScreensaver(true);
    }

    @Override
    public void onBuffering() {
        // Check LIVE threshold and set speed to normal
        restoreSpeed();
        // Live stream starts to buffer after the end
        showHideScreensaver(true);
    }

    @Override
    public void onSeekEnd() {
        // Scenario: user opens ui and does some seeking
        // NOTE: dangerous: there's possibility of simultaneous seeks (e.g. when sponsor block is enabled)
        //saveState();
    }

    @Override
    public void onControlsShown(boolean shown) {
        // NOTE: bug: current position saving to wrong video id. Explanation below.
        // Bug in casting: current video doesn't match currently loaded one into engine.
        //if (shown) {
        //    // Scenario: user clicked on channel button
        //    saveState();
        //}
    }

    @Override
    public void onSourceChanged(Video item) {
        restoreFormats();
    }

    @Override
    public void onViewPaused() {
        persistState();
    }

    private void clearStateOfNextVideo() {
        if (getVideo() != null && getVideo().nextMediaItem != null) {
            resetPosition(getVideo().nextMediaItem.getVideoId());
        }
    }

    /**
     * Reset position of currently opened music and live videos.
     */
    private void resetPositionIfNeeded(Video item) {
        if (mStateService == null || item == null) {
            return;
        }

        State state = mStateService.getByVideoId(item.videoId);

        // Reset position of music videos
        boolean isShort = state != null && (state.lengthMs < MUSIC_VIDEO_MAX_DURATION_MS && !mPlayerTweaksData.isRememberPositionOfShortVideosEnabled());

        if (isShort || item.isLive) {
            resetPosition(item);
        }
    }

    private void resetGlobalSpeedIfNeeded() {
        if (mPlayerData != null && !mPlayerData.isRememberSpeedEnabled()) {
            mPlayerData.setSpeed(1.0f);
        }
    }

    private void resetPosition(Video video) {
        video.percentWatched = 0;
        resetPosition(video.videoId);
    }

    private void resetPosition(String videoId) {
        State state = mStateService.getByVideoId(videoId);

        if (state != null) {
            if (mPlayerData.isRememberSpeedEachEnabled()) {
                mStateService.save(new State(videoId, 0, state.lengthMs, state.speed));
            } else {
                mStateService.removeByVideoId(videoId);
            }
        }
    }

    private void persistState() {
        if (AppDialogPresenter.instance(getActivity()).isDialogShown()) {
            return;
        }

        mStateService.persistState();
    }

    private void restoreVideoFormat() {
        if (mTempVideoFormat != null) {
            getController().setFormat(mTempVideoFormat);
        } else {
            getController().setFormat(mPlayerData.getFormat(FormatItem.TYPE_VIDEO));
        }
    }

    private void restoreAudioFormat() {
        getController().setFormat(mPlayerData.getFormat(FormatItem.TYPE_AUDIO));
    }

    private void restoreSubtitleFormat() {
        getController().setFormat(mPlayerData.getFormat(FormatItem.TYPE_SUBTITLE));
    }

    private void saveState() {
        savePosition();
        updateHistory();
        //persistState();
    }

    private void savePosition() {
        Video video = getVideo();

        if (video == null || getController() == null || !getController().containsMedia()) {
            return;
        }

        // Exceptional cases:
        // 1) Track is ended
        // 2) Pause on end enabled
        // 3) Watching live stream in real time
        long lengthMs = getController().getDurationMs();
        long positionMs = getController().getPositionMs();
        long remainsMs = lengthMs - positionMs;
        boolean isPositionActual = remainsMs > 1_000;
        boolean isRealLiveStream = video.isLive && remainsMs < LIVE_THRESHOLD_MS;
        if ((isPositionActual && !isRealLiveStream) || !getPlayEnabled()) { // Is pause after each video enabled?
            mStateService.save(new State(video.videoId, positionMs, lengthMs, getController().getSpeed()));
            // Sync video. You could safely use it later to restore state.
            video.percentWatched = positionMs / (lengthMs / 100f);
        } else {
            // Mark video as fully viewed. This could help to restore proper progress marker on the video card later.
            mStateService.save(new State(video.videoId, lengthMs, lengthMs, 1.0f));
            video.percentWatched = 100;

            // NOTE: Storage optimization!!!
            // Reset position when video almost ended
            //resetPosition(video);
        }

        Playlist.instance().sync(video);
    }

    private void restorePosition() {
        Video item = getVideo();

        State state = mStateService.getByVideoId(item.videoId);

        // Ignore up to 10% watched because the video might be opened on phone and closed immediately.
        boolean containsWebPosition = item.percentWatched > RESTORE_POSITION_PERCENTS;
        boolean stateIsOutdated = state == null || state.timestamp < item.timestamp;
        if (containsWebPosition && stateIsOutdated) {
            // Web state is buggy on short videos (e.g. video clips)
            boolean isLongVideo = getController().getDurationMs() > MUSIC_VIDEO_MAX_DURATION_MS;
            if (isLongVideo) {
                state = new State(item.videoId, convertToMs(item.percentWatched));
            }
        }

        // Web live position is broken. Ignore it.
        if (stateIsOutdated && item.isLive) {
            state = null;
        }

        // Set actual position for live videos with uncommon length
        if (state == null && item.isLive && getController().getDurationMs() > Video.MAX_DURATION_MS) {
            state = new State(item.videoId, item.getLiveBufferDurationMs());
        }

        // Do I need to check that item isn't live? (state != null && !item.isLive)
        if (state != null) {
            long remainsMs = getController().getDurationMs() - state.positionMs;
            // Url list videos at this stage has undefined (-1) length. So, we need to ensure that remains is positive.
            boolean isVideoEnded = remainsMs >= 0 && remainsMs < (item.isLive ? 30_000 : 1_000); // live buffer fix
            if (!isVideoEnded || !getPlayEnabled()) {
                getController().setPositionMs(state.positionMs);
            }
        }

        if (!mIsPlayBlocked) {
            getController().setPlayWhenReady(getPlayEnabled());
        }
    }

    private void updateHistory() {
        Video video = getVideo();

        if (video == null || !getController().containsMedia()) {
            return;
        }

        RxUtils.disposeActions(mHistoryAction);

        MediaService service = YouTubeMediaService.instance();
        MediaItemService mediaItemManager = service.getMediaItemService();

        Observable<Void> historyObservable;

        long positionSec = video.isLive ? 0 : getController().getPositionMs() / 1_000;

        if (video.mediaItem != null) {
            historyObservable = mediaItemManager.updateHistoryPositionObserve(video.mediaItem, positionSec);
        } else { // video launched form ATV channels
            historyObservable = mediaItemManager.updateHistoryPositionObserve(video.videoId, positionSec);
        }

        mHistoryAction = RxUtils.execute(historyObservable);
    }

    /**
     * Restore position from description time code
     */
    private void restorePendingPosition() {
        Video item = getVideo();

        if (item.pendingPosMs > 0) {
            getController().setPositionMs(item.pendingPosMs);
            item.pendingPosMs = 0;
        }
    }

    private void restoreSpeed() {
        Video item = getVideo();
        boolean isLiveThreshold = getController().getDurationMs() - getController().getPositionMs() < LIVE_THRESHOLD_MS;
        boolean isLive = item.isLive && isLiveThreshold;
        boolean isMusic = item.belongsToMusic();

        if (isLive || isMusic) {
            getController().setSpeed(1.0f);
        } else {
            State state = mStateService.getByVideoId(item.videoId);
            getController().setSpeed(state != null && mPlayerData.isRememberSpeedEachEnabled() ? state.speed : mPlayerData.getSpeed());
        }
    }

    private long convertToMs(float percentWatched) {
        if (percentWatched >= 100) {
            return -1;
        }

        long newPositionMs = (long) (getController().getDurationMs() / 100 * percentWatched);

        boolean samePositions = Math.abs(newPositionMs - getController().getPositionMs()) < 10_000;

        if (samePositions) {
            newPositionMs = -1;
        }

        return newPositionMs;
    }

    public void blockPlay(boolean block) {
        mIsPlayBlocked = block;
    }

    public void setPlayEnabled(boolean isPlayEnabled) {
        Log.d(TAG, "setPlayEnabled %s", isPlayEnabled);
        mIsPlayEnabled = isPlayEnabled;
    }

    public boolean getPlayEnabled() {
        return mIsPlayEnabled;
    }

    private void restoreVolume() {
        getController().setVolume(mPlayerData.getPlayerVolume());
    }

    private void restoreFormats() {
        restoreVideoFormat();
        restoreAudioFormat();
        restoreSubtitleFormat();
    }

    private void showHideScreensaver(boolean show) {
        if (getActivity() instanceof MotherActivity) {
            ScreensaverManager screensaverManager = ((MotherActivity) getActivity()).getScreensaverManager();

            if (show) {
                screensaverManager.enableChecked();
            } else {
                screensaverManager.disableChecked();
            }
        }
    }

    /**
     * Actual video that match currently loaded one.
     */
    private Video getVideo() {
        return mVideo;
    }
}
