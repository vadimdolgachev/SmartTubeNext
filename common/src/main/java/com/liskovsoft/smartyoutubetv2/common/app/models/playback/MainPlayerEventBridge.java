package com.liskovsoft.smartyoutubetv2.common.app.models.playback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import androidx.fragment.app.Fragment;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controller.PlaybackController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.PlayerEventListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.PlayerUiEventListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.ViewEventListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.AutoFrameRateManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.ContentBlockManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.HQDialogManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.LiveChatManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.PlayerUIManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.RemoteControlManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.SuggestionsLoaderManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.VideoLoaderManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.VideoStateManager;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.misc.TickleManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainPlayerEventBridge implements PlayerEventListener {
    private static final String TAG = MainPlayerEventBridge.class.getSimpleName();
    private final ArrayList<PlayerEventListener> mEventListeners = new ArrayList<PlayerEventListener>() {
        @Override
        public boolean add(PlayerEventListener listener) {
            ((PlayerEventListenerHelper) listener).setBridge(MainPlayerEventBridge.this);

            return super.add(listener);
        }
    };
    @SuppressLint("StaticFieldLeak")
    private static MainPlayerEventBridge sInstance;
    private WeakReference<PlaybackController> mController = new WeakReference<>(null);
    private WeakReference<Activity> mActivity = new WeakReference<>(null);

    public MainPlayerEventBridge(Context context) {
        if (context instanceof Activity) {
            mActivity = new WeakReference<>((Activity) context);
        }

        SuggestionsLoaderManager suggestionsLoader = new SuggestionsLoaderManager();
        VideoLoaderManager videoLoader = new VideoLoaderManager(suggestionsLoader);
        PlayerUIManager uiManager = new PlayerUIManager(videoLoader);
        VideoStateManager stateUpdater = new VideoStateManager();
//        ContentBlockManager contentBlockManager = new ContentBlockManager();
        LiveChatManager liveChatManager = new LiveChatManager();

//        RemoteControlManager commandManager = new RemoteControlManager(context, suggestionsLoader, videoLoader);
        HQDialogManager hqDialogManager = new HQDialogManager(stateUpdater);
        AutoFrameRateManager autoFrameRateManager = new AutoFrameRateManager(hqDialogManager, stateUpdater);

        suggestionsLoader.addMetadataListener(uiManager);
        suggestionsLoader.addMetadataListener(videoLoader);
        suggestionsLoader.addMetadataListener(stateUpdater);
//        suggestionsLoader.addMetadataListener(contentBlockManager);
        suggestionsLoader.addMetadataListener(liveChatManager);

        // NOTE: position matters!!!
        mEventListeners.add(autoFrameRateManager);
        mEventListeners.add(uiManager);
        mEventListeners.add(hqDialogManager);
        mEventListeners.add(stateUpdater);
        mEventListeners.add(suggestionsLoader);
        mEventListeners.add(videoLoader);
//        mEventListeners.add(commandManager);
//        mEventListeners.add(contentBlockManager);
        mEventListeners.add(liveChatManager);
    }

    public static MainPlayerEventBridge instance(Context context) {
        if (sInstance == null) {
            sInstance = new MainPlayerEventBridge(context);
        }

        return sInstance;
    }
    
    public void setController(PlaybackController controller) {
        if (controller != null) {
            if (mController.get() != controller) { // Be ready to re-init after app exit
                mController = new WeakReference<>(controller);
                mActivity = new WeakReference<>(((Fragment) controller).getActivity());
                process(PlayerEventListener::onInitDone);
            }
        }
    }

    public PlaybackController getController() {
        return mController.get();
    }

    public Activity getActivity() {
        return mActivity.get();
    }

    // Core events

    @Override
    public void openVideo(Video item) {
        process(listener -> listener.openVideo(item));
    }

    @Override
    public void onFinish() {
        process(PlayerEventListener::onFinish);
    }

    @Override
    public void onInitDone() {
        // NOP. Internal event.
    }

    // End core events

    // Helpers

    private boolean chainProcess(ChainProcessor processor) {
        boolean result = false;

        for (PlayerEventListener listener : mEventListeners) {
            result = processor.process(listener);

            if (result) {
                break;
            }
        }

        return result;
    }

    private interface ChainProcessor {
        boolean process(PlayerEventListener listener);
    }

    private void process(Processor processor) {
        for (PlayerEventListener listener : mEventListeners) {
            processor.process(listener);
        }
    }

    private interface Processor {
        void process(PlayerEventListener listener);
    }

    // End Helpers

    // Common events

    @Override
    public void onViewCreated() {
        process(ViewEventListener::onViewCreated);
    }

    @Override
    public void onViewDestroyed() {
        process(ViewEventListener::onViewDestroyed);
    }

    @Override
    public void onViewPaused() {
        process(ViewEventListener::onViewPaused);
    }

    @Override
    public void onViewResumed() {
        process(ViewEventListener::onViewResumed);
    }

    // End common events

    // Start engine events

    @Override
    public void onSourceChanged(Video item) {
        process(listener -> listener.onSourceChanged(item));
    }

    @Override
    public void onEngineInitialized() {
        TickleManager.instance().addListener(this);

        process(PlayerEventListener::onEngineInitialized);
    }

    @Override
    public void onEngineReleased() {
        TickleManager.instance().removeListener(this);

        process(PlayerEventListener::onEngineReleased);
    }

    @Override
    public void onEngineError(int type) {
        process(listener -> listener.onEngineError(type));
    }

    @Override
    public void onPlay() {
        process(PlayerEventListener::onPlay);
    }

    @Override
    public void onPause() {
        process(PlayerEventListener::onPause);
    }

    @Override
    public void onPlayClicked() {
        process(PlayerEventListener::onPlayClicked);
    }

    @Override
    public void onPauseClicked() {
        process(PlayerEventListener::onPauseClicked);
    }
    
    @Override
    public void onSeekEnd() {
        process(PlayerEventListener::onSeekEnd);
    }

    @Override
    public void onPlayEnd() {
        process(PlayerEventListener::onPlayEnd);
    }

    @Override
    public void onBuffering() {
        process(PlayerEventListener::onBuffering);
    }

    @Override
    public boolean onKeyDown(int keyCode) {
        return chainProcess(listener -> listener.onKeyDown(keyCode));
    }

    @Override
    public void onRepeatModeClicked(int modeIndex) {
        process(listener -> listener.onRepeatModeClicked(modeIndex));
    }

    @Override
    public void onVideoLoaded(Video item) {
        process(listener -> listener.onVideoLoaded(item));
    }

    @Override
    public void onTickle() {
        process(PlayerEventListener::onTickle);
    }

    // End engine events

    // Start UI events

    @Override
    public void onSuggestionItemClicked(Video item) {
        process(listener -> listener.onSuggestionItemClicked(item));
    }

    @Override
    public void onSuggestionItemLongClicked(Video item) {
        process(listener -> listener.onSuggestionItemLongClicked(item));
    }

    @Override
    public void onScrollEnd(Video item) {
        process(listener -> listener.onScrollEnd(item));
    }

    @Override
    public boolean onPreviousClicked() {
        return chainProcess(PlayerEventListener::onPreviousClicked);
    }

    @Override
    public boolean onNextClicked() {
        return chainProcess(PlayerEventListener::onNextClicked);
    }

    @Override
    public void onHighQualityClicked() {
        process(PlayerUiEventListener::onHighQualityClicked);
    }

    @Override
    public void onSubscribeClicked(boolean subscribed) {
        process(listener -> listener.onSubscribeClicked(subscribed));
    }

    @Override
    public void onDislikeClicked(boolean dislike) {
        process(listener -> listener.onDislikeClicked(dislike));
    }

    @Override
    public void onLikeClicked(boolean like) {
        process(listener -> listener.onLikeClicked(like));
    }

    @Override
    public void onChannelClicked() {
        process(PlayerUiEventListener::onChannelClicked);
    }

    @Override
    public void onTrackSelected(FormatItem track) {
        process(listener -> listener.onTrackSelected(track));
    }

    @Override
    public void onSubtitleClicked(boolean enabled) {
        process(listener -> listener.onSubtitleClicked(enabled));
    }

    @Override
    public void onSubtitleLongClicked(boolean enabled) {
        process(listener -> listener.onSubtitleLongClicked(enabled));
    }

    @Override
    public void onControlsShown(boolean shown) {
        process(listener -> listener.onControlsShown(shown));
    }

    @Override
    public void onTrackChanged(FormatItem track) {
        process(listener -> listener.onTrackChanged(track));
    }

    @Override
    public void onPlaylistAddClicked() {
        process(PlayerUiEventListener::onPlaylistAddClicked);
    }

    @Override
    public void onDebugInfoClicked(boolean enabled) {
        process(listener -> listener.onDebugInfoClicked(enabled));
    }

    @Override
    public void onVideoSpeedClicked(boolean enabled) {
        process(listener -> listener.onVideoSpeedClicked(enabled));
    }

    @Override
    public void onVideoSpeedLongClicked(boolean enabled) {
        process(listener -> listener.onVideoSpeedLongClicked(enabled));
    }

    @Override
    public void onSeekIntervalClicked() {
        process(PlayerUiEventListener::onSeekIntervalClicked);
    }

    @Override
    public void onContentBlockClicked(boolean enabled) {
        process(listener -> listener.onContentBlockClicked(enabled));
    }

    @Override
    public void onChatClicked(boolean enabled) {
        process(listener -> listener.onChatClicked(enabled));
    }

    @Override
    public void onChatLongClicked(boolean enabled) {
        process(listener -> listener.onChatLongClicked(enabled));
    }

    @Override
    public void onVideoInfoClicked() {
        process(PlayerUiEventListener::onVideoInfoClicked);
    }

    @Override
    public void onShareLinkClicked() {
        process(PlayerUiEventListener::onShareLinkClicked);
    }

    @Override
    public void onSearchClicked() {
        process(PlayerUiEventListener::onSearchClicked);
    }

    @Override
    public void onVideoZoomClicked() {
        process(PlayerUiEventListener::onVideoZoomClicked);
    }

    @Override
    public void onPipClicked() {
        process(PlayerUiEventListener::onPipClicked);
    }

    @Override
    public void onScreenOffClicked() {
        process(PlayerUiEventListener::onScreenOffClicked);
    }

    @Override
    public void onPlaybackQueueClicked() {
        process(PlayerUiEventListener::onPlaybackQueueClicked);
    }

    // End UI events
}
