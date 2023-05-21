package com.gee12.mytetroid.domain

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

interface ComponentWrapper {

    val context: Context

    val activity: Activity

    fun startActivityForResult(intent: Intent, requestCode: Int): Boolean
}

class ComponentActivityWrapper(
    override val activity: ComponentActivity,
    var activityResultCallback: ((Int, Uri) -> Unit)?,
) : ComponentWrapper {

    var requestCode = 0

    private val activityResultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                activityResultCallback?.invoke(requestCode, uri)
            }
        }
    }

    override val context: Context
        get() = activity

    override fun startActivityForResult(intent: Intent, requestCode: Int): Boolean {
        return try {
            activityResultLauncher.launch(intent)
            this.requestCode = requestCode
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}