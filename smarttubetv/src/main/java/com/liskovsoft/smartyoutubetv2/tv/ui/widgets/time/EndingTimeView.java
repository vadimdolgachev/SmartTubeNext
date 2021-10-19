package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.widget.AppCompatTextView;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.misc.TickleManager;
import com.liskovsoft.smartyoutubetv2.common.misc.TickleManager.TickleListener;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.utils.DateFormatter;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class EndingTimeView extends AppCompatTextView implements TickleListener {
    private TickleManager mTickleManager;

    public EndingTimeView(Context context) {
        super(context);
        init();
    }

    public EndingTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EndingTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTickleManager = TickleManager.instance();
        updateListener();
    }

    private void updateListener() {
        mTickleManager.removeListener(this);

        if (getVisibility() == View.VISIBLE) {
            mTickleManager.addListener(this);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        updateListener();
    }

    @Override
    public void onTickle() {
        if (getVisibility() == View.VISIBLE) {
            String endingTime = getEndingTime();
            if (endingTime != null) {
                setText(endingTime);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Player has been closed
        mTickleManager.removeListener(this);
    }

    private String getEndingTime() {
        PlaybackView playbackView = PlaybackPresenter.instance(getContext()).getView();

        long remainingTimeMs = 0;

        if (playbackView != null) {
            remainingTimeMs = playbackView.getController().getLengthMs() - playbackView.getController().getPositionMs();
            remainingTimeMs = applySpeedCorrection(remainingTimeMs);
        }

        if (remainingTimeMs == 0) {
            return null;
        }

        return String.format("⌛ %s", DateFormatter.formatTimeShort(getContext(), System.currentTimeMillis() + remainingTimeMs));
        //return getContext().getString(R.string.player_ending_time, DateFormatter.formatTimeShort(getContext(), System.currentTimeMillis() + remainingTimeMs));
    }

    private long applySpeedCorrection(long timeMs) {
        timeMs = (long) (timeMs / PlayerData.instance(getContext()).getSpeed());

        return timeMs >= 0 ? timeMs : 0;
    }
}
