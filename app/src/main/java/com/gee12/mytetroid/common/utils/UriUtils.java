package com.gee12.mytetroid.common.utils;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bpellin on 3/5/16.
 */
public class UriUtils {

    public static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
        return true;
    }

    public static boolean isNetworkURL(String url) {
        return URLUtil.isNetworkUrl(url);
    }

    public static String getFileName(Uri uri) {
        if (uri == null) {
            return null;
        }
        return getFileName(uri.toString());
//        return URLUtil.guessFileName(uri.toString(), null, null);
    }

    public static String getFileName(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        int lastSlashIndex = url.lastIndexOf('/') + 1;
        if (lastSlashIndex == url.length()) {
            // если URL заканчивается на "/", тогда берем строку между
            //  последним и предпоследним символом "/"
            String withoutLastSlash = url.substring(0, url.length() - 1);
            int preLastSlashIndex = withoutLastSlash.lastIndexOf('/') + 1;
            return withoutLastSlash.substring(preLastSlashIndex);
        } else {
            return url.substring(lastSlashIndex);
        }
    }

}
