package com.liskovsoft.smartyoutubetv2.common.app.presenters.base;

import android.app.Activity;
import android.content.Context;
import androidx.fragment.app.Fragment;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
//import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.BootDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
//import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.BootDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.BootDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.Presenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.SearchData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public abstract class BasePresenter<T> implements Presenter<T> {
    private WeakReference<T> mView = new WeakReference<>(null);
    private WeakReference<Activity> mActivity = new WeakReference<>(null);
    private WeakReference<Context> mApplicationContext = new WeakReference<>(null);
    private Runnable mOnDone;
    private static boolean sRunOnce;
    private long mUpdateCheckMs;
    private static boolean mNeedSync;

    public BasePresenter(Context context) {
        setContext(context);
    }

    @Override
    public void setView(T view) {
        if (view != null) {
            mView = new WeakReference<>(view);
        }
    }

    @Override
    public T getView() {
        T view = mView.get();
        return checkView(view) ? view : null;
    }

    @Override
    public void setContext(Context context) {
        if (context == null) {
            return;
        }

        if (!sRunOnce) {
            sRunOnce = true;
            // Init shared prefs used inside remote control service.
            Utils.initGlobalData(context);
        }

        // Localization fix: prefer Activity context
        if (context instanceof Activity && Utils.checkActivity((Activity) context)) {
            mActivity = new WeakReference<>((Activity) context);
        }

        // In case view was disposed like SplashView does
        mApplicationContext = new WeakReference<>(context.getApplicationContext());

        //initGlobalData();
    }

    @Override
    public Context getContext() {
        Activity activity = null;

        Activity viewActivity = getViewActivity(mView.get());

        // Trying to find localized context.
        // First, try the view that belongs to this presenter.
        // Second, try the activity that presenter called (could be destroyed).
        if (viewActivity != null) {
            activity = viewActivity;
        } else if (mActivity.get() != null) {
            activity = mActivity.get();
        }

        // In case view was disposed like SplashView does
        // Fallback to non-localized ApplicationContext if others fail
        return Utils.checkActivity(activity) ? activity : mApplicationContext.get();
    }

    @Override
    public void onViewInitialized() {
        enableSync();
        showBootDialogs();
    }

    @Override
    public void onViewDestroyed() {
        // View stays in RAM after has been destroyed. Is it a bug?
        mView = new WeakReference<>(null);
        mActivity = new WeakReference<>(null);
    }

    @Override
    public void onViewResumed() {
        if (mNeedSync && canViewBeSynced()) {
            // NOTE: don't place cleanup in the onViewResumed!!! This could cause errors when view is resumed.
            syncItem(Playlist.instance().getChangedItems());
        }

        showBootDialogs();
    }

    @Override
    public void onFinish() {
        if (SearchData.instance(getContext()).getTempBackgroundModeClass() == this.getClass() &&
            PlaybackPresenter.instance(getContext()).isRunningInBackground()) {
            ViewManager.instance(getContext()).startView(SplashView.class);
        }
    }

    public void setOnDone(Runnable onDone) {
        mOnDone = onDone;
    }

    protected void onDone() {
        if (mOnDone != null) {
            mOnDone.run();
            mOnDone = null;
        }
    }

    protected void removeItem(Video item) {
        removeItem(Collections.singletonList(item), VideoGroup.ACTION_REMOVE);
    }

    protected void removeItemAuthor(Video item) {
        removeItem(Collections.singletonList(item), VideoGroup.ACTION_REMOVE_AUTHOR);
    }

    private void removeItem(List<Video> items, int action) {
        if (items.size() == 0) {
            return;
        }

        VideoGroup removedGroup = VideoGroup.from(items);
        removedGroup.setAction(action);
        T view = getView();

        updateView(removedGroup, view);
    }

    public void syncItem(Video item) {
        syncItem(Collections.singletonList(item));
    }

    public void syncItem(List<Video> items) {
        if (items.size() == 0) {
            return;
        }

        VideoGroup syncGroup = VideoGroup.from(items);
        syncGroup.setAction(VideoGroup.ACTION_SYNC);
        T view = getView();

        if (updateView(syncGroup, view)) {
            mNeedSync = false;
        }
    }

    private boolean canViewBeSynced() {
        T view = getView();
        return view instanceof BrowseView ||
               view instanceof ChannelView ||
               view instanceof ChannelUploadsView ||
               view instanceof SearchView;
    }

    private boolean updateView(VideoGroup group, T view) {
        if (view instanceof BrowseView) {
            ((BrowseView) view).updateSection(group);
        } else if (view instanceof ChannelView) {
            ((ChannelView) view).update(group);
        } else if (view instanceof ChannelUploadsView) {
            ((ChannelUploadsView) view).update(group);
        } else if (view instanceof SearchView) {
            ((SearchView) view).updateSearch(group);
        } else {
            return false;
        }

        return true;
    }

    private void enableSync() {
        if (this instanceof PlaybackPresenter) {
            mNeedSync = true;
            Playlist.instance().onNewSession();
        }
    }

    private void showBootDialogs() {

    }

    /**
     * Check that view's activity is alive
     */
    private static <T> boolean checkView(T view) {
        Activity activity = getViewActivity(view);

        return Utils.checkActivity(activity);
    }

    private static <T> Activity getViewActivity(T view) {
        Activity activity = null;

        if (view instanceof Fragment) { // regular fragment
            activity = ((Fragment) view).getActivity();
        } else if (view instanceof android.app.Fragment) { // dialog fragment
            activity = ((android.app.Fragment) view).getActivity();
        } else if (view instanceof Activity) { // splash view
            activity = (Activity) view;
        }
        return activity;
    }
}
