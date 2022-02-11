package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu;

import android.content.Context;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;

import java.util.Iterator;

public class SectionMenuPresenter extends BaseMenuPresenter {
    private static final String TAG = SectionMenuPresenter.class.getSimpleName();
    private final AppDialogPresenter mDialogPresenter;
    private final MediaServiceManager mServiceManager;
    private Video mVideo;
    private BrowseSection mSection;
    private boolean mIsUnpinFromSidebarEnabled;
    private boolean mIsUnpinSectionFromSidebarEnabled;
    private boolean mIsReturnToBackgroundVideoEnabled;
    private boolean mIsAccountSelectionEnabled;
    private boolean mIsMarkAllChannelsWatchedEnabled;
    private boolean mIsRefreshEnabled;
    private boolean mIsMoveSectionEnabled;

    private SectionMenuPresenter(Context context) {
        super(context);
        MediaService service = YouTubeMediaService.instance();
        mServiceManager = MediaServiceManager.instance();
        mDialogPresenter = AppDialogPresenter.instance(context);
    }

    public static SectionMenuPresenter instance(Context context) {
        return new SectionMenuPresenter(context);
    }

    @Override
    protected Video getVideo() {
        return mVideo;
    }

    @Override
    protected AppDialogPresenter getDialogPresenter() {
        return mDialogPresenter;
    }

    @Override
    protected boolean isPinToSidebarEnabled() {
        return mIsUnpinFromSidebarEnabled;
    }

    @Override
    protected boolean isAccountSelectionEnabled() {
        return mIsAccountSelectionEnabled;
    }

    public void showMenu(BrowseSection section) {
        mIsReturnToBackgroundVideoEnabled = true;
        mIsUnpinFromSidebarEnabled = true;
        mIsUnpinSectionFromSidebarEnabled = true;
        mIsAccountSelectionEnabled = true;
        mIsRefreshEnabled = true;
        mIsMarkAllChannelsWatchedEnabled = true;
        mIsMoveSectionEnabled = true;

        showMenuInt(section);
    }

    private void showMenuInt(BrowseSection section) {
        if (section == null) {
            return;
        }

        updateEnabledMenuItems();

        disposeActions();

        mSection = section;
        mVideo = section.getData();

        MediaServiceManager.instance().authCheck(this::obtainPlaylistsAndShowDialogSigned, this::prepareAndShowDialogUnsigned);
    }

    private void obtainPlaylistsAndShowDialogSigned() {
        prepareAndShowDialogSigned();
    }

    private void prepareAndShowDialogSigned() {
        if (getContext() == null) {
            return;
        }

        mDialogPresenter.clear();

//        appendReturnToBackgroundVideoButton();
        appendRefreshButton();
        appendUnpinVideoFromSidebarButton();
        appendUnpinSectionFromSidebarButton();
        appendMarkAllChannelsWatchedButton();
        appendAccountSelectionButton();
        appendMoveSectionButton();

        if (!mDialogPresenter.isEmpty()) {
            String title = mSection != null ? mSection.getTitle() : null;
            mDialogPresenter.showDialog(title, this::disposeActions);
        }
    }

    private void prepareAndShowDialogUnsigned() {
        if (getContext() == null) {
            return;
        }

        mDialogPresenter.clear();

//        appendReturnToBackgroundVideoButton();
        appendRefreshButton();
//        appendUnpinFromSidebarButton();
//        appendUnpinSectionFromSidebarButton();
        appendAccountSelectionButton();
        appendMoveSectionButton();

        if (mDialogPresenter.isEmpty()) {
            MessageHelpers.showMessage(getContext(), R.string.msg_signed_users_only);
        } else {
            String title = mSection != null ? mSection.getTitle() : null;
            mDialogPresenter.showDialog(title, this::disposeActions);
        }
    }

