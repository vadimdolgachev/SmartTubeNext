package com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard;

class KeyBoardHelper {
    public static Alphabet getAlphabet(String type) {
        Alphabet alphabet = new Alphabet();
        switch (type) {
            case Alphabet.en_en:
                alphabet.setFirstRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
                alphabet.setTwoRow(new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "@"});
                alphabet.setThreeRow(new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l", "_", "&"});
                alphabet.setFourRow(new String[]{"z", "x", "c", "v", "b", "n", "m", "<", ">", "/", "."});
                break;

            case Alphabet.ru_ru:
                alphabet.setFirstRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
                alphabet.setTwoRow(new String[]{"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"});
                alphabet.setThreeRow(new String[]{"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"});
                alphabet.setFourRow(new String[]{"я", "ч", "с", "м", "и", "т", "ь", "б", "ю", ".", "?"});
                break;

            case Alphabet.num:
                alphabet.setFirstRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
                alphabet.setTwoRow(new String[]{"@", "#", "_", "&", "-", "+", "(", ")", "/", "*", "\""});
                alphabet.setThreeRow(new String[]{"'", ":", ";", "!", "?", ".", "=", "^", "$", "{", "}"});
                alphabet.setFourRow(new String[]{"~", "%", "[", "]", "<", ">", "^", "`", "|", "//", "/"});
                break;

        }
        return alphabet;
    }
}
