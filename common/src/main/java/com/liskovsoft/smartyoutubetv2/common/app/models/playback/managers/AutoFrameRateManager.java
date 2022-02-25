package com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.PlayerEventListenerHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.AutoFrameRateHelper;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.ModeSyncManager;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.internal.DisplayHolder.Mode;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.internal.DisplaySyncHelper.AutoFrameRateListener;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.internal.UhdHelper;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.utils.TvQuickActions;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AutoFrameRateManager extends PlayerEventListenerHelper implements AutoFrameRateListener {
    private static final String TAG = AutoFrameRateManager.class.getSimpleName();
    private static final int AUTO_FRAME_RATE_ID = 21;
    private static final int AUTO_FRAME_RATE_DELAY_ID = 22;
    private final HQDialogManager mUiManager;
    private final VideoStateManager mStateUpdater;
    private final AutoFrameRateHelper mAutoFrameRateHelper;
    private final ModeSyncManager mModeSyncManager;
    private final Runnable mApplyAfr = this::applyAfr;
    private final Runnable mApplyAfrStop = this::applyAfrStop;
    private final Handler mHandler;
    private PlayerData mPlayerData;
    private boolean mIsPlay;
    private final Runnable mPlaybackResumeHandler = () -> {
        getController().setAfrRunning(false);
        restorePlayback();
    };

    public AutoFrameRateManager(HQDialogManager uiManager, VideoStateManager stateUpdater) {
        mUiManager = uiManager;
        mStateUpdater = stateUpdater;
        mAutoFrameRateHelper = AutoFrameRateHelper.instance(null);
        mAutoFrameRateHelper.setListener(this);
        mModeSyncManager = ModeSyncManager.instance();
        mModeSyncManager.setAfrHelper(mAutoFrameRateHelper);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onInitDone() {
        mPlayerData = PlayerData.instance(getActivity());
        mAutoFrameRateHelper.saveOriginalState(getActivity());
    }

    @Override
    public void onViewResumed() {
        mAutoFrameRateHelper.setFpsCorrectionEnabled(mPlayerData.isAfrFpsCorrectionEnabled());
        mAutoFrameRateHelper.setResolutionSwitchEnabled(mPlayerData.isAfrResSwitchEnabled(), false);
        mAutoFrameRateHelper.setDoubleRefreshRateEnabled(mPlayerData.isDoubleRefreshRateEnabled());

//        addUiOptions();
    }

    @Override
    public void onVideoLoaded(Video item) {
        savePlayback();

        // Sometimes AFR is not working on activity startup. Trying to fix with delay.
        applyAfrDelayed();
        //applyAfr();
    }

    @Override
    public void onModeStart(Mode newMode) {
        // Ugoos already displays this message on each mode switch
        String message = getActivity().getString(
                R.string.auto_frame_rate_applying,
                newMode.getPhysicalWidth(),
                newMode.getPhysicalHeight(),
                newMode.getRefreshRate());
        Log.d(TAG, message);
        //MessageHelpers.showLongMessage(getActivity(), message);
        maybePausePlayback();
    }

    @Override
    public void onModeError(Mode newMode) {
        String msg = getActivity().getString(R.string.msg_mode_switch_error, UhdHelper.toResolution(newMode));
        Log.e(TAG, msg);

        restorePlayback();

        // This error could appear even on success.
        // MessageHelpers.showMessage(getActivity(), msg);
    }

    @Override
    public void onCancel() {
        restorePlayback();
    }

    @Override
    public void onEngineReleased() {
        if (mPlayerData.isAfrEnabled()) {
            applyAfrStopDelayed();
        }
    }

    private void applyAfrStopDelayed() {
        Utils.postDelayed(mHandler, mApplyAfrStop, 200);
    }

    private void applyAfrStop() {
        // Send data to AFR daemon via tvQuickActions app
        TvQuickActions.sendStopAFR(getActivity());
    }

    private void onFpsCorrectionClick() {
        mAutoFrameRateHelper.setFpsCorrectionEnabled(mPlayerData.isAfrFpsCorrectionEnabled());
    }

    private void onResolutionSwitchClick() {
        mAutoFrameRateHelper.setResolutionSwitchEnabled(mPlayerData.isAfrResSwitchEnabled(), mPlayerData.isAfrEnabled());
    }

    private void onDoubleRefreshRateClick() {
        mAutoFrameRateHelper.setDoubleRefreshRateEnabled(mPlayerData.isDoubleRefreshRateEnabled());
    }

    private void applyAfrWrapper() {
        if (mPlayerData.isAfrEnabled()) {
            AppDialogPresenter.instance(getActivity()).showDialogMessage("Applying AFR...", this::applyAfr, 1_000);
        }
    }

    private void applyAfrDelayed() {
        Utils.postDelayed(mHandler, mApplyAfr, 500);
    }

    private void applyAfr() {
        if (mPlayerData.isAfrEnabled()) {
            FormatItem videoFormat = getController().getVideoFormat();
            applyAfr(videoFormat, false);
            // Send data to AFR daemon via tvQuickActions app
            TvQuickActions.sendStartAFR(getActivity(), videoFormat);
        } else {
            restoreAfr();
        }
    }

    private void restoreAfr() {
        String msg = "Restoring original frame rate...";
        Log.d(TAG, msg);
        mAutoFrameRateHelper.restoreOriginalState(getActivity());
        mModeSyncManager.save(null);
    }

    private void applyAfr(FormatItem videoFormat, boolean force) {
        if (videoFormat != null) {
            String msg = String.format("Applying afr... fps: %s, resolution: %sx%s, activity: %s",
                    videoFormat.getFrameRate(),
                    videoFormat.getWidth(),
                    videoFormat.getHeight(),
                    getActivity().getClass().getSimpleName()
            );
            Log.d(TAG, msg);

            mAutoFrameRateHelper.apply(getActivity(), videoFormat, force);
        }
    }

    private void maybePausePlayback() {
        getController().setAfrRunning(true);
        int delayMs = 5_000;

        if (mPlayerData.getAfrPauseSec() > 0) {
            getController().setPlay(false);
            delayMs = mPlayerData.getAfrPauseSec() * 1_000;
        }

        Utils.postDelayed(mHandler, mPlaybackResumeHandler, delayMs);
    }

    private void savePlayback() {
        if (mAutoFrameRateHelper.isSupported() && mPlayerData.isAfrEnabled() && mPlayerData.getAfrPauseSec() > 0) {
            mStateUpdater.blockPlay(true);
            mIsPlay = mStateUpdater.getPlayEnabled();
        }
    }

    private void restorePlayback() {
        if (mAutoFrameRateHelper.isSupported() && mPlayerData.isAfrEnabled() && mPlayerData.getAfrPauseSec() > 0) {
            mStateUpdater.blockPlay(false);
            getController().setPlay(mIsPlay);
        }
    }

    //private void addUiOptions() {
    //    if (mAutoFrameRateHelper.isSupported()) {
    //        OptionCategory afrCategory = createAutoFrameRateCategory(
    //                getActivity(), PlayerData.instance(getActivity()),
    //                () -> {}, this::onResolutionSwitchClick, this::onFpsCorrectionClick, this::onDoubleRefreshRateClick);
    //
    //        OptionCategory afrDelayCategory = createAutoFrameRatePauseCategory(
    //                getActivity(), PlayerData.instance(getActivity()));
    //
    //        mUiManager.addCategory(afrCategory);
    //        mUiManager.addCategory(afrDelayCategory);
    //        mUiManager.addOnDialogHide(mApplyAfr);
    //    } else {
    //        mUiManager.removeCategory(AUTO_FRAME_RATE_ID);
    //        mUiManager.removeCategory(AUTO_FRAME_RATE_DELAY_ID);
    //        mUiManager.removeOnDialogHide(mApplyAfr);
    //    }
    //}

    // Avoid nested dialogs. They have problems with timings. So player controls may hide without user interaction.
    private void addUiOptions() {
        if (mAutoFrameRateHelper.isSupported()) {
            OptionCategory afrCategory = createAutoFrameRateCategory(
                    getActivity(), PlayerData.instance(getActivity()),
                    () -> {}, this::onResolutionSwitchClick, this::onFpsCorrectionClick, this::onDoubleRefreshRateClick);

            OptionCategory afrPauseCategory = createAutoFrameRatePauseCategory(
                    getActivity(), PlayerData.instance(getActivity()));

            // Create nested dialogs

            List<OptionItem> options = new ArrayList<>();
            options.add(UiOptionItem.from(afrCategory.title, optionItem -> {
                AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(getActivity());
                dialogPresenter.clear();
                dialogPresenter.appendCheckedCategory(afrCategory.title, afrCategory.options);
                dialogPresenter.showDialog(afrCategory.title, mApplyAfr);
            }));
            options.add(UiOptionItem.from(afrPauseCategory.title, optionItem -> {
                AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(getActivity());
                dialogPresenter.clear();
                dialogPresenter.appendRadioCategory(afrPauseCategory.title, afrPauseCategory.options);
                dialogPresenter.showDialog(afrPauseCategory.title, mApplyAfr);
            }));

            mUiManager.addCategory(OptionCategory.from(AUTO_FRAME_RATE_ID, OptionCategory.TYPE_STRING, getActivity().getString(R.string.auto_frame_rate), options));
        } else {
            mUiManager.removeCategory(AUTO_FRAME_RATE_ID);
        }
    }

    public static OptionCategory createAutoFrameRateCategory(Context context, PlayerData playerData) {
        return createAutoFrameRateCategory(context, playerData, () -> {}, () -> {}, () -> {}, () -> {});
    }

    private static OptionCategory createAutoFrameRateCategory(Context context, PlayerData playerData,
            Runnable onAfrCallback, Runnable onResolutionCallback, Runnable onFpsCorrectionCallback, Runnable onDoubleRefreshRateCallback) {
        String title = context.getString(R.string.auto_frame_rate);
        String fpsCorrection = context.getString(R.string.frame_rate_correction, "24->23.97, 30->29.97, 60->59.94");
        String resolutionSwitch = context.getString(R.string.resolution_switch);
        String doubleRefreshRate = context.getString(R.string.double_refresh_rate);
        List<OptionItem> options = new ArrayList<>();

        OptionItem afrEnableOption = UiOptionItem.from(title, optionItem -> {
            playerData.setAfrEnabled(optionItem.isSelected());
            onAfrCallback.run();
        }, playerData.isAfrEnabled());
        OptionItem afrResSwitchOption = UiOptionItem.from(resolutionSwitch, optionItem -> {
            playerData.setAfrResSwitchEnabled(optionItem.isSelected());
            onResolutionCallback.run();
        }, playerData.isAfrResSwitchEnabled());
        OptionItem afrFpsCorrectionOption = UiOptionItem.from(fpsCorrection, optionItem -> {
            playerData.setAfrFpsCorrectionEnabled(optionItem.isSelected());
            onFpsCorrectionCallback.run();
        }, playerData.isAfrFpsCorrectionEnabled());
        OptionItem doubleRefreshRateOption = UiOptionItem.from(doubleRefreshRate, optionItem -> {
            playerData.setDoubleRefreshRateEnabled(optionItem.isSelected());
            onDoubleRefreshRateCallback.run();
        }, playerData.isDoubleRefreshRateEnabled());

        afrResSwitchOption.setRequired(afrEnableOption);
        afrFpsCorrectionOption.setRequired(afrEnableOption);
        doubleRefreshRateOption.setRequired(afrEnableOption);

        options.add(afrEnableOption);
        options.add(afrResSwitchOption);
        options.add(afrFpsCorrectionOption);
        options.add(doubleRefreshRateOption);

        return OptionCategory.from(AUTO_FRAME_RATE_ID, OptionCategory.TYPE_CHECKED, title, options);
    }

    public static OptionCategory createAutoFrameRatePauseCategory(Context context, PlayerData playerData) {
        String title = context.getString(R.string.auto_frame_rate_pause);

        List<OptionItem> options = new ArrayList<>();

        for (int pauseSec : new int[] {0, 1, 2, 3, 4, 5, 6, 7}) {
            String optionTitle = pauseSec == 0 ? context.getString(R.string.option_never) : context.getString(R.string.auto_frame_rate_sec, pauseSec);
            options.add(UiOptionItem.from(optionTitle,
                    optionItem -> {
                        playerData.setAfrPauseSec(pauseSec);
                        playerData.setAfrEnabled(true);
                    },
                    pauseSec == playerData.getAfrPauseSec()));
        }

        return OptionCategory.from(AUTO_FRAME_RATE_DELAY_ID, OptionCategory.TYPE_RADIO, title, options);
    }
}
