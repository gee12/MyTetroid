package com.gee12.mytetroid.data;

import android.content.Context;

import com.gee12.mytetroid.model.TetroidNode;

public interface INodeIconLoader {
    void loadIcon(Context context, TetroidNode node);
}
