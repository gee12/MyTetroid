package com.gee12.mytetroid.common.extensions

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.TetroidObject


fun TetroidObject.getIdString(resourcesProvider: IResourcesProvider): String {
    return resourcesProvider.getString(R.string.log_obj_id_mask, this.id)
}

fun TetroidObject.getIdNameString(resourcesProvider: IResourcesProvider): String {
    return resourcesProvider.getString(R.string.log_obj_id_name_mask, this.id, this.name)
}

fun IResourcesProvider.getStringFromTo(from: String, to: String): String {
    return getString(R.string.log_from_to_mask, from, to)
}

fun IResourcesProvider.getStringTo(to: String): String {
    return getString(R.string.log_to_mask, to)
}
