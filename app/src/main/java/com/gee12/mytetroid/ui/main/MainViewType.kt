package com.gee12.mytetroid.ui.main

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class MainViewType(val index: Int) {
    NONE(0),
    NODE_RECORDS(1),
    RECORD_ATTACHES(2),
    TAG_RECORDS(3),
    FAVORITES(4);

    fun getSubtitle(resourcesProvider: IResourcesProvider, isMultiTagsMode: Boolean): String? {
        return when (this) {
            NONE -> null
            NODE_RECORDS -> resourcesProvider.getString(R.string.subtitle_main_page_node_records)
            RECORD_ATTACHES -> resourcesProvider.getString(R.string.subtitle_main_page_record_files)
            TAG_RECORDS -> {
                if (isMultiTagsMode) resourcesProvider.getString(R.string.subtitle_main_page_multiple_tags_records)
                else resourcesProvider.getString(R.string.subtitle_main_page_tag_records)
            }
            FAVORITES -> null
        }
    }

}