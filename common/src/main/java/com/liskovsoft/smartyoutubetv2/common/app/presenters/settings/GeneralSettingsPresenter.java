package com.liskovsoft.smartyoutubetv2.common.app.presenters.settings;

import android.content.Context;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.HQDialogManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.BackupAndRestoreManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.misc.ProxyManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class GeneralSettingsPresenter extends BasePresenter<Void> {
    private final GeneralData mGeneralData;
    private final PlayerData mPlayerData;
    private boolean mRestartApp;

    public GeneralSettingsPresenter(Context context) {
        super(context);
        mGeneralData = GeneralData.instance(context);
        mPlayerData = PlayerData.instance(context);
    }

    public static GeneralSettingsPresenter instance(Context context) {
        return new GeneralSettingsPresenter(context);
    }

    public void show() {
        AppDialogPresenter settingsPresenter = AppDialogPresenter.instance(getContext());
        settingsPresenter.clear();

        appendBootToSection(settingsPresenter);
        appendEnabledSections(settingsPresenter);
        appendAppExitCategory(settingsPresenter);
        appendBackgroundPlaybackCategory(settingsPresenter);
        appendBackgroundPlaybackActivationCategory(settingsPresenter);
        appendScreenDimmingCategory(settingsPresenter);
        appendKeyRemappingCategory(settingsPresenter);
        appendAppBackupCategory(settingsPresenter);
        appendMiscCategory(settingsPresenter);

        settingsPresenter.showDialog(getContext().getString(R.string.settings_general), () -> {
            if (mRestartApp) {
                mRestartApp = false;
                MessageHelpers.showLongMessage(getContext(), R.string.msg_restart_app);
            }
        });
    }

    private void appendEnabledSections(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        Map<Integer, Integer> sections = mGeneralData.getDefaultSections();

        for (Entry<Integer, Integer> section : sections.entrySet()) {
             options.add(UiOptionItem.from(getContext().getString(section.getKey()), optionItem -> {
                 mGeneralData.enableSection(section.getValue(), optionItem.isSelected());
                 BrowsePresenter.instance(getContext()).updateSections();
             }, mGeneralData.isSectionEnabled(section.getValue())));
        }

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.side_panel_sections), options);
    }

    private void appendBootToSection(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        Map<Integer, Integer> sections = mGeneralData.getDefaultSections();

        for (Entry<Integer, Integer> section : sections.entrySet()) {
            options.add(
                    UiOptionItem.from(
                            getContext().getString(section.getKey()),
                            optionItem -> mGeneralData.setBootSectionId(section.getValue()),
                            section.getValue().equals(mGeneralData.getBootSectionId())
                    )
            );
        }

        Set<Video> pinnedItems = mGeneralData.getPinnedItems();

        for (Video item : pinnedItems) {
            if (item != null && item.title != null) {
                options.add(
                        UiOptionItem.from(
                                item.title,
                                optionItem -> mGeneralData.setBootSectionId(item.hashCode()),
                                item.hashCode() == mGeneralData.getBootSectionId()
                        )
                );
            }
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.boot_to_section), options);
    }

    private void appendAppExitCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int[] pair : new int[][] {
                {R.string.app_exit_none, GeneralData.EXIT_NONE},
                {R.string.app_double_back_exit, GeneralData.EXIT_DOUBLE_BACK},
                {R.string.app_single_back_exit, GeneralData.EXIT_SINGLE_BACK}}) {
            options.add(UiOptionItem.from(getContext().getString(pair[0]),
                    optionItem -> mGeneralData.setAppExitShortcut(pair[1]),
                    mGeneralData.getAppExitShortcut() == pair[1]));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.app_exit_shortcut), options);
    }

    private void appendBackgroundPlaybackCategory(AppDialogPresenter settingsPresenter) {
        OptionCategory category = HQDialogManager.createBackgroundPlaybackCategory(getContext(), mPlayerData);
        settingsPresenter.appendRadioCategory(category.title, category.options);
    }

    private void appendBackgroundPlaybackActivationCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from("HOME",
                option -> mGeneralData.setBackgroundShortcut(GeneralData.BACKGROUND_SHORTCUT_HOME),
                mGeneralData.getBackgroundShortcut() == GeneralData.BACKGROUND_SHORTCUT_HOME));

        options.add(UiOptionItem.from("HOME/BACK",
                option -> mGeneralData.setBackgroundShortcut(GeneralData.BACKGROUND_SHORTCUT_HOME_N_BACK),
                mGeneralData.getBackgroundShortcut() == GeneralData.BACKGROUND_SHORTCUT_HOME_N_BACK));

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.background_playback_activation), options);
    }

    private void appendKeyRemappingCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from("Fast Forward/Rewind -> Next/Previous",
                option -> {
                    mGeneralData.remapFastForwardToNext(option.isSelected());
                    mRestartApp = true;
                },
                mGeneralData.isRemapFastForwardToNextEnabled()));

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.key_remapping), options);
    }

    private void appendScreenDimmingCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(
                getContext().getString(R.string.option_never),
                option -> mGeneralData.setScreenDimmingTimeoutMin(GeneralData.SCREEN_DIMMING_NEVER),
                mGeneralData.getScreenDimmingTimeoutMin() == GeneralData.SCREEN_DIMMING_NEVER));

        for (int i = 1; i <= 15; i++) {
            int timeoutMin = i;
            options.add(UiOptionItem.from(
                    getContext().getString(R.string.screen_dimming_timeout_min, i),
                    option -> mGeneralData.setScreenDimmingTimeoutMin(timeoutMin),
                    mGeneralData.getScreenDimmingTimeoutMin() == i));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.screen_dimming), options);
    }

    private void appendAppBackupCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        BackupAndRestoreManager backupManager = new BackupAndRestoreManager(getContext());

        if (getContext() instanceof MotherActivity) {
            ((MotherActivity) getContext()).addOnPermissions(backupManager);
        }

        options.add(UiOptionItem.from(
                String.format("%s\n%s", getContext().getString(R.string.app_restore), backupManager.getBackupPath()),
                option -> {
                    backupManager.checkPermAndRestore();
                    MessageHelpers.showMessage(getContext(), R.string.msg_done);
                }));

        options.add(UiOptionItem.from(
                String.format("%s\n%s", getContext().getString(R.string.app_backup), backupManager.getBackupPath()),
                option -> {
                    mGeneralData.enableSettingsSection(true); // prevent Settings lock
                    backupManager.checkPermAndBackup();
                    MessageHelpers.showMessage(getContext(), R.string.msg_done);
                }));

        settingsPresenter.appendStringsCategory(getContext().getString(R.string.app_backup_restore), options);
    }

    private void appendMiscCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        // Disable long press on buggy controllers.
        options.add(UiOptionItem.from(getContext().getString(R.string.disable_ok_long_press),
                option -> mGeneralData.disableOkButtonLongPress(option.isSelected()),
                mGeneralData.isOkButtonLongPressDisabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.hide_shorts),
                option -> mGeneralData.hideShorts(option.isSelected()),
                mGeneralData.isHideShortsEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.return_to_launcher),
                option -> mGeneralData.enableReturnToLauncher(option.isSelected()),
                mGeneralData.isReturnToLauncherEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.hide_settings_section),
                option -> {
                    mGeneralData.enableSettingsSection(!option.isSelected());
                    mRestartApp = true;
                },
                !mGeneralData.isSettingsSectionEnabled()));

        ProxyManager proxyManager = ProxyManager.instance(getContext());

        options.add(UiOptionItem.from("Web Proxy config: " + proxyManager.getConfigPath(),
                option -> {
                    mGeneralData.enableProxy(option.isSelected());
                    proxyManager.enableProxy(option.isSelected());
                    mRestartApp = true;
                },
                mGeneralData.isProxyEnabled()));

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.player_other), options);
    }
}
