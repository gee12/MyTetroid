package com.gee12.mytetroid.model;

import android.content.Context;

import com.gee12.mytetroid.R;

public class ReceivedData {
    private boolean isCreate;
    private boolean isAttach;
    private int stringId;

    public ReceivedData(boolean isCreate, boolean isAttach, int stringId) {
        this.isCreate = isCreate;
        this.isAttach = isAttach;
        this.stringId = stringId;
    }

    public boolean isCreate() {
        return isCreate;
    }

    public boolean isAttach() {
        return isAttach;
    }

    public int getStringId() {
        return stringId;
    }

    public static ReceivedData[] textIntents() {
        return new ReceivedData[] {
            new ReceivedData(true, false, R.string.text_intent_create_text),
            new ReceivedData(false, false, R.string.text_intent_exist_text)
        };
    }

    public static ReceivedData[] imageIntents() {
        return new ReceivedData[] {
                new ReceivedData(true, false, R.string.text_intent_create_image),
                new ReceivedData(true, true, R.string.text_intent_create_image_attach),
                new ReceivedData(false, false, R.string.text_intent_exist_image),
                new ReceivedData(false, true, R.string.text_intent_exist_image_attach),
        };
    }

    public String[] splitTitles(Context context) {
        return context.getString(stringId).split(";");
    }
}
