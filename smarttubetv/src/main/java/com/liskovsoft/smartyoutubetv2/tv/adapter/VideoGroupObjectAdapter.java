package com.liskovsoft.smartyoutubetv2.tv.adapter;

import androidx.leanback.widget.ObjectAdapter;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.tv.presenter.CardPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;

import java.util.ArrayList;
import java.util.List;

public class VideoGroupObjectAdapter extends ObjectAdapter {
    private static final String TAG = VideoGroupObjectAdapter.class.getSimpleName();
    private final List<Video> mMediaItems;
    private VideoGroup mLastGroup;

    // TODO: Select presenter based on the video item type. Such channel, playlist, or simple video
    // https://github.com/googlearchive/leanback-showcase/blob/master/app/src/main/java/android/support/v17/leanback/supportleanbackshowcase/app/page/PageAndListRowFragment.java
    // CardPresenterSelector cardPresenter = new CardPresenterSelector(getActivity());
    public VideoGroupObjectAdapter(VideoGroup videoGroup, CardPresenter presenter) {
        super(presenter);
        mMediaItems = new ArrayList<>();

        if (videoGroup != null) {
            append(videoGroup);
        }
    }

    public VideoGroupObjectAdapter(CardPresenter presenter) {
        this(null, presenter);
    }

    @Override
    public int size() {
        return mMediaItems.size();
    }

    @Override
    public Object get(int position) {
        return mMediaItems.get(position);
    }

    public void append(VideoGroup group) {
        if (group != null) {
            int begin = mMediaItems.size();

            mMediaItems.addAll(group.getVideos());
            mLastGroup = group;

            // Fix double item blinking by specifying exact range
            notifyItemRangeInserted(begin, mMediaItems.size() - begin);
        }
    }

    public VideoGroup getGroup() {
        return mLastGroup;
    }

    public int indexOf(Video item) {
        return mMediaItems.indexOf(item);
    }

    public void clear() {
        int itemCount = mMediaItems.size();
        if (itemCount == 0) {
            return;
        }
        mMediaItems.clear();
        notifyItemRangeRemoved(0, itemCount);
    }
}
