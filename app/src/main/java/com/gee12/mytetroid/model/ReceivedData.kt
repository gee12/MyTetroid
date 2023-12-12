package com.gee12.mytetroid.model

import androidx.annotation.StringRes
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

class ReceivedData(
    val isCreateRecord: Boolean,
    val isAttach: Boolean,
    @StringRes val stringResId: Int,
) {

    fun splitTitles(resourcesProvider: IResourcesProvider): Pair<String, String> {
        return resourcesProvider.getString(stringResId).split(";").let { parts ->
            if (parts.size >= 2) {
                parts[0] to parts[1]
            } else {
                "" to ""
            }
        }
    }

    companion object {
        fun textIntents(): List<ReceivedData> {
            return listOf(
                ReceivedData(true, false, R.string.text_intent_create_text),
                ReceivedData(false, false, R.string.text_intent_exist_text)
            )
        }

        fun imageIntents(): List<ReceivedData> {
            return listOf(
                ReceivedData(true, false, R.string.text_intent_create_image),
                ReceivedData(true, true, R.string.text_intent_create_image_attach),
                ReceivedData(false, false, R.string.text_intent_exist_image),
                ReceivedData(false, true, R.string.text_intent_exist_image_attach)
            )
        }
    }
}
