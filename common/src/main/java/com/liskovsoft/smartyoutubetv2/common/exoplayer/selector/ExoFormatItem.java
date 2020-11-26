package com.liskovsoft.smartyoutubetv2.common.exoplayer.selector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.MimeTypes;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.MediaTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExoFormatItem implements FormatItem {
    public static final int RESOLUTION_FHD = 3;
    public static final int RESOLUTION_HD = 2;
    public static final int RESOLUTION_SD = 1;
    public static final int RESOLUTION_LD = 0;
    public static final int FORMAT_AVC = 0;
    public static final int FORMAT_VP9 = 1;
    public static final int FORMAT_MP4A = 2;
    public static final int FPS_30 = 0;
    private int mType;
    private int mId;
    private CharSequence mTitle;
    private MediaTrack mTrack;
    private boolean mIsSelected;
    private boolean mIsDefault;
    private float mFrameRate;
    private int mWidth;
    private int mHeight;
    private String mCodecs;
    private String mFormatId;

    public static List<FormatItem> from(Set<MediaTrack> mediaTracks) {
        if (mediaTracks == null) {
            return null;
        }

        List<FormatItem> formats = new ArrayList<>();

        for (MediaTrack track : mediaTracks) {
            if (!track.isHidden) {
                formats.add(from(track));
            }
        }

        return formats;
    }

    public static FormatItem from(MediaTrack track) {
        if (track == null) {
            return null;
        }

        ExoFormatItem videoFormatItem = from(track.format);

        videoFormatItem.mType = track.rendererIndex;
        videoFormatItem.mIsSelected = track.isSelected;
        videoFormatItem.mTrack = track;

        return videoFormatItem;
    }

    public static MediaTrack toMediaTrack(FormatItem option) {
        if (option instanceof ExoFormatItem) {
            return ((ExoFormatItem) option).mTrack;
        }

        return null;
    }

    public static ExoFormatItem from(Format format) {
        ExoFormatItem formatItem = new ExoFormatItem();

        if (format != null) {
            formatItem.mTitle = TrackSelectorUtil.buildTrackNameShort(format);
            formatItem.mFrameRate = format.frameRate;
            formatItem.mWidth = format.width;
            formatItem.mHeight = format.height;
            formatItem.mCodecs = format.codecs;
            formatItem.mType = getType(format);

            if (format.id != null) {
                formatItem.mId = format.id.hashCode();
                formatItem.mFormatId = format.id;
            }
        } else {
            formatItem.mIsDefault = true; // fake auto track
        }

        return formatItem;
    }

    public static boolean isVideo(Format format) {
        return MimeTypes.isVideo(format.sampleMimeType);
    }

    private static int getType(Format format) {
        String sampleMimeType = format.sampleMimeType;

        return MimeTypes.isVideo(sampleMimeType) ? TYPE_VIDEO : MimeTypes.isAudio(sampleMimeType) ? TYPE_AUDIO : TYPE_SUBTITLE;
    }

    @NonNull
    @Override
    public String toString() {
        int rendererIndex = -1;
        String codecs = "";
        int width = -1;
        int height = -1;
        float frameRate = -1;
        String language = "";
        String id = "";

        if (mTrack != null && mTrack.format != null) {
            id = mTrack.format.id;
            rendererIndex = mTrack.rendererIndex;
            codecs = mTrack.format.codecs;
            width = mTrack.format.width;
            height = mTrack.format.height;
            frameRate = mTrack.format.frameRate;
            language = mTrack.format.language;
        }

        return String.format("%s,%s,%s,%s,%s,%s,%s,%s", mType, rendererIndex, id, codecs, width, height, frameRate, language);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ExoFormatItem) {
            ExoFormatItem formatItem = (ExoFormatItem) obj;

            return mType == formatItem.mType &&
                    mFrameRate == formatItem.mFrameRate &&
                    mWidth == formatItem.mWidth &&
                    mHeight == formatItem.mHeight &&
                    mCodecs != null && mCodecs.equals(formatItem.mCodecs);
        }

        return super.equals(obj);
    }

    public static FormatItem from(int type, int rendererIndex, String id, String codecs, int width, int height, float frameRate, String language) {
        MediaTrack mediaTrack = MediaTrack.forRendererIndex(rendererIndex);

        switch (type) {
            case TYPE_VIDEO:
                // Fake format. It's used in app internal comparison routine.
                mediaTrack.format = Format.createVideoSampleFormat(
                        id, null, codecs, -1, -1, width, height, frameRate, null, null);
                break;
            case TYPE_AUDIO:
                // Fake format. It's used in app internal comparison routine.
                mediaTrack.format = Format.createAudioSampleFormat(
                        id, null, codecs, -1, -1,0, 0, null, null, 0, null);
                break;
            case TYPE_SUBTITLE:
                // Fake format. It's used in app internal comparison routine.
                mediaTrack.format = Format.createTextSampleFormat(
                        id, null, -1, language);
                break;
        }

        return from(mediaTrack);
    }

    public static FormatItem from(String spec) {
        if (spec == null) {
            return null;
        }

        String[] split = spec.split(",");

        if (split.length != 8) {
            return null;
        }

        int type = Helpers.parseInt(split[0]);
        int rendererIndex = Helpers.parseInt(split[1]);
        String id = split[2];
        String codecs = split[3];
        int width = Helpers.parseInt(split[4]);
        int height = Helpers.parseInt(split[5]);
        float frameRate = Helpers.parseFloat(split[6]);
        String language = split[7];

        return from(type, rendererIndex, id, codecs, width, height, frameRate, language);
    }

    /**
     * "2560,1440,30,vp9"
     */
    public static FormatItem fromVideoPreset(String spec) {
        if (spec == null) {
            return null;
        }

        String[] split = spec.split(",");

        if (split.length != 4) {
            return null;
        }

        int width = Helpers.parseInt(split[0]);
        int height = Helpers.parseInt(split[1]);
        float frameRate = Helpers.parseFloat(split[2]);
        String codec = split[3];

        return from(TYPE_VIDEO, TrackSelectorManager.RENDERER_INDEX_VIDEO, null, codec, width, height, frameRate, null);
    }

    public static FormatItem fromConstants(int resolution, int format, int frameRate) {
        ExoFormatItem formatItem = new ExoFormatItem();
        MediaTrack mediaTrack = MediaTrack.forRendererIndex(TrackSelectorManager.RENDERER_INDEX_VIDEO);
        formatItem.mTrack = mediaTrack;
        formatItem.mType = TYPE_VIDEO;

        int width = -1;
        int height = -1;
        String codec = null;
        int fps = 30;

        switch (resolution) {
            case RESOLUTION_FHD:
                width = 1920;
                height = 1080;
                break;
            case RESOLUTION_HD:
                width = 1280;
                height = 720;
                break;
            case RESOLUTION_SD:
                width = 640;
                height = 360;
                break;
            case RESOLUTION_LD:
                width = 426;
                height = 240;
                break;
        }

        switch (format) {
            case FORMAT_AVC:
                codec = "avc";
                break;
        }

        switch (frameRate) {
            case FPS_30:
                fps = 30;
                break;
        }

        // Fake format. It's used in app internal comparison routine.
        mediaTrack.format = Format.createVideoSampleFormat(
                null, null, codec, -1, -1, width, height, fps, null, null);

        return formatItem;
    }

    public static FormatItem createFakeAudioFormat(int format) {
        ExoFormatItem formatItem = new ExoFormatItem();
        MediaTrack mediaTrack = MediaTrack.forRendererIndex(TrackSelectorManager.RENDERER_INDEX_AUDIO);
        formatItem.mTrack = mediaTrack;
        formatItem.mType = TYPE_AUDIO;

        String codec = null;

        switch (format) {
            case FORMAT_MP4A:
            default:
                codec = "mp4a";
                break;
        }

        // Fake format. It's used in app internal comparison routine.
        mediaTrack.format = Format.createAudioSampleFormat(
                null, null, codec, -1, -1,0, 0, null, null, 0, null);

        return formatItem;
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
    public boolean isSelected() {
        return mIsSelected;
    }

    @Override
    public boolean isDefault() {
        return mIsDefault;
    }

    @Override
    public float getFrameRate() {
        return mFrameRate;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getType() {
        return mType;
    }
}
