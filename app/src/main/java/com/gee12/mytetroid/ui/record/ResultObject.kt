package com.gee12.mytetroid.ui.record

sealed class ResultObject {

    var obj: Any? = null
    var needReloadText = false

    object None : ResultObject()
    data class Finish(val isOpenMainActivity: Boolean) : ResultObject()
    data class OpenRecord(val recordId: String) : ResultObject()
    data class OpenNode(val nodeId: String) : ResultObject()
    data class OpenTag(val tagName: String) : ResultObject()
    data class OpenAttaches(val recordId: String) : ResultObject()

}