package com.liskovsoft.smartyoutubetv2.common.app.presenters.settings;

import android.content.Context;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;

import java.util.ArrayList;
import java.util.List;

public class PlayerSettingsPresenter extends BasePresenter<Void> {
    private final PlayerData mPlayerData;
    private final PlayerTweaksData mPlayerTweaksData;

    private PlayerSettingsPresenter(Context context) {
        super(context);
        mPlayerData = PlayerData.instance(context);
        mPlayerTweaksData = PlayerTweaksData.instance(context);
    }

    public static PlayerSettingsPresenter instance(Context context) {
        return new PlayerSettingsPresenter(context);
    }

    public void show() {
        AppDialogPresenter settingsPresenter = AppDialogPresenter.instance(getContext());
        settingsPresenter.clear();

        appendVideoPresetsCategory(settingsPresenter);
        appendVideoBufferCategory(settingsPresenter);
        //appendVideoZoomCategory(settingsPresenter);
        appendAudioShiftCategory(settingsPresenter);
        appendMasterVolumeCategory(settingsPresenter);
        appendOKButtonCategory(settingsPresenter);
        appendPlayerButtonsCategory(settingsPresenter);
        appendUIAutoHideCategory(settingsPresenter);
        appendSeekTypeCategory(settingsPresenter);
        appendSeekingPreviewCategory(settingsPresenter);
        AppDialogUtil.appendSeekIntervalDialogItems(getContext(), settingsPresenter, mPlayerData, false);
        appendRememberSpeedCategory(settingsPresenter);
        appendEndingTimeCategory(settingsPresenter);
        appendMiscCategory(settingsPresenter);
        appendTweaksCategory(settingsPresenter);

        settingsPresenter.showDialog(getContext().getString(R.string.dialog_player_ui));
    }

