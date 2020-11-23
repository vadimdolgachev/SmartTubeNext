package com.liskovsoft.smartyoutubetv2.common.app.presenters;

import android.annotation.SuppressLint;
import android.content.Context;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.Presenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.utils.RxUtils;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SignInPresenter implements Presenter<SignInView> {
    private static final String TAG = SignInPresenter.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static SignInPresenter sInstance;
    private final MediaService mMediaService;
    private final Context mContext;
    private final BrowsePresenter mBrowsePresenter;
    private final SplashPresenter mSplashPresenter;
    private SignInView mView;
    private String mUserCode;
    private Disposable mSignInAction;

    private SignInPresenter(Context context) {
        mContext = context;
        mMediaService = YouTubeMediaService.instance(LocaleUtility.getCurrentLocale(context));
        mBrowsePresenter = BrowsePresenter.instance(context);
        mSplashPresenter = SplashPresenter.instance(context);
    }

    public static SignInPresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new SignInPresenter(context.getApplicationContext());
        }

        return sInstance;
    }

    public void unhold() {
        RxUtils.disposeActions(mSignInAction);
        sInstance = null;
    }

    @Override
    public void register(SignInView view) {
        mView = view;
    }

    @Override
    public void unregister(SignInView view) {
        mView = null;
        unhold();
    }

    @Override
    public void onInitDone() {
        RxUtils.disposeActions(mSignInAction);
        updateUserCode();
    }

    public void onActionClicked() {
        mView.close();
    }

    private void updateUserCode() {
        mSignInAction = mMediaService.getSignInManager().signInObserve()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userCode -> mView.showCode(userCode),
                        error -> Log.e(TAG, error),
                        () -> {
                            // Success
                            mBrowsePresenter.refresh();
                            if (mView != null) {
                                mView.close();
                            }
                            mSplashPresenter.updateChannels();
                        });
    }

    public void start() {
        RxUtils.disposeActions(mSignInAction);
        ViewManager.instance(mContext).startView(SignInView.class);
    }
}
