package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.ModeSyncManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.keyhandler.DoubleBackManager;

/**
 * This parent class contains common methods that run in every activity such as search.
 */
public abstract class LeanbackActivity extends MotherActivity {
    private static final String TAG = LeanbackActivity.class.getSimpleName();
    private UriBackgroundManager mBackgroundManager;
    private ViewManager mViewManager;
    private ModeSyncManager mModeSyncManager;
    private DoubleBackManager mDoubleBackManager;
    private MainUIData mMainUiData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fix situations when the app is killed but the activity is restored by the system
        //if (savedInstanceState != null) {
        //    finishTheApp();
        //}
        mBackgroundManager = new UriBackgroundManager(this);
        mViewManager = ViewManager.instance(this);
        mModeSyncManager = ModeSyncManager.instance();
        mDoubleBackManager = new DoubleBackManager(this);
        mMainUiData = MainUIData.instance(this);
    }

    @Override
    public boolean onSearchRequested() {
        SearchPresenter.instance(this).startSearch(null);
        return true;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, event);

        if (mDoubleBackManager.checkDoubleBack(event)) {
            finishTheApp();
        }

        return super.dispatchKeyEvent(event);
    }

    public UriBackgroundManager getBackgroundManager() {
        return mBackgroundManager;
    }

    @Override
    protected void onStart() {
        super.onStart();

        mBackgroundManager.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // PIP fix: While entering/exiting PIP mode only Pause/Resume is called

        mModeSyncManager.restore(this);

        mViewManager.addTop(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBackgroundManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBackgroundManager.onDestroy();
    }

    @Override
    public void finish() {
        // user pressed back key
        if (!mViewManager.startParentView(this)) {
            switch (mMainUiData.getAppExitShortcut()) {
                case MainUIData.EXIT_DOUBLE_BACK:
                    mDoubleBackManager.enableDoubleBackExit();
                    break;
                case MainUIData.EXIT_SINGLE_BACK:
                    finishTheApp();
                    break;
            }
        } else {
            finishReally();
        }
    }

    private void finishTheApp() {
        mViewManager.properlyFinishTheApp(this);
    }
}
