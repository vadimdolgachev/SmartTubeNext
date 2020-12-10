package com.liskovsoft.smartyoutubetv2.common.app.models.search;

import android.util.Log;

import com.liskovsoft.mediaserviceinterfaces.MediaGroupManager;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.utils.RxUtils;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;

import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MediaServiceSearchTagProvider implements SearchTagsProvider {
    private final MediaGroupManager mGroupManager;
    private Disposable mTagsAction;

    public MediaServiceSearchTagProvider(Locale currentLocale) {
        MediaService mediaService = YouTubeMediaService.instance(currentLocale);
        mGroupManager = mediaService.getMediaGroupManager();
    }

    @Override
    public void search(String query, ResultsCallback callback) {
        Log.d("Search", "[Sergey] search tag: ");
        RxUtils.disposeActions(mTagsAction);

        mTagsAction = mGroupManager.getSearchTagsObserve(query)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tags -> {
                    callback.onResults(Tag.from(tags));
                });
    }
}
