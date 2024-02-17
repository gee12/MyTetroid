package com.gee12.mytetroid.logs

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.Tense


enum class LogOper(private val tensesResArray: Int? = null) {
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
    MOVE(R.array.oper_move),
    REORDER(R.array.oper_reorder),
    SAVE(R.array.oper_save),
    ATTACH(R.array.oper_attach),
    ENCRYPT(R.array.oper_encrypt),
    DECRYPT(R.array.oper_decrypt),
    DROPCRYPT(R.array.oper_dropcrypt),
    REENCRYPT(R.array.oper_reencrypt),
    CHECK;

    fun getString(tense: Tense, resourcesProvider: IResourcesProvider): String {
        return tensesResArray?.takeIf { tense.id in 0..2 }?.let {
            resourcesProvider.getStringArray(tensesResArray)[tense.id]
        }.orEmpty()
    }

}