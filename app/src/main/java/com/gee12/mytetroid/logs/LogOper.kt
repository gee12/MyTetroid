package com.gee12.mytetroid.logs

import com.gee12.mytetroid.R


enum class LogOper {
    NONE,
    SET(R.array.oper_set),
    LOAD(R.array.oper_load),
    CREATE(R.array.oper_create),
    ADD(R.array.oper_add),
    CHANGE(R.array.oper_change),
    RENAME(R.array.oper_rename),
    DELETE(R.array.oper_delete),
    COPY(R.array.oper_copy),
    CUT(R.array.oper_cut),
    INSERT(R.array.oper_insert),
    REORDER(R.array.oper_move),
    SAVE(R.array.oper_save),
    ATTACH(R.array.oper_attach),
    ENCRYPT(R.array.oper_encrypt),
    DECRYPT(R.array.oper_decrypt),
    DROPCRYPT(R.array.oper_dropcrypt),
    REENCRYPT(R.array.oper_reencrypt),
    CHECK;

    var maRes: Int

    constructor() {
        maRes = 0
    }

    constructor(arrayRes: Int) {
        maRes = arrayRes
    }

    fun getString(tense: Int, getStringArrayCallback: (arrayResId: Int) -> Array<String>): String {
        return if (maRes > 0 && tense >= 0 && tense < 3) getStringArrayCallback(maRes)[tense] else ""
    }
}