package com.gee12.mytetroid.common.extensions

import com.gee12.mytetroid.R
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.model.TetroidObject


fun TetroidObject.getIdString(resourcesProvider: IResourcesProvider): String {
    return resourcesProvider.getString(R.string.log_obj_id_mask, this.id)
}

fun TetroidObject.getIdNameString(resourcesProvider: IResourcesProvider): String {
    return resourcesProvider.getString(R.string.log_obj_id_name_mask, this.id, this.name)
}