    private void appendUnpinVideoFromSidebarButton() {
        if (!mIsUnpinFromSidebarEnabled) {
            return;
        }

        if (mVideo == null || (!mVideo.hasPlaylist() && !mVideo.hasReloadPageKey() && !mVideo.hasChannel())) {
            return;
        }

        getDialogPresenter().appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.unpin_from_sidebar),
                        optionItem -> {
                            togglePinToSidebar(mVideo);
                            mDialogPresenter.closeDialog();
                        }));
    }

    private void appendUnpinSectionFromSidebarButton() {
        if (!mIsUnpinSectionFromSidebarEnabled) {
            return;
        }

        if (mSection == null || mSection.getId() == MediaGroup.TYPE_SETTINGS || mVideo != null) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.unpin_from_sidebar),
                        optionItem -> {
                            BrowsePresenter.instance(getContext()).enableSection(mSection.getId(), false);
                            mDialogPresenter.closeDialog();
                        }));
    }

    private void appendRefreshButton() {
        if (!mIsRefreshEnabled) {
            return;
        }

        if (mSection == null || mSection.getId() == MediaGroup.TYPE_SETTINGS) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.refresh_section), optionItem -> {
                    if (BrowsePresenter.instance(getContext()).getView() != null) {
                        BrowsePresenter.instance(getContext()).getView().focusOnContent();
                        BrowsePresenter.instance(getContext()).refresh();
                    }
                    mDialogPresenter.closeDialog();
                }));
    }

    private void appendMoveSectionButton() {
        if (!mIsMoveSectionEnabled) {
            return;
        }

        if (mSection == null) {
            return;
        }

        GeneralData generalData = GeneralData.instance(getContext());

        if (generalData.canMoveSectionUp(mSection.getId())) {
            mDialogPresenter.appendSingleButton(
                    UiOptionItem.from(getContext().getString(R.string.move_section_up), optionItem -> {
                        mDialogPresenter.closeDialog();
                        BrowsePresenter.instance(getContext()).moveSectionUp(mSection.getId());
                    }));
        }

        if (generalData.canMoveSectionDown(mSection.getId())) {
            mDialogPresenter.appendSingleButton(
                    UiOptionItem.from(getContext().getString(R.string.move_section_down), optionItem -> {
                        mDialogPresenter.closeDialog();
                        BrowsePresenter.instance(getContext()).moveSectionDown(mSection.getId());
                    }));
        }
    }

    private void appendReturnToBackgroundVideoButton() {
        if (!mIsReturnToBackgroundVideoEnabled || !PlaybackPresenter.instance(getContext()).isRunningInBackground()) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.return_to_background_video),
                        // Assume that the Playback view already blocked and remembered.
                        optionItem -> ViewManager.instance(getContext()).startView(SplashView.class)
                )
        );
    }

    private void appendMarkAllChannelsWatchedButton() {
        if (!mIsMarkAllChannelsWatchedEnabled) {
            return;
        }

        if (mSection == null || mSection.getId() != MediaGroup.TYPE_CHANNEL_UPLOADS) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.mark_all_channels_watched), optionItem -> {
                    mDialogPresenter.closeDialog();

                    MediaServiceManager serviceManager = MediaServiceManager.instance();

                    MessageHelpers.showMessage(getContext(), R.string.wait_data_loading);

                    serviceManager.loadSubscribedChannels(group -> {
                        Iterator<MediaItem> iterator = group.getMediaItems().iterator();

                        processNextChannel(serviceManager, iterator);
                    });
                }));
    }

    private void processNextChannel(MediaServiceManager serviceManager, Iterator<MediaItem> iterator) {
        if (iterator.hasNext()) {
            MediaItem next = iterator.next();

            if (!next.hasNewContent()) {
                processNextChannel(serviceManager, iterator);
                return;
            }

            MessageHelpers.showMessage(getContext(), next.getTitle());
            serviceManager.loadChannelUploads(next, (groupTmp) -> processNextChannel(serviceManager, iterator));
        } else {
            MessageHelpers.showMessage(getContext(), R.string.msg_done);
        }
    }

    private void disposeActions() {
        //RxUtils.disposeActions(mPlaylistAction);
    }

    private void updateEnabledMenuItems() {
        MainUIData mainUIData = MainUIData.instance(getContext());

        if (!mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_PIN_TO_SIDEBAR)) {
            mIsUnpinFromSidebarEnabled = false;
            mIsUnpinSectionFromSidebarEnabled = false;
        }

        if (!mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_SELECT_ACCOUNT)) {
            mIsAccountSelectionEnabled = false;
        }

        if (!mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_MOVE_SECTION_UP)) {
            mIsMoveSectionEnabled = false;
        }

        if (!mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_MOVE_SECTION_DOWN)) {
            mIsMoveSectionEnabled = false;
        }
    }
}
