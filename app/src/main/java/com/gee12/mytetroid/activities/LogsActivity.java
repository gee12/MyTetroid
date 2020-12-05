package com.gee12.mytetroid.activities;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.TextAdapter;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.utils.FileUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Активность для просмотра логов.
 */
public class LogsActivity extends AppCompatActivity {

    public static final int LINES_IN_RECYCLER_VIEW_ITEM = 10;
    
    private RecyclerView mRecycleView;
    private LinearLayout mLayoutError;
    private TextAdapter mTextAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /*// убираем перенос слов, замедляющий работу
        if (Build.VERSION.SDK_INT >= 23) {
            mTextViewLogs.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            mTextViewLogs.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }*/
        this.mRecycleView = findViewById(R.id.recycle_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        this.mTextAdapter = new TextAdapter();
        mRecycleView.setAdapter(mTextAdapter);
        this.mLayoutError = findViewById(R.id.layout_read_error);

        if (SettingsManager.isWriteLogToFile(this)) {
            // читаем лог-файл
            LogManager.log(this, getString(R.string.log_open_log_file), ILogger.Types.INFO);
            new FileReadTask().execute(LogManager.getFullFileName());
        } else {
            // выводим логи текущего сеанса запуска приложения
            showBufferLogs();
        }
    }

    private void showBufferLogs() {
        String curLogs = LogManager.getBufferString();
        if (!TextUtils.isEmpty(curLogs)) {
            mLayoutError.setVisibility(View.GONE);
            mRecycleView.setVisibility(View.VISIBLE);
//            mTextAdapter.setItem(curLogs);
            try {
                // разбиваем весь поток строк логов на блоки
                List<String> logsBlocks = FileUtils.splitToBlocks(curLogs, LINES_IN_RECYCLER_VIEW_ITEM);
                mTextAdapter.setItems(logsBlocks);
            } catch (IOException e) {
                LogManager.log(this, R.string.log_error_logs_reading_from_memory, ILogger.Types.ERROR);
                mTextAdapter.setItem(curLogs);
            }
            scrollToBottom();
        } else {
            LogManager.log(this, getString(R.string.log_logs_is_missing), ILogger.Types.WARNING, Toast.LENGTH_SHORT);
        }
    }

    /**
     * Пролистывание в конец.
     */
    private void scrollToBottom() {
        mRecycleView.postDelayed(() -> mRecycleView.scrollToPosition(mTextAdapter.getItemCount() - 1), 100);
    }

    private void setProgresVisible(boolean isVis) {
        findViewById(R.id.progress_bar).setVisibility((isVis) ? View.VISIBLE : View.GONE);
    }


    /**
     * Задание, в котором выполняется чтение лог-файла.
     */
    private class FileReadTask extends AsyncTask<String, Void, FileReadTask.FileReadResult> {

        class FileReadResult {
//            PrecomputedTextCompat text;
            List<String> data;
            String text;
            boolean res;
//            FileReadResult(PrecomputedTextCompat text, boolean res) {
            FileReadResult(List<String> data, String text, boolean res) {
                this.data = data;
                this.text = text;
                this.res = res;
            }
        }

        @Override
        protected void onPreExecute() {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            setProgresVisible(true);

        }

        @Override
        protected FileReadResult doInBackground(String... strings) {
            String fullFileName = strings[0];
            List<String> data = null;
            String text = null;
            boolean res = false;
            try {
//                text = FileUtils.readTextFile(Uri.parse(fullFileName));
                data = FileUtils.readTextFile(Uri.parse(fullFileName), LINES_IN_RECYCLER_VIEW_ITEM);
                res = true;
            } catch (IOException ex) {
                // ошибка чтения
                text = ex.getLocalizedMessage();
            }
//            PrecomputedTextCompat.Params params = mTextViewLogs.getTextMetricsParamsCompat();
//            PrecomputedTextCompat precomputedText = PrecomputedTextCompat.create(text, params);
//            return new FileReadResult(precomputedText, res);
            return new FileReadResult(data, text, res);
        }

        @Override
        protected void onPostExecute(FileReadResult res) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            setProgresVisible(false);
            if (res.res) {
//                mTextViewLogs.setText(res.text);
                mTextAdapter.setItems(res.data);
            } else {
                String mes = String.format(Locale.getDefault(), "%s%s\n\n%s",
                        getString(R.string.log_file_read_error), LogManager.getFullFileName(), res.text);
                LogManager.log(LogsActivity.this, mes, ILogger.Types.ERROR, Toast.LENGTH_LONG);
                ((TextView) findViewById(R.id.text_view_error)).setText(mes);
                mLayoutError.setVisibility(View.VISIBLE);
                mRecycleView.setVisibility(View.GONE);
                // выводим логи текущего сеанса запуска приложения
                (findViewById(R.id.button_show_cur_logs)).setOnClickListener(v -> {
                    showBufferLogs();
                });
            }
            scrollToBottom();
        }
    }

}
