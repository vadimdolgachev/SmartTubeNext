package com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.tv.R;


public class KeyBoardCustomView extends FrameLayout {

    private static final String TAG = KeyBoardCustomView.class.getName();

    String currentKeyBoard;

    String currentRegister = "low";

    LinearLayout llFirstRow;

    LinearLayout llSecondRow;

    LinearLayout llThirdRow;

    LinearLayout llFourRow;

    Button btnPaymentKeyZeroTwo;

    FrameLayout btnPaymentKeyClear;

    FrameLayout btnUpperCase;

    FrameLayout btnSpace;

    FrameLayout btnClear;

    FrameLayout changeLang;

    FrameLayout btnChangeNum;


    TextView tvNum;

    private IOnKeyboardKeyClickListener onKeyboardKeyClickListener;

    public KeyBoardCustomView(Context context) {
        this(context, null);
    }


    public KeyBoardCustomView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyBoardCustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.keyboard_view, this);
        llFirstRow = (LinearLayout) findViewById(R.id.llFirstRow);
        llSecondRow = (LinearLayout) findViewById(R.id.llSecondRow);
        llThirdRow = (LinearLayout) findViewById(R.id.llThirdRow);
        llFourRow = (LinearLayout) findViewById(R.id.llFourRow);

        btnPaymentKeyClear = findViewById(R.id.btnPaymentKeyClear);
        btnUpperCase = findViewById(R.id.btnUpperCase);
        btnSpace = findViewById(R.id.btnSpace);
        btnClear = findViewById(R.id.btnClear);
        changeLang = findViewById(R.id.changeLang);
        btnChangeNum = findViewById(R.id.btnChangeNum);
        tvNum = findViewById(R.id.tvNum);
    }

    public void changeKeyboard(String type) {
        clearViews();
        initAlphabet(type);
    }

    public void initAlphabet(String type) {
        llFirstRow.setNextFocusLeftId(llFirstRow.getId());
        llSecondRow.setNextFocusLeftId(llSecondRow.getId());
        llThirdRow.setNextFocusLeftId(llThirdRow.getId());
        llFourRow.setNextFocusLeftId(llFourRow.getId());

        Alphabet alphabet = KeyBoardHelper.getAlphabet(type);
        currentKeyBoard = type;
        //-----------------------------Первый ряд---------------------------------------------------

        for (String ch : alphabet.getFirstRow()) {
            final KeyBoard keyView = new KeyBoard(getContext());
            if (currentRegister.equals("UP")) {
                ch = ch.toUpperCase();
            }
            keyView.setTvButtonText(ch);
            keyView.setiOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: ");
                    if (onKeyboardKeyClickListener != null) {
                        onKeyboardKeyClickListener.onKeyClicked(keyView.getValue());
                    }
                }
            });
            llFirstRow.addView(keyView);
        }

        //-----------------------------Второй ряд---------------------------------------------------

        for (String ch : alphabet.getTwoRow()) {
            final KeyBoard keyView = new KeyBoard(getContext());
            if (currentRegister.equals("UP")) {
                ch = ch.toUpperCase();
            }
            keyView.setTvButtonText(ch);
            keyView.setiOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: ");
                    if (onKeyboardKeyClickListener != null) {
                        onKeyboardKeyClickListener.onKeyClicked(keyView.getValue());
                    }
                }
            });
            llSecondRow.addView(keyView);
        }
        //-----------------------------Третий ряд---------------------------------------------------

        for (String ch : alphabet.getThreeRow()) {
            final KeyBoard keyView = new KeyBoard(getContext());
            if (currentRegister.equals("UP")) {
                ch = ch.toUpperCase();
            }
            keyView.setTvButtonText(ch);
            keyView.setiOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: ");
                    if (onKeyboardKeyClickListener != null) {
                        onKeyboardKeyClickListener.onKeyClicked(keyView.getValue());
                    }
                }
            });
            llThirdRow.addView(keyView);
        }
        //-----------------------------Четвертый ряд------------------------------------------------

        for (String ch : alphabet.getFourRow()) {
            final KeyBoard keyView = new KeyBoard(getContext());
            if (currentRegister.equals("UP")) {
                ch = ch.toUpperCase();
            }
            keyView.setTvButtonText(ch);
            keyView.setiOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: ");
                    if (onKeyboardKeyClickListener != null) {
                        onKeyboardKeyClickListener.onKeyClicked(keyView.getValue());
                    }
                }
            });
            llFourRow.addView(keyView);
        }

        btnPaymentKeyClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onKeyboardKeyClickListener != null) {
                    onKeyboardKeyClickListener.onKeyClicked(null);
                }
            }
        });

        btnPaymentKeyClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onKeyboardKeyClickListener != null) {
                    onKeyboardKeyClickListener.onKeyClicked(null);
                }
            }
        });

        btnSpace.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onKeyboardKeyClickListener != null) {
                    onKeyboardKeyClickListener.onKeyClicked("SPACE");
                }
            }
        });

        btnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onKeyboardKeyClickListener != null) {
                    onKeyboardKeyClickListener.onKeyClicked("CLEAR");
                }
            }
        });

        changeLang.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (currentKeyBoard) {
                    case Alphabet.en_en:
                        changeKeyboard(Alphabet.ru_ru);
                        break;
                    case Alphabet.ru_ru:
                        changeKeyboard(Alphabet.en_en);
                        break;
                }
            }
        });

        btnUpperCase.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentRegister.equals("DOWN")) {
                    currentRegister = "UP";
                } else {
                    currentRegister = "DOWN";
                }
                changeKeyboard(currentKeyBoard);
            }
        });

        btnChangeNum.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentKeyBoard.equals(Alphabet.num)) {
                    changeKeyboard(Alphabet.ru_ru);
                    tvNum.setText("?123");
                } else {
                    changeKeyboard(Alphabet.num);
                    tvNum.setText("ABC");
                }
            }
        });


    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow: ");
        clearViews();
    }

    private void clearViews() {
        llFirstRow.removeAllViews();
        llSecondRow.removeAllViews();
        llThirdRow.removeAllViews();
        llFourRow.removeAllViews();
    }

    public void setOnKeyboardKeyClickListener(IOnKeyboardKeyClickListener onKeyboardKeyClickListener) {
        this.onKeyboardKeyClickListener = onKeyboardKeyClickListener;
    }


}
