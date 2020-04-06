package com.gee12.mytetroid.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.utils.FileUtils;

import java.io.IOException;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView textViewLogs = findViewById(R.id.text_view_logs);
        String logFullFileName = null;
        try {
            // читаем лог-файл
            logFullFileName = LogManager.getFullFileName();
            String logsText = FileUtils.readTextFile(Uri.parse(logFullFileName));
            textViewLogs.setText(logsText);
        } catch (IOException ex) {
            // ошибка чтения
            String mes = getString(R.string.log_file_read_error) + logFullFileName;
            LogManager.addLog(mes, ex);
            String errorText = String.format(Locale.getDefault(), "%s\n\n%s", mes, ex.getLocalizedMessage());
            ((TextView)findViewById(R.id.text_view_error)).setText(errorText);
            LinearLayout errorLayout = findViewById(R.id.layout_read_error);
            errorLayout.setVisibility(View.VISIBLE);
            textViewLogs.setVisibility(View.GONE);
            (findViewById(R.id.button_show_cur_logs)).setOnClickListener(v -> {
                // выводим логи текущего сеанса запуска приложения
                String curLogs = LogManager.getBufferString();
                if (!TextUtils.isEmpty(curLogs)) {
                    errorLayout.setVisibility(View.GONE);
                    textViewLogs.setText(curLogs);
                    textViewLogs.setVisibility(View.VISIBLE);
                } else {
                    LogManager.addLog(getString(R.string.log_logs_is_missing), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                }
            });
        }
    }
}
