package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidNode;

public interface ICryptHandler {
    boolean decryptNode(TetroidNode node);
    String encryptField(String field);
}
