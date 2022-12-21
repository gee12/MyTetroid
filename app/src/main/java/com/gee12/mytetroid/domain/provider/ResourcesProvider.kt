package com.gee12.mytetroid.domain.provider

import android.content.Context

interface IResourcesProvider {
    fun getString(id: Int, vararg args: Any): String
    fun getStringArray(id: Int): Array<String>
}

class ResourcesProvider(
    private val context: Context,
) : IResourcesProvider {

    override fun getString(id: Int, vararg args: Any): String {
        return context.getString(id, *args)
    }


    override fun getStringArray(id: Int): Array<String> {
        return context.resources.getStringArray(id)
    }

}