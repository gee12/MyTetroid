package com.gee12.mytetroid.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.ui.adapters.TextAdapter
import com.gee12.mytetroid.viewmodels.LogsViewModel
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.viewmodels.LogsViewModel.*
import com.gee12.mytetroid.ui.TetroidMessage
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.IOException

/**
 * Активность для просмотра логов.
 */
class LogsActivity : AppCompatActivity() {

    private lateinit var recycleView: RecyclerView
    private lateinit var layoutError: LinearLayout
    private lateinit var textAdapter: TextAdapter

    private val viewModel: LogsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel.initialize()

        /*// убираем перенос слов, замедляющий работу
        if (Build.VERSION.SDK_INT >= 23) {
            mTextViewLogs.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            mTextViewLogs.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }*/
        recycleView = findViewById(R.id.recycle_view)
        textAdapter = TextAdapter()
        recycleView.adapter = textAdapter
        layoutError = findViewById(R.id.layout_read_error)

        lifecycleScope.launch {
            viewModel.logsEventFlow.collect { event -> onEvent(event) }
        }
        lifecycleScope.launch {
            viewModel.messageEventFlow.collect { showMessage(it) }
        }

        viewModel.load()

        viewModel.logDebug(getString(R.string.log_activity_opened_mask, javaClass.simpleName))
    }

    private fun onEvent(event: LogsEvent) {
        when (event) {
            LogsEvent.Loading.InProcess -> {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                setProgresVisible(true)
            }

            is LogsEvent.Loading.Success,
            is LogsEvent.Loading.Failed -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                setProgresVisible(false)

                when (event) {
                    is LogsEvent.Loading.Success -> {
                        textAdapter.setItems(event.data)
                    }
                    is LogsEvent.Loading.Failed -> {
                        val mes = "%s%s\n\n%s".format(getString(R.string.log_file_read_error), viewModel.logger.fullFileName, event.text)
                        (findViewById<View>(R.id.text_view_error) as TextView).text = mes
                        layoutError.visibility = View.VISIBLE
                        recycleView.visibility = View.GONE

                        // выводим логи текущего сеанса запуска приложения
                        findViewById<View>(R.id.button_show_cur_logs).setOnClickListener {
                            showBufferLogs()
                        }
                    }
                    else -> {}
                }

                scrollToBottom()
            }

            LogsEvent.ShowBufferLogs -> {
                showBufferLogs()
            }
        }
    }

    private fun showBufferLogs() {
        val curLogs = viewModel.getLogsBufferString()
        if (!TextUtils.isEmpty(curLogs)) {
            layoutError.visibility = View.GONE
            recycleView.visibility = View.VISIBLE

//            textAdapter.setItem(curLogs)
            try {
                // разбиваем весь поток строк логов на блоки
                // TODO: в IO поток ?
                val logsBlocks: List<String> = FileUtils.splitToBlocks(curLogs, LogsViewModel.LINES_IN_RECYCLER_VIEW_ITEM)
                textAdapter.setItems(logsBlocks)
            } catch (e: IOException) {
                viewModel.logError(R.string.log_error_logs_reading_from_memory)
                textAdapter.setItem(curLogs)
            }
            scrollToBottom()
        } else {
            viewModel.logWarning(getString(R.string.log_logs_is_missing))
        }
    }

    /**
     * Пролистывание в конец.
     */
    private fun scrollToBottom() {
        recycleView.postDelayed({ recycleView.scrollToPosition(textAdapter.itemCount - 1) }, 100)
    }

    private fun setProgresVisible(isVisible: Boolean) {
        findViewById<View>(R.id.progress_bar).visibility = if (isVisible) View.VISIBLE else View.GONE
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

    private fun showMessage(message: Message) {
        TetroidMessage.show(this, message)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LogsActivity::class.java)
            context.startActivity(intent)
        }
    }
}