package com.gee12.mytetroid.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.utils.Utils;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tvVersion = findViewById(R.id.text_view_version);
        tvVersion.setText(Utils.getVersionName(this));

        TextView tvappSumm = findViewById(R.id.text_view_app_summ);
        tvappSumm.setText(Html.fromHtml(getString(R.string.app_summ_html)));
        tvappSumm.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvUrl = findViewById(R.id.text_view_url);
        tvUrl.setText(Html.fromHtml(getString(R.string.project_url)));
        tvUrl.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvPolicy = findViewById(R.id.text_view_policy);
        tvPolicy.setText(Html.fromHtml(getString(R.string.policy_link)));
        tvPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        Button bRateApp = findViewById(R.id.button_rate_app);
        bRateApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateApp();
            }
        });
    }

    void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            LogManager.log(e);
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
}
