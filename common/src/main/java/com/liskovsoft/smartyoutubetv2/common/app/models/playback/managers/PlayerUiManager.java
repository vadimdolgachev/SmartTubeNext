package com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.liskovsoft.mediaserviceinterfaces.MediaItemManager;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.sharedutils.helpers.KeyHelpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.PlayerEventListenerHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controller.PlaybackEngineController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.SuggestionsLoader.MetadataListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.VideoMenuPresenter;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.SubtitleManager.OnSelectSubtitleStyle;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.SubtitleManager.SubtitleStyle;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.utils.RxUtils;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.Observable;

import java.util.ArrayList;
import java.util.List;

public class PlayerUiManager extends PlayerEventListenerHelper implements MetadataListener {
    private static final String TAG = PlayerUiManager.class.getSimpleName();
    private static final long SUGGESTIONS_RESET_TIMEOUT_MS = 500;
    private static final int SUBTITLE_STYLES_ID = 45;
    private final Handler mHandler;
    private final MediaItemManager mMediaItemManager;
    private boolean mEngineReady;
    private boolean mDebugViewEnabled;
    private PlayerData mPlayerData;
    private boolean mIsMetadataLoaded;
    private final Runnable mSuggestionsResetHandler = () -> getController().resetSuggestedPosition();
    private final Runnable mUiAutoHideHandler = () -> {
        if (getController().isPlaying()) {
            if (!getController().isSuggestionsShown()) { // don't hide when suggestions is shown
                getController().showControls(false);
            }
        } else {
            // in seeking state? doing recheck...
            disableUiAutoHideTimeout();
            enableUiAutoHideTimeout();
        }
    };

    public PlayerUiManager() {
        mHandler = new Handler(Looper.getMainLooper());

        MediaService service = YouTubeMediaService.instance();
        mMediaItemManager = service.getMediaItemManager();
    }

    @Override
    public void onInitDone() {
        AppSettingsPresenter.instance(getActivity()).setPlayerUiManager(this);
        mPlayerData = PlayerData.instance(getActivity());
    }

    @Override
    public void onViewResumed() {
        getController().setVideoZoomMode(mPlayerData.getVideoZoomMode());
    }

