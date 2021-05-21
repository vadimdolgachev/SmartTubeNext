package com.liskovsoft.smartyoutubetv2.tv.ui.search.keyboard;

public class Alphabet {

    public static final String ru_ru = "ru_ru";
    public static final String en_en = "en_en";
    public static final String num = "num";

    String type;
    String[] firstRow;
    String[] twoRow;
    String[] threeRow;
    String[] fourRow;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getFirstRow() {
        return firstRow;
    }

    public void setFirstRow(String[] firstRow) {
        this.firstRow = firstRow;
    }

    public String[] getTwoRow() {
        return twoRow;
    }

    public void setTwoRow(String[] twoRow) {
        this.twoRow = twoRow;
    }

    public String[] getThreeRow() {
        return threeRow;
    }

    public void setThreeRow(String[] threeRow) {
        this.threeRow = threeRow;
    }

    public String[] getFourRow() {
        return fourRow;
    }

    public void setFourRow(String[] fourRow) {
        this.fourRow = fourRow;
    }
}
