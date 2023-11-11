package com.gee12.mytetroid.ui.logs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.base.BaseEvent

/**
 * Активность для просмотра логов.
 */
class LogsActivity : TetroidActivity<LogsViewModel>() {

    private lateinit var recycleView: RecyclerView
    private lateinit var layoutError: LinearLayout
    private lateinit var textAdapter: TextAdapter

    override fun getLayoutResourceId() = R.layout.activity_logs

    override fun getViewModelClazz() = LogsViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*// убираем перенос слов, замедляющий работу
        if (Build.VERSION.SDK_INT >= 23) {
            mTextViewLogs.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            mTextViewLogs.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }*/
        recycleView = findViewById(R.id.recycle_view)
        textAdapter = TextAdapter()
        recycleView.adapter = textAdapter
        layoutError = findViewById(R.id.layout_read_error)

        viewModel.loadLogs()
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            LogsEvent.LoadFromFile.InProcess,
            LogsEvent.LoadFromBuffer.InProcess -> {
                showProgress()
            }
            is LogsEvent.LoadFromFile.LogPathIsEmpty -> {
                showError(getString(R.string.error_log_file_path_is_null), isFromBuffer = false)
            }
            is LogsEvent.LoadFromFile.Failed-> {
                val errorText = event.failure.ex?.localizedMessage.orEmpty()
                val errorMessage = "%s%s\n\n%s".format(getString(R.string.log_file_read_error), event.logFullFileName, errorText)
                showError(errorMessage, isFromBuffer = false)
            }
            is LogsEvent.LoadFromBuffer.Failed -> {
                val errorMessage = event.failure.ex?.localizedMessage.orEmpty()
                showError(errorMessage, isFromBuffer = true)
            }
            is LogsEvent.LoadFromFile.Success -> {
                showLogs(event.data)
            }
            is LogsEvent.LoadFromBuffer.Success-> {
                showLogs(event.data)
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun showProgress() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        setProgressVisibility(true)
    }

    private fun showError(errorMessage: String, isFromBuffer: Boolean) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        setProgressVisibility(false)

        layoutError.isVisible = true
        (findViewById<View>(R.id.text_view_error) as TextView).also {
            it.isVisible = true
            it.text = errorMessage
        }
        (findViewById<View>(R.id.text_view_cur_logs)).isVisible = !isFromBuffer
        (findViewById<View>(R.id.button_show_cur_logs)).isVisible = !isFromBuffer
        (findViewById<View>(R.id.text_view_empty)).isVisible = false
        recycleView.isVisible = false

        findViewById<View>(R.id.button_show_cur_logs).setOnClickListener {
            // выводим логи текущего сеанса запуска приложения
            viewModel.loadLogsFromBuffer()
        }
    }

    private fun showLogs(data: List<String>) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        setProgressVisibility(false)

        textAdapter.setItems(data)
        layoutError.isVisible = false
        recycleView.isVisible = true

        scrollToBottom()

        (findViewById<View>(R.id.text_view_empty)).isVisible = data.isEmpty()
    }

    /**
     * Пролистывание в конец.
     */
    private fun scrollToBottom() {
        recycleView.postDelayed(
            { recycleView.scrollToPosition(textAdapter.itemCount - 1) },
            100
        )
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
            val intent = Intent(context, LogsActivity::class.java)
            context.startActivity(intent)
        }
    }

}