    @Override
    public void onControlsShown(boolean shown) {
        disableUiAutoHideTimeout();

        if (shown) {
            enableUiAutoHideTimeout();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode) {
        disableUiAutoHideTimeout();
        disableSuggestionsResetTimeout();

        boolean controlsShown = getController().isControlsShown();

        if (KeyHelpers.isBackKey(keyCode)) {
            enableSuggestionsResetTimeout();
        } else if (KeyHelpers.isMenuKey(keyCode)) {
            getController().showControls(!controlsShown);

            if (controlsShown) {
                enableSuggestionsResetTimeout();
            }
        } else if (KeyHelpers.isConfirmKey(keyCode) && !controlsShown) {
            switch (mPlayerData.getOKButtonBehavior()) {
                case PlayerData.ONLY_UI:
                    getController().showControls(true);
                    return true; // don't show ui
                case PlayerData.UI_AND_PAUSE:
                    // NOP
                    break;
                case PlayerData.ONLY_PAUSE:
                    getController().setPlay(!getController().getPlay());
                    return true; // don't show ui
            }
        } else if (KeyHelpers.isStopKey(keyCode)) {
            getController().exit();
            return true;
        }

        enableUiAutoHideTimeout();

        return false;
    }

    @Override
    public void onChannelClicked() {
        ChannelPresenter.instance(getActivity()).openChannel(getController().getVideo());
    }

    @Override
    public void onSubtitlesClicked() {
        List<FormatItem> subtitleFormats = getController().getSubtitleFormats();

        String subtitlesCategoryTitle = getActivity().getString(R.string.subtitle_category_title);
        String subtitleFormatsTitle = getActivity().getString(R.string.subtitle_language);

        AppSettingsPresenter settingsPresenter = AppSettingsPresenter.instance(getActivity());

        settingsPresenter.clear();

        settingsPresenter.appendRadioCategory(subtitleFormatsTitle,
                UiOptionItem.from(subtitleFormats,
                        option -> getController().setFormat(UiOptionItem.toFormat(option)),
                        getActivity().getString(R.string.subtitles_disabled)));

        settingsPresenter.showDialog(subtitlesCategoryTitle);
    }

    @Override
    public void onPlaylistAddClicked() {
        VideoMenuPresenter mp = VideoMenuPresenter.instance(getActivity());

        mp.showPlaylistMenu(getController().getVideo());
    }

    @Override
    public void onVideoStatsClicked(boolean enabled) {
        mDebugViewEnabled = enabled;
        getController().showDebugView(enabled);
    }

    @Override
    public void onEngineInitialized() {
        mEngineReady = true;
    }

    @Override
    public void onVideoLoaded(Video item) {
        mIsMetadataLoaded = false; // metadata isn't loaded yet at this point
        resetButtonStates();

        // Next lines on engine initialized stage cause other listeners to disappear.
        getController().showDebugView(mDebugViewEnabled);
        getController().setDebugButtonState(mDebugViewEnabled);

        if (mPlayerData.getSeekPreviewMode() != PlayerData.SEEK_PREVIEW_NONE) {
            getController().loadStoryboard();
        }
    }

    private void resetButtonStates() {
        getController().setLikeButtonState(false);
        getController().setDislikeButtonState(false);
        getController().setSubscribeButtonState(false);
    }

    @Override
    public void onEngineReleased() {
        Log.d(TAG, "Engine released. Disabling all callbacks...");
        mEngineReady = false;

        disposeTimeouts();
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        mIsMetadataLoaded = true;
        getController().setLikeButtonState(metadata.getLikeStatus() == MediaItemMetadata.LIKE_STATUS_LIKE);
        getController().setDislikeButtonState(metadata.getLikeStatus() == MediaItemMetadata.LIKE_STATUS_DISLIKE);
        getController().setSubscribeButtonState(metadata.isSubscribed());
    }

    @Override
    public void onSuggestionItemLongClicked(Video item) {
        VideoMenuPresenter.instance(getActivity()).showVideoMenu(item);
    }

    private void showBriefInfo(boolean subscribed) {
        if (subscribed) {
            MessageHelpers.showMessage(getActivity(), R.string.subscribed_to_channel);
        } else {
            MessageHelpers.showMessage(getActivity(), R.string.unsubscribed_from_channel);
        }
    }

    @Override
    public void onSubscribeClicked(boolean subscribed) {
        if (!mIsMetadataLoaded) {
            MessageHelpers.showLongMessage(getActivity(), R.string.wait_data_loading);
            getController().setSubscribeButtonState(!subscribed);
            return;
        }

        if (subscribed) {
            callMediaItemObservable(mMediaItemManager::subscribeObserve);
        } else {
            callMediaItemObservable(mMediaItemManager::unsubscribeObserve);
        }

        showBriefInfo(subscribed);
    }

    @Override
    public void onThumbsDownClicked(boolean thumbsDown) {
        if (!mIsMetadataLoaded) {
            MessageHelpers.showLongMessage(getActivity(), R.string.wait_data_loading);
            getController().setDislikeButtonState(!thumbsDown);
            return;
        }

        if (thumbsDown) {
            callMediaItemObservable(mMediaItemManager::setDislikeObserve);
        } else {
            callMediaItemObservable(mMediaItemManager::removeDislikeObserve);
        }
    }

    @Override
    public void onThumbsUpClicked(boolean thumbsUp) {
        if (!mIsMetadataLoaded) {
            MessageHelpers.showLongMessage(getActivity(), R.string.wait_data_loading);
            getController().setLikeButtonState(!thumbsUp);
            return;
        }

        if (thumbsUp) {
            callMediaItemObservable(mMediaItemManager::setLikeObserve);
        } else {
            callMediaItemObservable(mMediaItemManager::removeLikeObserve);
        }
    }

    @Override
    public void onVideoSpeedClicked() {
        List<OptionItem> items = new ArrayList<>();

        AppSettingsPresenter settingsPresenter = AppSettingsPresenter.instance(getActivity());
        settingsPresenter.clear();

        // suppose live stream if buffering near the end
        // boolean isStream = Math.abs(player.getDuration() - player.getCurrentPosition()) < 10_000;
        intSpeedItems(settingsPresenter, items, new float[]{0.25f, 0.5f, 0.75f, 0.80f, 0.85f, 0.90f, 0.95f, 1.0f, 1.1f, 1.15f, 1.25f, 1.5f, 1.75f, 2f, 2.25f, 2.5f, 2.75f, 3.0f});

        settingsPresenter.appendRadioCategory(getActivity().getString(R.string.video_speed), items);
        settingsPresenter.showDialog();
    }

    @Override
    public void onSearchClicked() {
        SearchPresenter.instance(getActivity()).startSearch(null);
    }

    @Override
    public void onVideoZoomClicked() {
        OptionCategory videoZoomCategory = createVideoZoomCategory(
                getActivity(), mPlayerData, () -> getController().setVideoZoomMode(mPlayerData.getVideoZoomMode()));

        AppSettingsPresenter settingsPresenter = AppSettingsPresenter.instance(getActivity());
        settingsPresenter.clear();
        settingsPresenter.appendRadioCategory(videoZoomCategory.title, videoZoomCategory.options);
        settingsPresenter.showDialog();
    }

    @Override
    public void onPipClicked() {
        getController().showControls(false);
        getController().setPlaybackMode(PlaybackEngineController.BACKGROUND_MODE_PIP);
        getController().exit();
    }

    private void intSpeedItems(AppSettingsPresenter settingsPresenter, List<OptionItem> items, float[] speedValues) {
        for (float speed : speedValues) {
            items.add(UiOptionItem.from(
                    String.valueOf(speed),
                    optionItem -> {
                        mPlayerData.setSpeed(speed);
                        getController().setSpeed(speed);
                        //settingsPresenter.closeDialog();
                    },
                    getController().getSpeed() == speed));
        }
    }

    public void disableUiAutoHideTimeout() {
        Log.d(TAG, "Stopping auto hide ui timer...");
        mHandler.removeCallbacks(mUiAutoHideHandler);
    }

    public void enableUiAutoHideTimeout() {
        Log.d(TAG, "Starting auto hide ui timer...");
        if (mEngineReady && mPlayerData.getUIHideTimoutSec() > 0) {
            mHandler.postDelayed(mUiAutoHideHandler, mPlayerData.getUIHideTimoutSec() * 1_000L);
        }
    }

    private void disableSuggestionsResetTimeout() {
        Log.d(TAG, "Stopping reset position timer...");
        mHandler.removeCallbacks(mSuggestionsResetHandler);
    }

    private void enableSuggestionsResetTimeout() {
        Log.d(TAG, "Starting reset position timer...");
        if (mEngineReady) {
            mHandler.postDelayed(mSuggestionsResetHandler, SUGGESTIONS_RESET_TIMEOUT_MS);
        }
    }

    private void disposeTimeouts() {
        disableUiAutoHideTimeout();
        disableSuggestionsResetTimeout();
    }

    private void callMediaItemObservable(MediaItemObservable callable) {
        Video video = getController().getVideo();

        if (video == null || video.mediaItem == null) {
            Log.e(TAG, "Seems that video isn't initialized yet. Cancelling...");
            return;
        }

        Observable<Void> observable = callable.call(video.mediaItem);

        RxUtils.execute(observable);
    }

    private interface MediaItemObservable {
        Observable<Void> call(MediaItem item);
    }

    public static OptionCategory createSubtitleStylesCategory(Context context, PlayerData playerData) {
        return createSubtitleStylesCategory(context, playerData, style -> {});
    }

    private static OptionCategory createSubtitleStylesCategory(Context context, PlayerData playerData, OnSelectSubtitleStyle onSelectSubtitleStyle) {
        List<SubtitleStyle> subtitleStyles = playerData.getSubtitleStyles();
        
        String subtitleStyleTitle = context.getString(R.string.subtitle_style);

        return OptionCategory.from(SUBTITLE_STYLES_ID, OptionCategory.TYPE_RADIO, subtitleStyleTitle, fromSubtitleStyles(context, playerData, subtitleStyles, onSelectSubtitleStyle));
    }

    private static List<OptionItem> fromSubtitleStyles(Context context, PlayerData playerData, List<SubtitleStyle> subtitleStyles, OnSelectSubtitleStyle onSelectSubtitleStyle) {
        List<OptionItem> styleOptions = new ArrayList<>();

        for (SubtitleStyle subtitleStyle : subtitleStyles) {
            styleOptions.add(UiOptionItem.from(
                    context.getString(subtitleStyle.nameResId),
                    option -> {
                        playerData.setSubtitleStyle(subtitleStyle);
                        onSelectSubtitleStyle.onSelectSubtitleStyle(subtitleStyle);
                    },
                    subtitleStyle.equals(playerData.getSubtitleStyle())));
        }

        return styleOptions;
    }

    public static OptionCategory createVideoZoomCategory(Context context, PlayerData playerData) {
        return createVideoZoomCategory(context, playerData, () -> {});
    }

    private static OptionCategory createVideoZoomCategory(Context context, PlayerData playerData, Runnable onSelectZoomMode) {
        List<OptionItem> options = new ArrayList<>();

        for (int[] pair : new int[][] {
                {R.string.video_zoom_default, PlaybackEngineController.ZOOM_MODE_DEFAULT},
                {R.string.video_zoom_fit_width, PlaybackEngineController.ZOOM_MODE_FIT_WIDTH},
                {R.string.video_zoom_fit_height, PlaybackEngineController.ZOOM_MODE_FIT_HEIGHT},
                {R.string.video_zoom_fit_both, PlaybackEngineController.ZOOM_MODE_FIT_BOTH},
                {R.string.video_zoom_stretch, PlaybackEngineController.ZOOM_MODE_STRETCH}}) {
            options.add(UiOptionItem.from(context.getString(pair[0]),
                    optionItem -> {
                        playerData.setVideoZoomMode(pair[1]);
                        onSelectZoomMode.run();
                    },
                    playerData.getVideoZoomMode() == pair[1]));
        }

        String videoZoomTitle = context.getString(R.string.video_zoom);

        return OptionCategory.from(SUBTITLE_STYLES_ID, OptionCategory.TYPE_RADIO, videoZoomTitle, options);
    }
}
