package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class DefaultScript(val fileName: String) {
    TableSort("table_sort.js"),
    TableTotals("table_totals.js"),
    LinkUrl("link_url_tooltip.js");

    fun getTitle(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                TableSort -> R.string.title_def_script_title_table_sort
                TableTotals -> R.string.title_def_script_title_table_totals
                LinkUrl -> R.string.title_def_script_title_link_url
            }
        )
    }

    fun getDescription(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                TableSort -> R.string.title_def_script_description_table_sort
                TableTotals -> R.string.title_def_script_description_table_totals
                LinkUrl -> R.string.title_def_script_description_link_url
            }
        )
    }

}