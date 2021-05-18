package com.liskovsoft.smartyoutubetv2.common.app.presenters.settings;

import android.content.Context;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.managers.PlayerUiManager;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.misc.LangUpdater;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class SubtitleSettingsPresenter extends BasePresenter<Void> {
    private final PlayerData mPlayerData;

    public SubtitleSettingsPresenter(Context context) {
        super(context);
        mPlayerData = PlayerData.instance(context);
    }

    public static SubtitleSettingsPresenter instance(Context context) {
        return new SubtitleSettingsPresenter(context);
    }

    public void show() {
        AppSettingsPresenter settingsPresenter = AppSettingsPresenter.instance(getContext());
        settingsPresenter.clear();

        appendSubtitleLanguageCategory(settingsPresenter);
        appendSubtitleStyleCategory(settingsPresenter);

        settingsPresenter.showDialog(getContext().getString(R.string.subtitle_category_title));
    }

    private void appendSubtitleLanguageCategory(AppSettingsPresenter settingsPresenter) {
        String subtitleLanguageTitle = getContext().getString(R.string.subtitle_language);
        String subtitlesDisabled = getContext().getString(R.string.subtitles_disabled);

        LangUpdater langUpdater = new LangUpdater(getContext());
        HashMap<String, String> locales = langUpdater.getSupportedLocales();
        FormatItem currentFormat = mPlayerData.getFormat(FormatItem.TYPE_SUBTITLE);

        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(
                subtitlesDisabled, option -> mPlayerData.setFormat(FormatItem.fromLanguage(null)),
                currentFormat == null || currentFormat.equals(FormatItem.fromLanguage(null))));

        for (Entry<String, String> entry : locales.entrySet()) {
            if (entry.getValue().isEmpty()) {
                // Remove default language entry
                continue;
            }

            options.add(UiOptionItem.from(
                    entry.getKey(), option -> mPlayerData.setFormat(FormatItem.fromLanguage(entry.getValue())),
                    FormatItem.fromLanguage(entry.getValue()).equals(currentFormat)));
        }
        
        settingsPresenter.appendRadioCategory(subtitleLanguageTitle, options);
    }

    private void appendSubtitleStyleCategory(AppSettingsPresenter settingsPresenter) {
        OptionCategory category = PlayerUiManager.createSubtitleStylesCategory(getContext(), mPlayerData);
        settingsPresenter.appendRadioCategory(category.title, category.options);
    }
}
