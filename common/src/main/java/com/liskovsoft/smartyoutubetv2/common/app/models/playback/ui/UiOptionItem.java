package com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui;

import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import android.os.Build;

import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorUtil;

import java.util.ArrayList;
import java.util.List;

public class UiOptionItem implements OptionItem {
    private int mId;
    private CharSequence mTitle;
    private CharSequence mDescription;
    private boolean mIsSelected;
    private FormatItem mFormat;
    private OptionCallback mCallback;
    private Object mData;
    private OptionItem[] mRequiredItems;
    private OptionItem[] mRadioItems;
    private ChatReceiver mChatReceiver;
    private CommentsReceiver mCommentsReceiver;

    private final static int MAX_VIDEO_WIDTH = (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT ? 1280 :
            Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1 ? 1920 : 3840);
    private final static float MAX_FRAME_RATE =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 ? 30f : 60f;

    public static List<OptionItem> from(List<FormatItem> formats, OptionCallback callback) {
        return from(formats, callback, null);
    }

    public static List<OptionItem> from(List<FormatItem> formats, OptionCallback callback, String defaultTitle) {
        if (formats == null) {
            return null;
        }

        List<OptionItem> options = new ArrayList<>();

        for (FormatItem format : formats) {
            final boolean isVerticalVideo = 1.0 * format.getWidth() / format.getHeight() <= 1.0;
            if (isVerticalVideo && format.getTitle() != null
                    && ((String) format.getTitle()).contains(TrackSelectorUtil.CODEC_SHORT_VP9)) {
                continue;
            }
            // fix crash on amlogic s905x
            if (isVerticalVideo && format.getHeight() >= 1080
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                continue;
            }
            final boolean isProperlyAspect = Math.abs(
                    (1.0 * format.getWidth()  / format.getHeight()) - 16 / 9.0) < 0.1;
            if (!isProperlyAspect && format.getWidth() > 1920) {
                continue;
            }
            if (format.getWidth() > MAX_VIDEO_WIDTH) {
                continue;
            }
            if (format.getFrameRate() > MAX_FRAME_RATE) {
                continue;
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT
                    && format.getTitle() != null
                    && ((String) format.getTitle()).contains(TrackSelectorUtil.CODEC_SHORT_VP9)) {
                continue;
            }
            if (format.getTitle() != null && ((String) format.getTitle()).contains("HDR")) {
                continue;
            }
            if (format.getTitle() != null
                    && ((String) format.getTitle()).contains(TrackSelectorUtil.CODEC_SHORT_AV1)) {
                continue;
            }
            options.add(from(format, callback, defaultTitle));
        }

        return options;
    }

    public static OptionItem from(FormatItem format, OptionCallback callback) {
        return from(format, callback, null);
    }

    public static OptionItem from(FormatItem format, OptionCallback callback, String defaultTitle) {
        if (format == null) {
            return null;
        }

        UiOptionItem uiOptionItem = new UiOptionItem();

        uiOptionItem.mTitle = format.isDefault() ? defaultTitle : format.getTitle();
        uiOptionItem.mIsSelected = format.isSelected();
        uiOptionItem.mFormat = format;
        uiOptionItem.mCallback = callback;

        return uiOptionItem;
    }

    public static OptionItem from(CharSequence title) {
        return from(title, (OptionCallback) null);
    }

    public static OptionItem from(CharSequence title, OptionCallback callback) {
        return from(title, callback, false);
    }

    public static OptionItem from(CharSequence title, OptionCallback callback, boolean isChecked) {
        return from(title, callback, isChecked, null);
    }

    public static OptionItem from(CharSequence title, CharSequence description, OptionCallback callback, boolean isChecked) {
        return from(title, description, callback, isChecked, null);
    }

    public static OptionItem from(CharSequence title, OptionCallback callback, boolean isChecked, Object data) {
        return from(title, null, callback, isChecked, data);
    }

    public static OptionItem from(CharSequence title, CharSequence description, OptionCallback callback, boolean isChecked, Object data) {
        UiOptionItem uiOptionItem = new UiOptionItem();

        uiOptionItem.mTitle = title;
        uiOptionItem.mDescription = description;
        uiOptionItem.mIsSelected = isChecked;
        uiOptionItem.mCallback = callback;
        uiOptionItem.mData = data;

        return uiOptionItem;
    }

    public static OptionItem from(CharSequence title, ChatReceiver chatReceiver) {
        UiOptionItem uiOptionItem = new UiOptionItem();
        uiOptionItem.mTitle = title;
        uiOptionItem.mChatReceiver = chatReceiver;

        return uiOptionItem;
    }

    public static OptionItem from(CharSequence title, CommentsReceiver commentsReceiver) {
        UiOptionItem uiOptionItem = new UiOptionItem();
        uiOptionItem.mTitle = title;
        uiOptionItem.mCommentsReceiver = commentsReceiver;

        return uiOptionItem;
    }

    public static FormatItem toFormat(OptionItem option) {
        if (option instanceof UiOptionItem) {
            return ((UiOptionItem) option).mFormat;
        }

        return null;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    @Override
    public CharSequence getDescription() {
        return mDescription;
    }

    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    @Override
    public void onSelect(boolean isSelected) {
        mIsSelected = isSelected;

        if (mCallback != null) {
            mCallback.onSelect(this);
        }
    }

    @Override
    public Object getData() {
        return mData;
    }

    @Override
    public void setRequired(OptionItem... items) {
        if (items == null || items.length == 0) {
            mRequiredItems = null;
        }

        mRequiredItems = items;
    }

    @Override
    public OptionItem[] getRequired() {
        return mRequiredItems;
    }

    @Override
    public void setRadio(OptionItem... items) {
        if (items == null || items.length == 0) {
            mRadioItems = null;
        }

        mRadioItems = items;
    }

    @Override
    public OptionItem[] getRadio() {
        return mRadioItems;
    }

    @Override
    public ChatReceiver getChatReceiver() {
        return mChatReceiver;
    }

    @Override
    public CommentsReceiver getCommentsReceiver() {
        return mCommentsReceiver;
    }
}
