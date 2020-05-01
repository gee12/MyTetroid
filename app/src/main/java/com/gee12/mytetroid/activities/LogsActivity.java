package com.gee12.mytetroid.activities;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.PrecomputedTextCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.utils.FileUtils;

import java.io.IOException;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {

    private AppCompatTextView mTextViewLogs;
    private LinearLayout mLayoutError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.mTextViewLogs = findViewById(R.id.text_view_logs);
        this.mLayoutError = findViewById(R.id.layout_read_error);

        if (SettingsManager.isWriteLogToFile()) {
            // читаем лог-файл
            LogManager.addLog(getString(R.string.log_open_log_file), LogManager.Types.INFO);
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
            mTextViewLogs.setVisibility(View.VISIBLE);
            setText(curLogs);
            scrollToBottom();
        } else {
            LogManager.addLog(getString(R.string.log_logs_is_missing), LogManager.Types.WARNING, Toast.LENGTH_SHORT);
        }
    }

    private void setText(String text) {
        mTextViewLogs.setText(text);
    }

    /**
     * Пролистывание в конец.
     */
    private void scrollToBottom() {
        ScrollView scrollView = findViewById(R.id.scroll_view_logs);
        scrollView.postDelayed(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN), 100);
    }

    private void setProgresVisible(boolean isVis) {
        findViewById(R.id.progress_bar).setVisibility((isVis) ? View.VISIBLE : View.GONE);
    }


    /**
     * Задание, в котором выполняется чтение лог-файла.
     */
    private class FileReadTask extends AsyncTask<String, Void, FileReadTask.FileReadResult> {

        class FileReadResult {
            PrecomputedTextCompat text;
            boolean res;
            FileReadResult(PrecomputedTextCompat text, boolean res) {
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
            String text;
            boolean res = false;
            try {
                text = FileUtils.readTextFile(Uri.parse(fullFileName));
                res = true;
            } catch (IOException ex) {
                // ошибка чтения
                text = ex.getLocalizedMessage();
            }
            PrecomputedTextCompat.Params params = mTextViewLogs.getTextMetricsParamsCompat();
            PrecomputedTextCompat precomputedText = PrecomputedTextCompat.create(text, params);
            return new FileReadResult(precomputedText, res);
        }

        @Override
        protected void onPostExecute(FileReadResult res) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            setProgresVisible(false);
            if (res.res) {
                mTextViewLogs.setText(res.text);
            } else {
                String mes = String.format(Locale.getDefault(), "%s%s\n\n%s",
                        getString(R.string.log_file_read_error), LogManager.getFullFileName(), res.text);
                LogManager.addLog(mes, LogManager.Types.ERROR, Toast.LENGTH_LONG);
                ((TextView) findViewById(R.id.text_view_error)).setText(mes);
                mLayoutError.setVisibility(View.VISIBLE);
                mTextViewLogs.setVisibility(View.GONE);
                // выводим логи текущего сеанса запуска приложения
                (findViewById(R.id.button_show_cur_logs)).setOnClickListener(v -> {
                    showBufferLogs();
                });
            }
            scrollToBottom();

        }
    }

}
