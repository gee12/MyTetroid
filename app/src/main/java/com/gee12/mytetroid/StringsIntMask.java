package com.gee12.mytetroid;

import java.util.Collection;

public class StringsIntMask {
    protected int mMask;

    public StringsIntMask() {
        this.mMask = 0;
    }

    public StringsIntMask(Collection<String> strings, String[] fullArray) {
        this.mMask = 0;
        if (strings == null || fullArray == null)
            return;
        for (int i = 0; i < fullArray.length; i++) {
            if (strings.contains(fullArray[i])) {
                addValue(i);
            }
        }
    }

    public void addValue(int valueIndex) {
        this.mMask |= (1 << valueIndex);
    }

    public boolean checkValue(int valueIndex) {
        return ((mMask & (1 << valueIndex)) > 0);
    }

    /**
     *
     * @param fullArray
     * @param skip Количество элементов, которые нужно пропустить
     * @return
     */
    public String joinToString(String[] fullArray, int skip) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < fullArray.length; i++)
            if (checkValue(i)) {
                int arIndex = i - skip;
                if (arIndex >= 0 && arIndex < fullArray.length) {
                    if (!isFirst)
                        sb.append(", ");
                    sb.append(fullArray[arIndex]);
                }
                isFirst = false;
            }
        return sb.toString();
    }
}
