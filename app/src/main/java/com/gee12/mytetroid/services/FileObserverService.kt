package com.gee12.mytetroid.services

import android.app.Service
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.FileObserver
import android.os.IBinder
import com.gee12.mytetroid.TetroidFileObserver
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class FileObserverService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!intent.hasExtra(EXTRA_ACTION_ID)) {
            return super.onStartCommand(intent, flags, startId)
        }
        when (intent.getIntExtra(EXTRA_ACTION_ID, 0)) {
            ACTION_ID_START -> if (intent.hasExtra(EXTRA_FILE_PATH)) {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH).orEmpty()
                val mask = intent.getIntExtra(EXTRA_EVENT_MASK, FileObserver.ALL_EVENTS)

                fileObserver?.stop()
                fileObserver = null

                fileObserver = TetroidFileObserver(filePath, mask) { event ->
                    callback(event)
                }
                fileObserver?.start()
            }
            ACTION_ID_STOP -> {
                fileObserver?.stop()
            }
            ACTION_ID_RESTART -> {
                fileObserver?.restart()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun callback(event: TetroidFileObserver.Event) {
        val intent = Intent(ACTION_OBSERVER_EVENT_COME).apply {
            putExtra(EXTRA_EVENT_ID, event.id)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        const val EXTRA_ACTION_ID = "EXTRA_ACTION_ID"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        const val EXTRA_EVENT_MASK = "EXTRA_EVENT_MASK"
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"

        const val ACTION_ID_START = 1
        const val ACTION_ID_STOP = 2
        const val ACTION_ID_RESTART = 3
        const val ACTION_OBSERVER_EVENT_COME = "com.gee12.mytetroid.ACTION_OBSERVER_EVENT_COME"

        private var fileObserver: TetroidFileObserver? = null


        fun sendCommand(context: ContextWrapper, actionId: Int) {
            val bundle = Bundle().apply {
                putInt(EXTRA_ACTION_ID, actionId)
            }
            sendCommand(context, bundle)
        }

        fun sendCommand(context: ContextWrapper, bundle: Bundle) {
            val intent = Intent(context, FileObserverService::class.java).apply {
                putExtras(bundle)
            }
            context.startService(intent)
        }

        fun stop(context: ContextWrapper) {
            val intent = Intent(context, FileObserverService::class.java)
            context.stopService(intent)
        }
    }
}