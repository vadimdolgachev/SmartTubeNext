package com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers;

import com.liskovsoft.mediaserviceinterfaces.MediaItemManager;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.mediaserviceinterfaces.data.SponsorSegment;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.PlayerEventListenerHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.SuggestionsLoader.MetadataListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.ContentBlockData;
import com.liskovsoft.smartyoutubetv2.common.utils.RxUtils;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ContentBlockManager extends PlayerEventListenerHelper implements MetadataListener {
    private static final String TAG = ContentBlockManager.class.getSimpleName();
    private MediaItemManager mMediaItemManager;
    private ContentBlockData mContentBlockData;
    private List<SponsorSegment> mSponsorSegments;
    private Disposable mProgressAction;
    private Disposable mSegmentsAction;
    private boolean mIsSameSegment;
    private Map<String, Integer> mLocalizedMapping;

    @Override
    public void onInitDone() {
        MediaService mediaService = YouTubeMediaService.instance();
        mMediaItemManager = mediaService.getMediaItemManager();
        mContentBlockData = ContentBlockData.instance(getActivity());
        initLocalizedMapping();
    }

    private void initLocalizedMapping() {
        mLocalizedMapping = new HashMap<>();
        mLocalizedMapping.put(SponsorSegment.CATEGORY_SPONSOR, R.string.content_block_sponsor);
        mLocalizedMapping.put(SponsorSegment.CATEGORY_INTRO, R.string.content_block_intro);
        mLocalizedMapping.put(SponsorSegment.CATEGORY_OUTRO, R.string.content_block_outro);
        mLocalizedMapping.put(SponsorSegment.CATEGORY_SELF_PROMO, R.string.content_block_self_promo);
        mLocalizedMapping.put(SponsorSegment.CATEGORY_INTERACTION, R.string.content_block_interaction);
        mLocalizedMapping.put(SponsorSegment.CATEGORY_MUSIC_OFF_TOPIC, R.string.content_block_music_off_topic);
    }

    @Override
    public void onVideoLoaded(Video item) {
        disposeActions();

        if (mContentBlockData.isSponsorBlockEnabled() && checkVideo(item)) {
            updateSponsorSegmentsAndWatch(item);
        }
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        // Disable sponsor for the live streams.
        // Fix when using remote control.
        if (!mContentBlockData.isSponsorBlockEnabled() || !checkVideo(getController().getVideo())) {
            disposeActions();
        }
    }

    @Override
    public void onEngineReleased() {
        disposeActions();
    }

    private boolean checkVideo(Video video) {
        return video != null && !video.isLive && !video.isUpcoming;
    }

    private void updateSponsorSegmentsAndWatch(Video item) {
        if (item == null || item.videoId == null) {
            mSponsorSegments = null;
            return;
        }

        mSegmentsAction = mMediaItemManager.getSponsorSegmentsObserve(item.videoId, mContentBlockData.getCategories())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        segments -> {
                            mSponsorSegments = segments;
                            startPlaybackWatcher();
                        },
                        error -> Log.d(TAG, "It's ok. Nothing to block in this video. Error msg: %s", error.getMessage())
                );
    }

    private void startPlaybackWatcher() {
        Observable<Long> playbackProgressObservable =
                Observable.interval(1, TimeUnit.SECONDS)
                        .map((val) -> getController().getPositionMs());

        mProgressAction = playbackProgressObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::skipSegment,
                        error -> Log.e(TAG, "startPlaybackWatcher error: %s", error.getMessage())
                );
    }

    private void disposeActions() {
        RxUtils.disposeActions(mProgressAction, mSegmentsAction);
    }

    private void skipSegment(long positionMs) {
        if (mSponsorSegments == null) {
            return;
        }

        boolean isSegmentFound = false;

        for (SponsorSegment segment : mSponsorSegments) {
            if (positionMs >= segment.getStartMs() && positionMs < segment.getEndMs()) {
                Integer resId = mLocalizedMapping.get(segment.getCategory());
                String localizedCategory = resId != null ? getActivity().getString(resId) : segment.getCategory();

                int type = mContentBlockData.getNotificationType();

                if (type == ContentBlockData.NOTIFICATION_TYPE_NONE || getController().isInPIPMode()) {
                    getController().setPositionMs(segment.getEndMs());
                } else if (type == ContentBlockData.NOTIFICATION_TYPE_TOAST) {
                    messageSkip(segment.getEndMs(), localizedCategory);
                } else if (type == ContentBlockData.NOTIFICATION_TYPE_DIALOG) {
                    confirmSkip(segment.getEndMs(), localizedCategory);
                }

                isSegmentFound = true;
                break;
            }
        }

        mIsSameSegment = isSegmentFound;
    }

    private void messageSkip(long skipPositionMs, String category) {
        MessageHelpers.showMessage(getActivity(), getActivity().getString(R.string.msg_skipping_segment, category));
        getController().setPositionMs(skipPositionMs);
    }

    private void confirmSkip(long skipPositionMs, String category) {
        if (mIsSameSegment) {
            return;
        }

        AppSettingsPresenter settingsPresenter = AppSettingsPresenter.instance(getActivity());
        settingsPresenter.clear();

        OptionItem sponsorBlockOption = UiOptionItem.from(
                getActivity().getString(R.string.confirm_segment_skip, category),
                option -> {
                    settingsPresenter.closeDialog();
                    getController().setPositionMs(skipPositionMs);
                }
        );

        settingsPresenter.appendSingleButton(sponsorBlockOption);
        settingsPresenter.setTimoutMs(skipPositionMs - getController().getPositionMs());

        settingsPresenter.showDialog(ContentBlockData.SPONSOR_BLOCK_NAME);
    }
}
