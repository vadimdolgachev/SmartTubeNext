package com.liskovsoft.smartyoutubetv2.tv.ui.search.tags;

import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc.SearchSupportFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard.Alphabet;
import com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard.IOnKeyboardKeyClickListener;
import com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard.KeyBoardCustomView;

public class SearchTagsActivity extends LeanbackActivity {

    static final String TAG = SearchTagsActivity.class.getSimpleName();

    private SearchTagsFragment mFragment;
    private boolean mDownPressed;
    public KeyBoardCustomView keyBoardCustomView;
    public FrameLayout btnSearch;
    public FrameLayout btnCatch;
    public EditText etSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_search_tags);
        keyBoardCustomView = (KeyBoardCustomView) findViewById(R.id.pkvKeyboard);
        keyBoardCustomView.initAlphabet(Alphabet.en_en);

        btnSearch = (FrameLayout) findViewById(R.id.btnSearch);
        btnCatch = (FrameLayout) findViewById(R.id.btnCatch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFragment.startSearch(etSearch.getText().toString());
            }
        });
        btnSearch.setNextFocusRightId(btnSearch.getId());
        etSearch = (EditText) findViewById(R.id.etSearch);
        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    Log.d(TAG, "onFocusChange: ");
                    visibleKeyBoard();
                }
            }
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mFragment.sendText(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Selection.setSelection(etSearch.getText(), etSearch.length());
            }
        });

        btnCatch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    keyBoardCustomView.requestFocus();
                }
            }
        });
        mFragment = (SearchTagsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_tags_fragment);
        mFragment.setEtId(etSearch.getId());
        //Клавиатура
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyBoardCustomView.setOnKeyboardKeyClickListener(keyboardKeyClickListener);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If there are no results found, press the left key to reselect the microphone
        Log.d(TAG, "onKeyDown: ");
        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 1) {
            mFragment.pressKeySearch();
        }
        return super.onKeyDown(keyCode, event);
    }


    public void hideKeyBoard() {
        Log.d(TAG, "hideKeyBoard: " + keyBoardCustomView);
        if (keyBoardCustomView != null && keyBoardCustomView.getVisibility() != View.GONE) {
            keyBoardCustomView.setVisibility(View.GONE);
            btnSearch.setVisibility(View.GONE);
        }
    }

    public void visibleKeyBoard() {
        Log.d(TAG, "visibleKeyBoard: " + keyBoardCustomView + " isVisible " + keyBoardCustomView.getVisibility());
        if (keyBoardCustomView != null && keyBoardCustomView.getVisibility() != View.VISIBLE) {
            btnSearch.setVisibility(View.VISIBLE);
            keyBoardCustomView.setVisibility(View.VISIBLE);
            keyBoardCustomView.requestFocus();
        }
    }

    public void visibleEtSearch() {
        if (etSearch != null && etSearch.getVisibility() != View.VISIBLE) {
            etSearch.setVisibility(View.VISIBLE);
        }
    }

    public void hideEtSearch() {
        if (etSearch != null && etSearch.getVisibility() != View.GONE) {
            etSearch.setVisibility(View.GONE);
        }
    }

    public void setTextEtSeach(String val) {
        etSearch.setText(val);
    }

    private void updateField(String val, EditText editText) {
        String entered = editText.getText().toString();

        if (entered.length() == 0 && val == null) {
            return;
        }

        if (val == null && entered.length() > 0) {
            entered = entered.substring(0, entered.length() - 1);
        } else {
            entered = entered + val;
        }

        editText.setText(entered);
    }

    private IOnKeyboardKeyClickListener keyboardKeyClickListener = new IOnKeyboardKeyClickListener() {
        @Override
        public void onKeyClicked(String key) {
            if (key == null) {
                updateField(key, etSearch);
                return;
            }
            switch (key) {
                case "SPACE":
                    updateField(" ", etSearch);
                    break;
                case "CLEAR":
                    etSearch.setText("");
                    break;
                default:
                    updateField(key, etSearch);
            }

        }
    };
}