    private void appendOKButtonCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(
                getContext().getString(R.string.player_only_ui),
                option -> mPlayerData.setOKButtonBehavior(PlayerData.ONLY_UI),
                mPlayerData.getOKButtonBehavior() == PlayerData.ONLY_UI));

        options.add(UiOptionItem.from(
                getContext().getString(R.string.player_ui_and_pause),
                option -> mPlayerData.setOKButtonBehavior(PlayerData.UI_AND_PAUSE),
                mPlayerData.getOKButtonBehavior() == PlayerData.UI_AND_PAUSE));

        options.add(UiOptionItem.from(
                getContext().getString(R.string.player_only_pause),
                option -> mPlayerData.setOKButtonBehavior(PlayerData.ONLY_PAUSE),
                mPlayerData.getOKButtonBehavior() == PlayerData.ONLY_PAUSE));

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.player_ok_button_behavior), options);
    }

    private void appendUIAutoHideCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(
                getContext().getString(R.string.option_never),
                option -> mPlayerData.setUIHideTimoutSec(PlayerData.AUTO_HIDE_NEVER),
                mPlayerData.getUIHideTimoutSec() == PlayerData.AUTO_HIDE_NEVER));

        for (int i = 1; i <= 15; i++) {
            int timeoutSec = i;
            options.add(UiOptionItem.from(
                    getContext().getString(R.string.ui_hide_timeout_sec, i),
                    option -> mPlayerData.setUIHideTimoutSec(timeoutSec),
                    mPlayerData.getUIHideTimoutSec() == i));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.player_ui_hide_behavior), options);
    }

    private void appendVideoBufferCategory(AppDialogPresenter settingsPresenter) {
        OptionCategory category = AppDialogUtil.createVideoBufferCategory(getContext(), mPlayerData);
        settingsPresenter.appendRadioCategory(category.title, category.options);
    }

    private void appendVideoPresetsCategory(AppDialogPresenter settingsPresenter) {
        OptionCategory category = AppDialogUtil.createVideoPresetsCategory(getContext(), mPlayerData);
        settingsPresenter.appendRadioCategory(category.title, category.options);
    }

    private void appendVideoZoomCategory(AppDialogPresenter settingsPresenter) {
        OptionCategory category = AppDialogUtil.createVideoZoomCategory(getContext(), mPlayerData);
        settingsPresenter.appendRadioCategory(category.title, category.options);
    }

    private void appendAudioShiftCategory(AppDialogPresenter settingsPresenter) {
        OptionCategory category = AppDialogUtil.createAudioShiftCategory(getContext(), mPlayerData);
        settingsPresenter.appendRadioCategory(category.title, category.options);
    }

    private void appendMasterVolumeCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int scalePercent : Helpers.range(0, 100, 5)) {
            float scale = scalePercent / 100f;
            options.add(UiOptionItem.from(String.format("%sx", scale),
                    optionItem -> mPlayerData.setPlayerVolume(scale),
                    Helpers.floatEquals(scale, mPlayerData.getPlayerVolume())));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.volume_limit), options);
    }

    private void appendSeekingPreviewCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int[] pair : new int[][] {
                {R.string.player_seek_preview_none, PlayerData.SEEK_PREVIEW_NONE},
                {R.string.player_seek_preview_single, PlayerData.SEEK_PREVIEW_SINGLE},
                {R.string.player_seek_preview_carousel_slow, PlayerData.SEEK_PREVIEW_CAROUSEL_SLOW},
                {R.string.player_seek_preview_carousel_fast, PlayerData.SEEK_PREVIEW_CAROUSEL_FAST}}) {
            options.add(UiOptionItem.from(getContext().getString(pair[0]),
                    optionItem -> mPlayerData.setSeekPreviewMode(pair[1]),
                    mPlayerData.getSeekPreviewMode() == pair[1]));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.player_seek_preview), options);
    }

    private void appendRememberSpeedCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(getContext().getString(R.string.player_remember_speed_none),
                optionItem -> {
                    mPlayerData.enableRememberSpeed(false);
                    mPlayerData.enableRememberSpeedEach(false);
                },
                !mPlayerData.isRememberSpeedEnabled() && !mPlayerData.isRememberSpeedEachEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_remember_speed_all),
                optionItem -> mPlayerData.enableRememberSpeed(true),
                mPlayerData.isRememberSpeedEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_remember_speed_each),
                optionItem -> mPlayerData.enableRememberSpeedEach(true),
                mPlayerData.isRememberSpeedEachEnabled()));

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.player_remember_speed), options);
    }

    private void appendPlayerButtonsCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int[] pair : new int[][] {
                {R.string.open_chat, PlayerTweaksData.PLAYER_BUTTON_CHAT},
                {R.string.content_block_provider, PlayerTweaksData.PLAYER_BUTTON_CONTENT_BLOCK},
                {R.string.seek_interval, PlayerTweaksData.PLAYER_BUTTON_SEEK_INTERVAL},
                {R.string.share_link, PlayerTweaksData.PLAYER_BUTTON_SHARE},
                {R.string.action_video_info, PlayerTweaksData.PLAYER_BUTTON_VIDEO_INFO},
                {R.string.action_video_stats, PlayerTweaksData.PLAYER_BUTTON_VIDEO_STATS},
                {R.string.action_playback_queue, PlayerTweaksData.PLAYER_BUTTON_PLAYBACK_QUEUE},
                {R.string.action_screen_off, PlayerTweaksData.PLAYER_BUTTON_SCREEN_OFF},
                {R.string.action_video_zoom, PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM},
                {R.string.action_channel, PlayerTweaksData.PLAYER_BUTTON_OPEN_CHANNEL},
                {R.string.action_search, PlayerTweaksData.PLAYER_BUTTON_SEARCH},
                {R.string.run_in_background, PlayerTweaksData.PLAYER_BUTTON_PIP},
                {R.string.action_video_speed, PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED},
                {R.string.action_subtitles, PlayerTweaksData.PLAYER_BUTTON_SUBTITLES},
                {R.string.action_subscribe, PlayerTweaksData.PLAYER_BUTTON_SUBSCRIBE},
                {R.string.action_like, PlayerTweaksData.PLAYER_BUTTON_LIKE},
                {R.string.action_dislike, PlayerTweaksData.PLAYER_BUTTON_DISLIKE},
                {R.string.action_playlist_add, PlayerTweaksData.PLAYER_BUTTON_ADD_TO_PLAYLIST},
                {R.string.action_play_pause, PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE},
                {R.string.action_repeat_mode, PlayerTweaksData.PLAYER_BUTTON_REPEAT_MODE},
                {R.string.action_next, PlayerTweaksData.PLAYER_BUTTON_NEXT},
                {R.string.action_previous, PlayerTweaksData.PLAYER_BUTTON_PREVIOUS},
                {R.string.playback_settings, PlayerTweaksData.PLAYER_BUTTON_HIGH_QUALITY}
        }) {
            options.add(UiOptionItem.from(getContext().getString(pair[0]), optionItem -> {
                if (optionItem.isSelected()) {
                    mPlayerTweaksData.enablePlayerButton(pair[1]);
                } else {
                    mPlayerTweaksData.disablePlayerButton(pair[1]);
                }
            }, mPlayerTweaksData.isPlayerButtonEnabled(pair[1])));
        }

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.player_buttons), options);
    }

    private void appendTweaksCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(getContext().getString(R.string.audio_sync_fix),
                getContext().getString(R.string.audio_sync_fix_desc),
                option -> mPlayerTweaksData.enableAudioSyncFix(option.isSelected()),
                mPlayerTweaksData.isAudioSyncFixEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.ambilight_ratio_fix),
                getContext().getString(R.string.ambilight_ratio_fix_desc),
                option -> {
                    mPlayerTweaksData.enableTextureView(option.isSelected());
                    if (option.isSelected()) {
                        // Tunneled playback works only with SurfaceView
                        mPlayerTweaksData.enableTunneledPlayback(false);
                    }
                },
                mPlayerTweaksData.isTextureViewEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.force_legacy_codecs),
                getContext().getString(R.string.force_legacy_codecs_desc),
                option -> mPlayerData.forceLegacyCodecs(option.isSelected()),
                mPlayerData.isLegacyCodecsForced()));

        options.add(UiOptionItem.from(getContext().getString(R.string.live_stream_fix),
                getContext().getString(R.string.live_stream_fix_desc),
                option -> mPlayerTweaksData.enableLiveStreamFix(option.isSelected()),
                mPlayerTweaksData.isLiveStreamFixEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.playback_notifications_fix),
                getContext().getString(R.string.playback_notifications_fix_desc),
                option -> mPlayerTweaksData.disablePlaybackNotifications(option.isSelected()),
                mPlayerTweaksData.isPlaybackNotificationsDisabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.amlogic_fix),
                getContext().getString(R.string.amlogic_fix_desc),
                option -> mPlayerTweaksData.enableAmlogicFix(option.isSelected()),
                mPlayerTweaksData.isAmlogicFixEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.tunneled_video_playback),
                getContext().getString(R.string.tunneled_video_playback_desc),
                option -> {
                    mPlayerTweaksData.enableTunneledPlayback(option.isSelected());
                    if (option.isSelected()) {
                        // Tunneled playback works only with SurfaceView
                        mPlayerTweaksData.enableTextureView(false);
                    }
                },
                mPlayerTweaksData.isTunneledPlaybackEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.alt_presets_behavior),
                getContext().getString(R.string.alt_presets_behavior_desc),
                option -> mPlayerTweaksData.enableNoFpsPresets(option.isSelected()),
                mPlayerTweaksData.isNoFpsPresetsEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.prefer_avc_over_vp9),
                option -> mPlayerTweaksData.preferAvcOverVp9(option.isSelected()),
                mPlayerTweaksData.isAvcOverVp9Preferred()));

        options.add(UiOptionItem.from(getContext().getString(R.string.sleep_timer),
                getContext().getString(R.string.sleep_timer_desc),
                option -> mPlayerData.enableSonyTimerFix(option.isSelected()),
                mPlayerData.isSonyTimerFixEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.disable_vsync),
                getContext().getString(R.string.disable_vsync_desc),
                option -> mPlayerTweaksData.disableSnapToVsync(option.isSelected()),
                mPlayerTweaksData.isSnappingToVsyncDisabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.skip_codec_profile_check),
                getContext().getString(R.string.skip_codec_profile_check_desc),
                option -> mPlayerTweaksData.skipProfileLevelCheck(option.isSelected()),
                mPlayerTweaksData.isProfileLevelCheckSkipped()));

        options.add(UiOptionItem.from(getContext().getString(R.string.force_sw_codec),
                getContext().getString(R.string.force_sw_codec_desc),
                option -> mPlayerTweaksData.forceSWDecoder(option.isSelected()),
                mPlayerTweaksData.isSWDecoderForced()));

        options.add(UiOptionItem.from("Frame drop fix (experimental)",
                option -> mPlayerTweaksData.enableFrameDropFix(option.isSelected()),
                mPlayerTweaksData.isFrameDropFixEnabled()));

        options.add(UiOptionItem.from("Buffering fix (experimental)",
                option -> mPlayerTweaksData.enableBufferingFix(option.isSelected()),
                mPlayerTweaksData.isBufferingFixEnabled()));

        options.add(UiOptionItem.from("Keep finished activities",
                option -> mPlayerTweaksData.enableKeepFinishedActivity(option.isSelected()),
                mPlayerTweaksData.isKeepFinishedActivityEnabled()));

        options.add(UiOptionItem.from("Disable Channels service",
                option -> GlobalPreferences.instance(getContext()).enableChannelsService(!option.isSelected()),
                !GlobalPreferences.instance(getContext()).isChannelsServiceEnabled()));

        // Disabled inside RetrofitHelper
        //options.add(UiOptionItem.from("Prefer IPv4 DNS",
        //        option -> GlobalPreferences.instance(getContext()).preferIPv4Dns(option.isSelected()),
        //        GlobalPreferences.instance(getContext()).isIPv4DnsPreferred()));
        //
        //options.add(UiOptionItem.from("Enable DNS over HTTPS",
        //        option -> GlobalPreferences.instance(getContext()).enableDnsOverHttps(option.isSelected()),
        //        GlobalPreferences.instance(getContext()).isDnsOverHttpsEnabled()));

        // Need to be enabled on older version of ExoPlayer (e.g. 2.10.6).
        // It's because there's no tweaks for modern devices.
        //options.add(UiOptionItem.from("Enable set output surface workaround",
        //        option -> mPlayerTweaksData.enableSetOutputSurfaceWorkaround(option.isSelected()),
        //        mPlayerTweaksData.isSetOutputSurfaceWorkaroundEnabled()));

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.player_tweaks), options);
    }

    private void appendSeekTypeCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(getContext().getString(R.string.player_seek_regular),
                option -> {
                    mPlayerData.enableSeekConfirmPause(false);
                    mPlayerData.enableSeekConfirmPlay(false);
                },
                !mPlayerData.isSeekConfirmPauseEnabled() && !mPlayerData.isSeekConfirmPlayEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_seek_confirmation_pause),
                option -> {
                    mPlayerData.enableSeekConfirmPause(true);
                    mPlayerData.enableSeekConfirmPlay(false);
                },
                mPlayerData.isSeekConfirmPauseEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_seek_confirmation_play),
                option -> {
                    mPlayerData.enableSeekConfirmPause(false);
                    mPlayerData.enableSeekConfirmPlay(true);
                },
                mPlayerData.isSeekConfirmPlayEnabled()));

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.player_seek_type), options);
    }

    private void appendEndingTimeCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(getContext().getString(R.string.option_disabled),
                option -> {
                    mPlayerData.enableRemainingTime(false);
                    mPlayerData.enableEndingTime(false);
                },
                !mPlayerData.isRemainingTimeEnabled() && !mPlayerData.isEndingTimeEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_remaining_time),
                option -> {
                    mPlayerData.enableRemainingTime(true);
                    mPlayerData.enableEndingTime(false);
                },
                mPlayerData.isRemainingTimeEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_ending_time),
                option -> {
                    mPlayerData.enableEndingTime(true);
                    mPlayerData.enableRemainingTime(false);
                },
                mPlayerData.isEndingTimeEnabled()));

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.player_show_ending_time), options);
    }

    private void appendMiscCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        //options.add(UiOptionItem.from(getContext().getString(R.string.player_full_date),
        //        option -> mPlayerData.enableAbsoluteDate(option.isSelected()),
        //        mPlayerData.isAbsoluteDateEnabled()));

        //options.add(UiOptionItem.from(getContext().getString(R.string.player_time_correction),
        //        option -> mPlayerData.enableTimeCorrection(option.isSelected()),
        //        mPlayerData.isTimeCorrectionEnabled()));

        //options.add(UiOptionItem.from(getContext().getString(R.string.player_pause_when_seek),
        //        option -> mPlayerData.enableSeekMemory(option.isSelected()),
        //        mPlayerData.isSeekMemoryEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.real_channel_icon),
                option -> mPlayerTweaksData.enableRealChannelIcon(option.isSelected()),
                mPlayerTweaksData.isRealChannelIconEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.place_chat_left),
                option -> mPlayerTweaksData.placeChatLeft(option.isSelected()),
                mPlayerTweaksData.isChatPlacedLeft()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_disable_suggestions),
                option -> mPlayerTweaksData.disableSuggestions(option.isSelected()),
                mPlayerTweaksData.isSuggestionsDisabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_number_key_seek),
                option -> mPlayerData.enableNumberKeySeek(option.isSelected()),
                mPlayerData.isNumberKeySeekEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_tooltips),
                option -> mPlayerData.enableTooltips(option.isSelected()),
                mPlayerData.isTooltipsEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_clock),
                option -> mPlayerData.enableClock(option.isSelected()),
                mPlayerData.isClockEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_quality_info),
                option -> mPlayerData.enableQualityInfo(option.isSelected()),
                mPlayerData.isQualityInfoEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_global_clock),
                option -> mPlayerData.enableGlobalClock(option.isSelected()),
                mPlayerData.isGlobalClockEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.player_show_global_ending_time),
                option -> mPlayerData.enableGlobalEndingTime(option.isSelected()),
                mPlayerData.isGlobalEndingTimeEnabled()));

        options.add(UiOptionItem.from(getContext().getString(R.string.remember_position_of_short_videos),
                option -> mPlayerTweaksData.enableRememberPositionOfShortVideos(option.isSelected()),
                mPlayerTweaksData.isRememberPositionOfShortVideosEnabled()));

        //OptionItem remainingTime = UiOptionItem.from(getContext().getString(R.string.player_show_remaining_time),
        //        option -> mPlayerData.enableRemainingTime(option.isSelected()), mPlayerData.isRemainingTimeEnabled());
        //
        //OptionItem endingTime = UiOptionItem.from(getContext().getString(R.string.player_show_ending_time),
        //        option -> mPlayerData.enableEndingTime(option.isSelected()), mPlayerData.isEndingTimeEnabled());
        //
        //remainingTime.setRadio(endingTime);
        //endingTime.setRadio(remainingTime);
        //
        //options.add(remainingTime);
        //options.add(endingTime);

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.player_other), options);
    }
}
