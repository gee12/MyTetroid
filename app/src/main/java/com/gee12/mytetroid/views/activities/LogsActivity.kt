package com.gee12.mytetroid.views.activities

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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.views.adapters.TextAdapter
import com.gee12.mytetroid.viewmodels.LogsViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.R
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.viewmodels.LogsViewModel.*
import com.gee12.mytetroid.views.TetroidMessage
import java.io.IOException

/**
 * Активность для просмотра логов.
 */
class LogsActivity : AppCompatActivity() {

    private lateinit var recycleView: RecyclerView
    private lateinit var layoutError: LinearLayout
    private lateinit var textAdapter: TextAdapter

    private lateinit var viewModel: LogsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this, TetroidViewModelFactory(application))
            .get(LogsViewModel::class.java)

        /*// убираем перенос слов, замедляющий работу
        if (Build.VERSION.SDK_INT >= 23) {
            mTextViewLogs.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            mTextViewLogs.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }*/
        recycleView = findViewById(R.id.recycle_view)
        textAdapter = TextAdapter()
        recycleView.adapter = textAdapter
        layoutError = findViewById(R.id.layout_read_error)

        viewModel.event.observe(this, { (state, data) -> onEvent(state, data) })
        viewModel.messageObservable.observe(this, { TetroidMessage.show(this, it) })

        viewModel.load()

        viewModel.logDebug(getString(R.string.log_activity_opened_mask, javaClass.simpleName))
    }

    private fun onEvent(event: Event, data: Any?) {
        when (event) {
            Event.PreLoading -> {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                setProgresVisible(true)
            }

            Event.PostLoading -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                setProgresVisible(false)

                when (data) {
                    is FileReadResult.Success -> {
                        textAdapter.setItems(data.data)
                    }
                    is FileReadResult.Failure -> {
                        val mes = "%s%s\n\n%s".format(getString(R.string.log_file_read_error), viewModel.logger.fullFileName, data.text)
                        (findViewById<View>(R.id.text_view_error) as TextView).text = mes
                        layoutError.visibility = View.VISIBLE
                        recycleView.visibility = View.GONE

                        // выводим логи текущего сеанса запуска приложения
                        findViewById<View>(R.id.button_show_cur_logs).setOnClickListener {
                            showBufferLogs()
                        }
                    }
                }

                scrollToBottom()
            }

            Event.ShowBufferLogs -> {
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

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LogsActivity::class.java)
            context.startActivity(intent)
        }
    }
}