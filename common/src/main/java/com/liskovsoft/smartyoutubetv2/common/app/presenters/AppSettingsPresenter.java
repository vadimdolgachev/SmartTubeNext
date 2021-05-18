package com.liskovsoft.smartyoutubetv2.common.app.presenters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controller.PlaybackEngineController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.PlayerUiManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppSettingsView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;

import java.util.ArrayList;
import java.util.List;

public class AppSettingsPresenter extends BasePresenter<AppSettingsView> {
    @SuppressLint("StaticFieldLeak")
    private static AppSettingsPresenter sInstance;
    private final List<SettingsCategory> mCategories;
    private final Handler mHandler;
    private final Runnable mCloseDialog = this::closeDialog;
    private String mTitle;
    private Runnable mOnClose;
    private PlayerUiManager mUiManager;
    private int mEnginePlaybackMode;
    private long mTimeoutMs;

    public static class SettingsCategory {
        public static SettingsCategory radioList(String title, List<OptionItem> items) {
            return new SettingsCategory(title, items, TYPE_RADIO_LIST);
        }

        public static SettingsCategory checkedList(String title, List<OptionItem> items) {
            return new SettingsCategory(title, items, TYPE_CHECKBOX_LIST);
        }

        public static SettingsCategory stringList(String title, List<OptionItem> items) {
            return new SettingsCategory(title, items, TYPE_STRING_LIST);
        }

        public static SettingsCategory singleSwitch(OptionItem item) {
            ArrayList<OptionItem> items = new ArrayList<>();
            items.add(item);
            return new SettingsCategory(null, items, TYPE_SINGLE_SWITCH);
        }

        public static SettingsCategory singleButton(OptionItem item) {
            ArrayList<OptionItem> items = new ArrayList<>();
            items.add(item);
            return new SettingsCategory(null, items, TYPE_SINGLE_BUTTON);
        }

        private SettingsCategory(String title, List<OptionItem> items, int type) {
            this.type = type;
            this.title = title;
            this.items = items;
        }

        public static final int TYPE_RADIO_LIST = 0;
        public static final int TYPE_CHECKBOX_LIST = 1;
        public static final int TYPE_SINGLE_SWITCH = 2;
        public static final int TYPE_SINGLE_BUTTON = 3;
        public static final int TYPE_STRING_LIST = 4;
        public int type;
        public String title;
        public List<OptionItem> items;
    }

    public AppSettingsPresenter(Context context) {
        super(context);
        mCategories = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static AppSettingsPresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new AppSettingsPresenter(context);
        }

        sInstance.setContext(context);

        return sInstance;
    }

    /**
     * Called after {@link #onClose}
     */
    @Override
    public void onViewDestroyed() {
        clear();
    }

    /**
     * Called when user pressed back button.
     */
    public void onClose() {
        clear();

        enablePlayerUiAutoHide(true);
        blockPlayerEngine(false);

        if (mOnClose != null) {
            mOnClose.run();
        }
    }

    public void clear() {
        mTimeoutMs = 0;
        mHandler.removeCallbacks(mCloseDialog);
        mCategories.clear();
    }

    @Override
    public void onViewInitialized() {
        getView().setTitle(mTitle);
        getView().addCategories(mCategories);
    }

    public void setPlayerUiManager(PlayerUiManager uiManager) {
        mUiManager = uiManager;
    }

    public void showDialog() {
        showDialog(null, null);
    }

    public void showDialog(String dialogTitle) {
        showDialog(dialogTitle, null);
    }

    public void showDialog(Runnable onClose) {
        showDialog(null, onClose);
    }

    public void showDialog(String dialogTitle, Runnable onClose) {
        mTitle = dialogTitle;
        mOnClose = onClose;

        enablePlayerUiAutoHide(false);
        blockPlayerEngine(true);

        if (getView() != null) {
            getView().clear();
            onViewInitialized();
        }

        ViewManager.instance(getContext()).startView(AppSettingsView.class, true);

        setupTimeout();
    }

    public void closeDialog() {
        if (getView() != null) {
            getView().finish();
        }
    }

    public boolean isDialogShown() {
        return !mCategories.isEmpty();
    }

    public void appendRadioCategory(String categoryTitle, List<OptionItem> items) {
        mCategories.add(SettingsCategory.radioList(categoryTitle, items));
    }

    public void appendCheckedCategory(String categoryTitle, List<OptionItem> items) {
        mCategories.add(SettingsCategory.checkedList(categoryTitle, items));
    }

    public void appendStringsCategory(String categoryTitle, List<OptionItem> items) {
        mCategories.add(SettingsCategory.stringList(categoryTitle, items));
    }

    public void appendSingleSwitch(OptionItem optionItem) {
        mCategories.add(SettingsCategory.singleSwitch(optionItem));
    }

    public void appendSingleButton(OptionItem optionItem) {
        mCategories.add(SettingsCategory.singleButton(optionItem));
    }

    public void showDialogMessage(String dialogTitle, Runnable onClose, int timeoutMs) {
        showDialog(dialogTitle, onClose);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getView() != null) {
                getView().finish();
            }
        }, timeoutMs);
    }

    public void setTimoutMs(long timeoutMs) {
        mTimeoutMs = timeoutMs;
    }

    private void setupTimeout() {
        mHandler.removeCallbacks(mCloseDialog);

        if (mTimeoutMs > 0) {
            mHandler.postDelayed(mCloseDialog, mTimeoutMs);
        }
    }

    private void blockPlayerEngine(boolean block) {
        if (mUiManager != null && mUiManager.getController() != null) {
            // Old Android fix: don't destroy player while dialog is open
            if (VERSION.SDK_INT < 25) {
                if (block) {
                    mEnginePlaybackMode = mUiManager.getController().getPlaybackMode(); // save orig value for later restoration
                    mUiManager.getController().setPlaybackMode(PlaybackEngineController.BACKGROUND_MODE_SOUND);
                } else {
                    mUiManager.getController().setPlaybackMode(mEnginePlaybackMode);
                }
            }
        }
    }

    private void enablePlayerUiAutoHide(boolean enable) {
        if (mUiManager != null) {
            if (enable) {
                mUiManager.enableUiAutoHideTimeout();
            } else {
                mUiManager.disableUiAutoHideTimeout();
            }
        }
    }
}
