package com.gee12.mytetroid.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.getAppVersionName
import com.gee12.mytetroid.ui.base.TetroidActivity

/**
 * Активность для просмотра информации о приложении.
 */
class AboutAppActivity : TetroidActivity<AboutAppViewModel>() {


    override fun getLayoutResourceId() = R.layout.activity_about_app

    override fun getViewModelClazz() = AboutAppViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<TextView>(R.id.text_view_version).also {
            it.text = this.getAppVersionName()
        }
        findViewById<Button>(R.id.button_rate_app).also {
            it.setOnClickListener {
                rateApp()
            }
        }
    }

    private fun rateApp() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (ex: ActivityNotFoundException) {
            logger.logError(ex, show = true)
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AboutAppActivity::class.java)
            context.startActivity(intent)
        }
    }

}