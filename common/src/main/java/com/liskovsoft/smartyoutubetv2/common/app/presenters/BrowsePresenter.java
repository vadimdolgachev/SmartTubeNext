package com.liskovsoft.smartyoutubetv2.common.app.presenters;

import android.annotation.SuppressLint;
import android.content.Context;
import com.liskovsoft.mediaserviceinterfaces.MediaGroupManager;
import com.liskovsoft.mediaserviceinterfaces.MediaItemManager;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.SignInManager;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.CategoryEmptyError;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.SignInError;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.ChannelUploadsMenuPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.VideoActionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.SectionMenuPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.VideoMenuPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.SectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.VideoGroupPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.AppDataSourceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.RxUtils;
import com.liskovsoft.smartyoutubetv2.common.utils.ScreenHelper;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.common.helpers.ServiceHelper;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class BrowsePresenter extends BasePresenter<BrowseView> implements SectionPresenter, VideoGroupPresenter {
    private static final String TAG = BrowsePresenter.class.getSimpleName();
    private static final long HEADER_REFRESH_PERIOD_MS = 120 * 60 * 1_000;
    private static final int MIN_GROUP_SIZE = 13;
    private static final int SHORTS_LEN_MS = 50 * 1_000;
    @SuppressLint("StaticFieldLeak")
    private static BrowsePresenter sInstance;
    private final PlaybackPresenter mPlaybackPresenter;
    private final MainUIData mMainUIData;
    private final GeneralData mGeneralData;
    private final List<BrowseSection> mSections;
    private final Map<Integer, Observable<MediaGroup>> mGridMapping;
    private final Map<Integer, Observable<List<MediaGroup>>> mRowMapping;
    private final Map<Integer, Callable<List<SettingsItem>>> mSettingsGridMapping;
    private final AppDataSourceManager mDataSourcePresenter;
    private final MediaGroupManager mGroupManager;
    private final MediaItemManager mItemManager;
    private final SignInManager mSignInManager;
    private Disposable mUpdateAction;
    private Disposable mContinueAction;
    private Disposable mSignCheckAction;
    private BrowseSection mCurrentSection;
    private long mLastUpdateTimeMs;
    private int mStartSectionIndex;

    private BrowsePresenter(Context context) {
        super(context);
        mDataSourcePresenter = AppDataSourceManager.instance();
        mPlaybackPresenter = PlaybackPresenter.instance(context);
        mSections = new ArrayList<>();
        mGridMapping = new HashMap<>();
        mRowMapping = new HashMap<>();
        mSettingsGridMapping = new HashMap<>();
        mMainUIData = MainUIData.instance(context);
        mGeneralData = GeneralData.instance(context);
        ScreenHelper.initPipMode(context);
        ScreenHelper.updateScreenInfo(context);

        MediaService mediaService = YouTubeMediaService.instance();
        mGroupManager = mediaService.getMediaGroupManager();
        mItemManager = mediaService.getMediaItemManager();
        mSignInManager = mediaService.getSignInManager();

        initCategories();
    }

    public static BrowsePresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new BrowsePresenter(context);
        }

        sInstance.setContext(context);

        return sInstance;
    }

    public static void unhold() {
        sInstance = null;
    }

    @Override
    public void onViewInitialized() {
        if (getView() == null) {
            return;
        }

        updateChannelSorting();
        updatePlaylistsStyle();
        updateSections();
        getView().selectSection(mStartSectionIndex);
        showBootDialogs();
        Utils.updateRemoteControlService(getContext());
    }

    private void initCategories() {
        cleanupPinnedItems();

        initCategoryHeaders();
        initPinnedHeaders();

        initCategoryCallbacks();
        initPinnedCallbacks();

        initSettingsSubCategories();
    }

    private void initCategoryHeaders() {
        int uploadsType = mMainUIData.isUploadsOldLookEnabled() ? BrowseSection.TYPE_GRID : BrowseSection.TYPE_MULTI_GRID;

        mSections.add(new BrowseSection(MediaGroup.TYPE_HOME, getContext().getString(R.string.header_home), BrowseSection.TYPE_ROW, R.drawable.icon_home));
        mSections.add(new BrowseSection(MediaGroup.TYPE_GAMING, getContext().getString(R.string.header_gaming), BrowseSection.TYPE_ROW, R.drawable.icon_gaming));
        mSections.add(new BrowseSection(MediaGroup.TYPE_NEWS, getContext().getString(R.string.header_news), BrowseSection.TYPE_ROW, R.drawable.icon_news));
        mSections.add(new BrowseSection(MediaGroup.TYPE_MUSIC, getContext().getString(R.string.header_music), BrowseSection.TYPE_ROW, R.drawable.icon_music));
        mSections.add(new BrowseSection(MediaGroup.TYPE_CHANNEL_UPLOADS, getContext().getString(R.string.header_channels), uploadsType, R.drawable.icon_channels, true));
        mSections.add(new BrowseSection(MediaGroup.TYPE_SUBSCRIPTIONS, getContext().getString(R.string.header_subscriptions), BrowseSection.TYPE_GRID, R.drawable.icon_subscriptions, true));
        mSections.add(new BrowseSection(MediaGroup.TYPE_HISTORY, getContext().getString(R.string.header_history), BrowseSection.TYPE_GRID, R.drawable.icon_history, true));
        mSections.add(new BrowseSection(MediaGroup.TYPE_USER_PLAYLISTS, getContext().getString(R.string.header_playlists), BrowseSection.TYPE_ROW, R.drawable.icon_playlist, true));

        if (mGeneralData.isSettingsSectionEnabled()) {
            mSections.add(new BrowseSection(MediaGroup.TYPE_SETTINGS, getContext().getString(R.string.header_settings), BrowseSection.TYPE_SETTINGS_GRID, R.drawable.icon_settings));
        }
    }

    private void initCategoryCallbacks() {
        mRowMapping.put(MediaGroup.TYPE_HOME, mGroupManager.getHomeObserve());
        mRowMapping.put(MediaGroup.TYPE_NEWS, mGroupManager.getNewsObserve());
        mRowMapping.put(MediaGroup.TYPE_MUSIC, mGroupManager.getMusicObserve());
        mRowMapping.put(MediaGroup.TYPE_GAMING, mGroupManager.getGamingObserve());
        mRowMapping.put(MediaGroup.TYPE_USER_PLAYLISTS, mGroupManager.getPlaylistsObserve());

        mGridMapping.put(MediaGroup.TYPE_SUBSCRIPTIONS, mGroupManager.getSubscriptionsObserve());
        mGridMapping.put(MediaGroup.TYPE_HISTORY, mGroupManager.getHistoryObserve());
        mGridMapping.put(MediaGroup.TYPE_CHANNEL_UPLOADS, mGroupManager.getSubscribedChannelsUpdateObserve());
    }

    private void cleanupPinnedItems() {
        Set<Video> pinnedItems = mGeneralData.getPinnedItems();

        Helpers.removeIf(pinnedItems, value -> {
            if (value == null) {
                return true;
            }

            value.videoId = null;
            return value.playlistId == null;
        });
    }

    private void initPinnedHeaders() {
        Set<Video> pinnedItems = mGeneralData.getPinnedItems();

        for (Video item : pinnedItems) {
            if (item != null) {
                BrowseSection category = new BrowseSection(item.hashCode(), item.title, BrowseSection.TYPE_GRID, item.cardImageUrl, true, item);
                mSections.add(category);
            }
        }
    }

    private void initPinnedCallbacks() {
        Set<Video> pinnedItems = mGeneralData.getPinnedItems();

        for (Video item : pinnedItems) {
            if (item != null) {
                mGridMapping.put(item.hashCode(), ChannelUploadsPresenter.instance(getContext()).obtainPlaylistObservable(item));
            }
        }
    }

    private void initSettingsSubCategories() {
        mSettingsGridMapping.put(MediaGroup.TYPE_SETTINGS, () -> mDataSourcePresenter.getSettingItems(getContext()));
    }

    public void updateSections() {
        int index = 0;

        for (BrowseSection section : mSections) {
            section.setEnabled(section.getId() == MediaGroup.TYPE_SETTINGS || mGeneralData.isBrowseSectionEnabled(section.getId()));

            if (section.isEnabled()) {
                if (section.getId() == mGeneralData.getBootSectionId()) {
                    mStartSectionIndex = index;
                }
                getView().addSection(index++, section);
            } else {
                getView().removeSection(section);
            }
        }
    }

    public void updateChannelSorting() {
        int sortingType = mMainUIData.getChannelCategorySorting();

        switch (sortingType) {
            case MainUIData.CHANNEL_SORTING_UPDATE:
                mGridMapping.put(MediaGroup.TYPE_CHANNEL_UPLOADS, mGroupManager.getSubscribedChannelsUpdateObserve());
                break;
            case MainUIData.CHANNEL_SORTING_AZ:
                mGridMapping.put(MediaGroup.TYPE_CHANNEL_UPLOADS, mGroupManager.getSubscribedChannelsAZObserve());
                break;
            case MainUIData.CHANNEL_SORTING_LAST_VIEWED:
                mGridMapping.put(MediaGroup.TYPE_CHANNEL_UPLOADS, mGroupManager.getSubscribedChannelsLastViewedObserve());
                break;
        }
    }

    public void updatePlaylistsStyle() {
        int playlistsStyle = mMainUIData.getPlaylistsStyle();

        switch (playlistsStyle) {
            case MainUIData.PLAYLISTS_STYLE_GRID:
                mRowMapping.remove(MediaGroup.TYPE_USER_PLAYLISTS);
                mGridMapping.put(MediaGroup.TYPE_USER_PLAYLISTS, mGroupManager.getEmptyPlaylistsObserve());
                updateCategoryType(MediaGroup.TYPE_USER_PLAYLISTS, BrowseSection.TYPE_GRID);
                break;
            case MainUIData.PLAYLISTS_STYLE_ROWS:
                mGridMapping.remove(MediaGroup.TYPE_USER_PLAYLISTS);
                mRowMapping.put(MediaGroup.TYPE_USER_PLAYLISTS, mGroupManager.getPlaylistsObserve());
                updateCategoryType(MediaGroup.TYPE_USER_PLAYLISTS, BrowseSection.TYPE_ROW);
                break;
        }
    }

    private void updateCategoryType(int categoryId, int categoryType) {
        if (categoryType == -1 || categoryId == -1 || mSections == null) {
            return;
        }

        for (BrowseSection category : mSections) {
            if (category.getId() == categoryId) {
                category.setType(categoryType);
                break;
            }
        }
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        disposeActions();
    }

    @Override
    public void onVideoItemSelected(Video item) {
        if (getView() == null) {
            return;
        }

        if (mCurrentSection.getType() == BrowseSection.TYPE_MULTI_GRID && item.isChannelUploadsSection()) {
            if (mMainUIData.isUploadsAutoLoadEnabled()) {
                updateMultiGrid(item);
            } else {
                updateMultiGrid(null); // clear
            }
        }
    }

    @Override
    public void onVideoItemClicked(Video item) {
        if (getView() == null) {
            return;
        }

        if (mCurrentSection.getType() == BrowseSection.TYPE_MULTI_GRID && item.hasUploads()) { // Is Channels new look enabled?
            updateMultiGrid(item);
        } else {
            VideoActionPresenter.instance(getContext()).apply(item);
        }

        updateRefreshTime();
    }

    @Override
    public void onVideoItemLongClicked(Video item) {
        if (getView() == null) {
            return;
        }

        if (item.isChannelUploadsSection()) { // We need to be sure we exactly on Channels section
            ChannelUploadsMenuPresenter.instance(getContext()).showMenu(item);
        } else {
            VideoMenuPresenter.instance(getContext()).showMenu(item);
        }

        updateRefreshTime();
    }

    @Override
    public void onScrollEnd(Video item) {
        if (item == null) {
            Log.e(TAG, "Can't scroll. Video is null.");
            return;
        }

        VideoGroup group = item.group;

        Log.d(TAG, "onScrollEnd. Group title: " + group.getTitle());

        continueGroup(group);
    }

    @Override
    public void onSectionFocused(int sectionId) {
        updateSection(sectionId);
    }

    @Override
    public void onSectionLongPressed(int sectionId) {
        SectionMenuPresenter.instance(getContext()).showMenu(getSection(sectionId));
    }

    @Override
    public boolean hasPendingActions() {
        return RxUtils.isAnyActionRunning(mUpdateAction, mContinueAction, mSignCheckAction);
    }

    public boolean isItemPinned(Video item) {
        Set<Video> items = mGeneralData.getPinnedItems();

        return items.contains(item);
    }

    public void pinItem(Video item) {
        Set<Video> items = mGeneralData.getPinnedItems();
        items.add(item);
        mGeneralData.setPinnedItems(items);

        BrowseSection category = new BrowseSection(item.hashCode(), item.title, BrowseSection.TYPE_GRID, item.cardImageUrl, true, item);
        mSections.add(category);
        mGridMapping.put(item.hashCode(), ChannelUploadsPresenter.instance(getContext()).obtainPlaylistObservable(item));

        if (getView() != null) {
            getView().addSection(-1, category); // add last
        }
    }

    public void unpinItem(Video item) {
        Set<Video> items = mGeneralData.getPinnedItems();
        items.remove(item);
        mGeneralData.setPinnedItems(items);

        BrowseSection section = null;

        for (BrowseSection cat : mSections) {
            if (cat.getId() == item.hashCode()) {
                section = cat;
                break;
            }
        }

        mGridMapping.remove(item.hashCode());

        if (getView() != null) {
            getView().removeSection(section);
        }
    }

    private void maybeRefreshHeader() {
        long timeAfterPauseMs = System.currentTimeMillis() - mLastUpdateTimeMs;
        if (timeAfterPauseMs > HEADER_REFRESH_PERIOD_MS) { // update header every n minutes
            refresh();
        }
    }

    private void showBootDialogs() {
//        BootDialogPresenter updatePresenter = BootDialogPresenter.instance(getContext());
//        updatePresenter.start();
//        updatePresenter.unhold();
    }

    public void refresh() {
        if (mCurrentSection != null) {
            updateSection(mCurrentSection.getId());
        }
    }

    private void updateRefreshTime() {
        mLastUpdateTimeMs = System.currentTimeMillis();
    }

    private void updateSection(int sectionId) {
        disposeActions();

        mCurrentSection = getSection(sectionId);

        if (getView() == null || sectionId < 0) {
            return;
        }

        if (mCurrentSection != null) {
            Log.d(TAG, "Update section %s", mCurrentSection.getTitle());
            updateSection(mCurrentSection);
        }
    }

    private void updateSection(BrowseSection section) {
        switch (section.getType()) {
            case BrowseSection.TYPE_GRID:
                Observable<MediaGroup> group = mGridMapping.get(section.getId());
                updateVideoGrid(section, group, section.isAuthOnly());
                break;
            case BrowseSection.TYPE_ROW:
                Observable<List<MediaGroup>> groups = mRowMapping.get(section.getId());
                updateVideoRows(section, groups, section.isAuthOnly());
                break;
            case BrowseSection.TYPE_SETTINGS_GRID:
                Callable<List<SettingsItem>> items = mSettingsGridMapping.get(section.getId());
                updateSettingsGrid(section, items);
                break;
            case BrowseSection.TYPE_MULTI_GRID:
                Observable<MediaGroup> group2 = mGridMapping.get(section.getId());
                updateVideoGrid(section, group2, 0, section.isAuthOnly());
                break;
        }

        updateRefreshTime();
    }

    private void updateSettingsGrid(BrowseSection section, Callable<List<SettingsItem>> items) {
        getView().updateSection(SettingsGroup.from(Helpers.get(items), section));
        getView().showProgressBar(false);
    }

    private void updateVideoRows(BrowseSection section, Observable<List<MediaGroup>> groups, boolean authCheck) {
        Log.d(TAG, "loadRowsHeader: Start loading section: " + section.getTitle());

        authCheck(authCheck, () -> updateVideoRows(section, groups));
    }

    private void updateVideoGrid(BrowseSection section, Observable<MediaGroup> group, boolean authCheck) {
        updateVideoGrid(section, group, -1, authCheck);
    }

    private void updateVideoGrid(BrowseSection category, Observable<MediaGroup> group, int position, boolean authCheck) {
        Log.d(TAG, "loadMultiGridHeader: Start loading category: " + category.getTitle());

        authCheck(authCheck, () -> updateVideoGrid(category, group, position));
    }

    private void updateVideoRows(BrowseSection section, Observable<List<MediaGroup>> groups) {
        Log.d(TAG, "updateRowsHeader: Start loading section: " + section.getTitle());

        disposeActions();
        getView().showProgressBar(true);

        getView().updateSection(VideoGroup.from(section, true));

        mUpdateAction = groups
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        mediaGroups -> {
                            filterIfNeeded(mediaGroups);

                            for (MediaGroup mediaGroup : mediaGroups) {
                                if (mediaGroup.isEmpty()) {
                                    Log.e(TAG, "loadRowsHeader: MediaGroup is empty. Group Name: " + mediaGroup.getTitle());
                                    continue;
                                }
                                
                                filterIfNeeded(mediaGroup);

                                VideoGroup videoGroup = VideoGroup.from(mediaGroup, section);

                                getView().updateSection(videoGroup);

                                loadNextPortionIfNeeded(videoGroup);
                            }

                            // Hide loading as long as first group received
                            if (!mediaGroups.isEmpty()) {
                                getView().showProgressBar(false);
                            }
                        },
                        error -> {
                            Log.e(TAG, "updateRowsHeader error: %s", error.getMessage());
                            getView().showProgressBar(false);
                            getView().showError(new CategoryEmptyError(getContext()));
                        });
    }

    private void updateVideoGrid(BrowseSection section, Observable<MediaGroup> group, int position) {
        Log.d(TAG, "updateGridHeader: Start loading section: " + section.getTitle());

        disposeActions();
        getView().showProgressBar(true);

        getView().updateSection(VideoGroup.from(section, position, true));

        if (group == null) {
            // No group. Maybe just clear.
            getView().showProgressBar(false);
            return;
        }

        mUpdateAction = group
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        mediaGroup -> {
                            if (getView() == null) {
                                Log.e(TAG, "Browse view has been unloaded from the memory. Low RAM?");
                                ViewManager.instance(getContext()).startView(BrowseView.class);
                                return;
                            }

                            filterIfNeeded(mediaGroup);

                            VideoGroup videoGroup = VideoGroup.from(mediaGroup, section, position);
                            getView().updateSection(videoGroup);

                            // Hide loading as long as first group received
                            if (mediaGroup.getMediaItems() != null) {
                                getView().showProgressBar(false);
                            }

                            loadNextPortionIfNeeded(videoGroup);
                        },
                        error -> {
                            Log.e(TAG, "updateGridHeader error: %s", error.getMessage());
                            if (getView() != null) {
                                getView().showProgressBar(false);
                                getView().showError(new CategoryEmptyError(getContext()));
                            }
                        });
    }

    private void continueGroup(VideoGroup group) {
        if (RxUtils.isAnyActionRunning(mContinueAction)) {
            Log.e(TAG, "Can't continue group. Another action is running.");
            return;
        }

        if (getView() == null) {
            Log.e(TAG, "Can't continue group. The view is null.");
            return;
        }

        Log.d(TAG, "continueGroup: start continue group: " + group.getTitle());
        
        getView().showProgressBar(true);

        MediaGroup mediaGroup = group.getMediaGroup();

        Observable<MediaGroup> continuation;

        if (mediaGroup.getType() == MediaGroup.TYPE_SUGGESTIONS) { // Pinned playlist
            continuation = mItemManager.continueGroupObserve(mediaGroup);
        } else {
            continuation = mGroupManager.continueGroupObserve(mediaGroup);
        }

        mContinueAction = continuation
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        continueGroup -> {
                            filterIfNeeded(continueGroup);

                            VideoGroup videoGroup = VideoGroup.from(continueGroup, group.getCategory(), group.getPosition());
                            getView().updateSection(videoGroup);
                        },
                        error -> {
                            Log.e(TAG, "continueGroup error: %s", error.getMessage());
                            if (getView() != null) {
                                getView().showProgressBar(false);
                            }
                        },
                        () -> getView().showProgressBar(false)
                );
    }

    private void authCheck(boolean check, Runnable callback) {
        disposeActions();

        if (!check) {
            callback.run();
            return;
        }

        getView().showProgressBar(true);

        mSignCheckAction = mSignInManager.isSignedObserve()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        isSigned -> {
                            if (isSigned) {
                                callback.run();
                            } else {
                                if (getView().isProgressBarShowing()) {
                                    getView().showProgressBar(false);
                                    getView().showError(new SignInError(getContext()));
                                }
                            }
                        },
                        error -> Log.e(TAG, "authCheck error: %s", error.getMessage())
                );

    }

    /**
     * Most tiny ui has 8 cards in a row or 24 in grid.
     */
    private void loadNextPortionIfNeeded(VideoGroup videoGroup) {
        boolean groupTooSmall = videoGroup.getVideos() != null && videoGroup.getVideos().size() < MIN_GROUP_SIZE;
        if (groupTooSmall || mMainUIData.getUIScale() < 0.8f || mMainUIData.getVideoGridScale() < 0.8f) {
            continueGroup(videoGroup);
        }
    }

    private void disposeActions() {
        RxUtils.disposeActions(mUpdateAction, mContinueAction, mSignCheckAction);
    }

    private void updateMultiGrid(Video item) {
        if (mCurrentSection == null) {
            return;
        }

        updateVideoGrid(mCurrentSection, ChannelUploadsPresenter.instance(getContext()).obtainPlaylistObservable(item), 1, true);
    }

    private BrowseSection getSection(int sectionId) {
        for (BrowseSection category : mSections) {
            if (category.getId() == sectionId) {
                return category;
            }
        }

        return null;
    }

    private void filterIfNeeded(MediaGroup mediaGroup) {
        if (mediaGroup == null || mediaGroup.getMediaItems() == null) {
            return;
        }

        if (mGeneralData.isHideShortsEnabled() && mediaGroup.getType() == MediaGroup.TYPE_SUBSCRIPTIONS) {

            // Remove Shorts
            // Predicate replacement function for devices with Android 6.0 and below.
            Helpers.removeIf(mediaGroup.getMediaItems(), value -> {
                if (value.getTitle() == null) {
                    return false;
                }

                int lengthMs = ServiceHelper.timeTextToMillis(value.getBadgeText());
                return lengthMs > 0 && lengthMs < SHORTS_LEN_MS;
                //return value.title.toLowerCase().contains("#short")  ||
                //       value.title.toLowerCase().contains("#shorts") ||
                //       value.title.toLowerCase().contains("#tiktok");
            });
        }
    }

    private void filterIfNeeded(List<MediaGroup> mediaGroups) {
        if (mediaGroups == null) {
            return;
        }
        
        Helpers.removeIf(mediaGroups, value -> Helpers.equalsAny(
                value.getTitle(),
                getContext().getString(R.string.breaking_news_row_name),
                getContext().getString(R.string.covid_news_row_name)
        ));
    }
}
