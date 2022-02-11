package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu;

import android.content.Context;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;

public abstract class BaseMenuPresenter extends BasePresenter<Void> {
    private final MediaServiceManager mServiceManager;

    protected BaseMenuPresenter(Context context) {
        super(context);
        mServiceManager = MediaServiceManager.instance();
    }

    protected abstract Video getVideo();
    protected abstract AppDialogPresenter getDialogPresenter();
    protected abstract boolean isPinToSidebarEnabled();
    protected abstract boolean isAccountSelectionEnabled();

    public void closeDialog() {
        if (getDialogPresenter() != null) {
            getDialogPresenter().closeDialog();
        }
        MessageHelpers.cancelToasts();
    }

    protected void appendTogglePinVideoToSidebarButton() {
        appendTogglePinPlaylistButton();
        appendTogglePinChannelButton();
    }

    private void appendTogglePinPlaylistButton() {
        if (!isPinToSidebarEnabled()) {
            return;
        }

        Video original = getVideo();

        if (original == null || !original.hasPlaylist()) {
            return;
        }

        getDialogPresenter().appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.pin_unpin_playlist),
                        optionItem -> togglePinToSidebar(createPinnedPlaylist(original))));
    }

    private void appendTogglePinChannelButton() {
        if (!isPinToSidebarEnabled()) {
            return;
        }

        Video original = getVideo();

        if (original == null || (!original.hasVideo() && !original.hasReloadPageKey() && !original.hasChannel())) {
            return;
        }

        getDialogPresenter().appendSingleButton(
                UiOptionItem.from(
                        getContext().getString(original.isChannelPlaylist() || original.belongsToPlaylist() ? R.string.pin_unpin_playlist : R.string.pin_unpin_channel),
                        optionItem -> {
                            if (original.hasVideo()) {
                                MessageHelpers.showLongMessage(getContext(), R.string.wait_data_loading);

                                mServiceManager.loadMetadata(
                                        original,
                                        metadata -> {
                                            Video video = Video.from(original);
                                            video.sync(metadata);
                                            togglePinToSidebar(createPinnedChannel(video));
                                        }
                                );
                            } else {
                                togglePinToSidebar(createPinnedChannel(original));
                            }
                        }));
    }
    
    protected void togglePinToSidebar(Video section) {
        BrowsePresenter presenter = BrowsePresenter.instance(getContext());

        // Toggle between pin/unpin while dialog is opened
        boolean isItemPinned = presenter.isItemPinned(section);

        if (isItemPinned) {
            presenter.unpinItem(section);
        } else {
            presenter.pinItem(section);
        }
        MessageHelpers.showMessage(getContext(), section.title + ": " + getContext().getString(isItemPinned ? R.string.unpinned_from_sidebar : R.string.pinned_to_sidebar));
    }

    private Video createPinnedPlaylist(Video video) {
        if (video == null || !video.hasPlaylist()) {
            return null;
        }

        Video section = new Video();
        section.itemType = video.itemType;
        section.playlistId = video.playlistId;
        section.playlistParams = video.playlistParams;
        // Trying to properly format channel playlists, mixes etc
        boolean isChannelPlaylistItem = video.getGroupTitle() != null && video.belongsToSameAuthorGroup() && video.belongsToSamePlaylistGroup();
        boolean isUserPlaylistItem = video.getGroupTitle() != null && video.belongsToSamePlaylistGroup();
        String title = isChannelPlaylistItem ? video.extractAuthor() : isUserPlaylistItem ? null : video.title;
        String subtitle = isChannelPlaylistItem || isUserPlaylistItem ? video.getGroupTitle() : video.extractAuthor();
        section.title = title != null && subtitle != null ? String.format("%s - %s", title, subtitle) : String.format("%s", title != null ? title : subtitle);
        section.cardImageUrl = video.cardImageUrl;

        return section;
    }

    private Video createPinnedChannel(Video video) {
        if (video == null || (!video.hasReloadPageKey() && !video.hasChannel() && !video.isChannel())) {
            return null;
        }

        Video section = new Video();
        section.itemType = video.itemType;
        section.channelId = video.channelId;
        section.reloadPageKey = video.getReloadPageKey();
        // Trying to properly format channel playlists, mixes etc
        boolean hasChannel = video.hasChannel() && !video.isChannel();
        boolean isUserPlaylistItem = video.getGroupTitle() != null && video.belongsToSamePlaylistGroup();
        String title = hasChannel ? video.extractAuthor() : isUserPlaylistItem ? null : video.title;
        String subtitle = isUserPlaylistItem ? video.getGroupTitle() : hasChannel || video.isChannel() ? null : video.extractAuthor();
        section.title = title != null && subtitle != null ? String.format("%s - %s", title, subtitle) : String.format("%s", title != null ? title : subtitle);
        section.cardImageUrl = video.cardImageUrl;

        return section;
    }

    protected void appendAccountSelectionButton() {
        if (!isAccountSelectionEnabled()) {
            return;
        }

        getDialogPresenter().appendSingleButton(
                UiOptionItem.from(
                        getContext().getString(R.string.dialog_account_list),
                        optionItem -> AccountSelectionPresenter.instance(getContext()).show(true)
                ));
    }
}
