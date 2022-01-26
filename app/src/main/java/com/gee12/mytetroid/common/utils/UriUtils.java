/*
 * Copyright 2016-2018 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.gee12.mytetroid.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.webkit.URLUtil;

import java.io.File;
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

    public static boolean isValidURL2(String url) {
        return URLUtil.isValidUrl(url);
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

    public static Uri parseDefaultFile(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }

        Uri uri = Uri.parse(text);
        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("file").authority("").build();
        }

        return uri;
    }
    public static Uri parseDefaultFile(Uri uri) {
        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("file").authority("").build();
        }

        return uri;
    }
//
//    public static boolean equalsDefaultfile(Uri left, String right) {
//        if (left == null || right == null) {
//            return false;
//        }
//
//        left = parseDefaultFile(left);
//        Uri uriRight = parseDefaultFile(right);
//
//        return left.equals(uriRight);
//    }
//
//    public static InputStream getUriInputStream(Context ctx, Uri uri) throws FileNotFoundException {
//        if (uri == null) return null;
//
//        String scheme = uri.getScheme();
//        if (TextUtils.isEmpty(scheme) || scheme.equals("file")) {
//            return new FileInputStream(uri.getPath());
//        }
//        else if (scheme.equals("content")) {
//            return ctx.getContentResolver().openInputStream(uri);
//        }
//        else {
//            return null;
//        }
//    }


    public static boolean supportsStorageFramework() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT; }

    public static boolean useStorageFramework(Context ctx) {
        if (!supportsStorageFramework()) { return false; }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
//        return prefs.getBoolean(ctx.sizeToString(R.string.saf_key), ctx.getResources().getBoolean(R.bool.saf_default));
        return true;
    }

    /**
     * Many android apps respond with non-writeable content URIs that correspond to files.
     * This will attempt to translate the content URIs to file URIs when possible/appropriate
     * @param uri
     * @return
     */
    public static Uri translate(Context ctx, Uri uri) {
        // StorageAF provides nice URIs
        if (useStorageFramework(ctx) || hasWritableContentUri(uri)) { return uri; }

        String scheme = uri.getScheme();
        if (TextUtils.isEmpty(scheme)) { return uri; }

        String filepath = null;

        try {
            // Use content resolver to try and find the file
            if (scheme.equalsIgnoreCase("content")) {
                Cursor cursor = ctx.getContentResolver().query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
                cursor.moveToFirst();

                if (cursor != null) {
                    filepath = cursor.getString(0);
                    cursor.close();


                    if (!isValidFilePath(filepath)) {
                        filepath = null;
                    }
                }
            }

            // Try using the URI path as a straight file
            if (TextUtils.isEmpty(filepath)) {
                filepath = uri.getEncodedPath();
                if (!isValidFilePath(filepath)) {
                    filepath = null;
                }
            }
        }
        // Fall back to URI if this fails.
        catch (Exception e) {
            filepath = null;
        }

        // Update the file to a file URI
        if (!TextUtils.isEmpty(filepath)) {
            Uri.Builder b = new Uri.Builder();
            uri = b.scheme("file").authority("").path(filepath).build();
        }

        return uri;
    }

    private static boolean isValidFilePath(String filepath) {
        if (TextUtils.isEmpty(filepath)) { return false; }

        File file = new File(filepath);
        return file.exists() && file.canRead();
    }

    /**
     * Whitelist for known content providers that support writing
     * @param uri
     * @return
     */
    private static boolean hasWritableContentUri(Uri uri) {
        String scheme = uri.getScheme();

        if (TextUtils.isEmpty(scheme)) { return false; }

        if (!scheme.equalsIgnoreCase("content")) { return false; }

        switch (uri.getAuthority()) {
            case "com.google.android.apps.docs.storage":
                return true;
        }

        return false;
    }

}
