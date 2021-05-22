package com.gee12.mytetroid.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.net.URL;

public class NetworkHelper {

    public interface IWebPageContentResult {
        void onSuccess(String content, boolean isTextOnly);
        void onError(Exception ex);
    }

    public interface IWebImageResult {
        void onSuccess(Bitmap bitmap);
        void onError(Exception ex);
    }

    public static void downloadWebPageContentAsync(String url, boolean isTextOnly, IWebPageContentResult callback) {
        new Thread(() -> {
            try {
                Connection conn = Jsoup.connect(url);
                Document doc = conn.get();
                String content = "";
                Element body = doc.body();
                if (body != null) {
                    if (isTextOnly) {
                        content = HtmlHelper.elementToText(body);
                    } else {
                        content = body.html();
                    }
                }
                if (callback != null)
                    callback.onSuccess(content, isTextOnly);
            } catch (Exception ex) {
                if (callback != null)
                    callback.onError(ex);
            }
        }).start();
    }

    public static void downloadImageAsync(String url, IWebImageResult callback) {
        new Thread(() -> {
            try {
                InputStream input = new URL(url).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (callback != null)
                    callback.onSuccess(bitmap);
            } catch (Exception ex) {
                if (callback != null)
                    callback.onError(ex);
            }
        }).start();
    }
}
