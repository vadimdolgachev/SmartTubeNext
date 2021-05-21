package com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.tv.R;


public class KeyBoard extends FrameLayout {

    FrameLayout flKeyContainer;
    TextView tvButtonText;

    private OnClickListener iOnClickListener;

    public KeyBoard(Context context) {
        this(context, null);
    }

    public KeyBoard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyBoard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.keyboard_btn, this);

        tvButtonText = findViewById(R.id.tvButtonText);
        flKeyContainer = findViewById(R.id.flKeyContainer);

        flKeyContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iOnClickListener != null) {
                    iOnClickListener.onClick(v);
                }
            }
        });
    }

    public void setTvButtonText(String text) {
        if (tvButtonText != null) {
            tvButtonText.setText(text);
        }
    }

    public void setiOnClickListener(OnClickListener iOnClickListener) {
        this.iOnClickListener = iOnClickListener;
    }

    public String getValue() {
        if (tvButtonText != null) {
            return tvButtonText.getText().toString();
        }
        return null;
    }
}
