package com.gee12.mytetroid.domain;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.common.utils.UriUtils;
import com.gee12.htmlwysiwygeditor.YoutubeHelper;

import java.net.URLConnection;

public class ClipboardHelper {

    public interface IClipboardResult {
        void pasteText(String text);
        void pasteHtml(String html);
        void pasteImage(Uri uri, boolean isLocal);
        void pasteAudio(Uri uri, boolean isLocal);
        void pasteVideo(Uri uri, boolean isLocal);
        void pasteFile(Uri uri, boolean isLocal);
        void pasteUri(Uri uri);
        void pasteTetroidObject(Uri uri, TetroidObject obj);
    }

    /**
     * Запись текста в буфер обмена.
     * @param context
     * @param label
     * @param text
     */
    public static void writeToClipboard(Context context, String label, String text, String html) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            clip = ClipData.newHtmlText(label, text, html);
        } else {
            clip = ClipData.newPlainText(label, text);
        }
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Получение текста из буфера обмена.
     * @param context
     * @param textOnly
     * @return
     */
    public static String readFromClipboard(Context context, boolean textOnly) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clipboard.hasPrimaryClip() && clip != null && clip.getItemCount() > 0) {
            ClipData.Item item = clip.getItemAt(0);
            if (item == null)
                return null;
            if (!textOnly) {
                if (item.getUri() != null) {
                    String mimeType = context.getContentResolver().getType(item.getUri());
                }
                String html;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    html = item.getHtmlText();
                } else {
                    html = item.coerceToHtmlText(context);
                }
                if (html != null) {
                    return html;
                }
            }
            if (item.getText() != null) {
//                return item.coerceToText(context).toString();
                return item.getText().toString();
            }
        }
        return null;
    }

    /**
     * Продвинутое получение текста из буфера обмена.
     * @param context
     * @param textOnly
     * @return
     */
    public static void readFromClipboard(Context context, boolean textOnly, IClipboardResult callback) {
        if (context == null || callback == null)
            return;
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clipboard.hasPrimaryClip() && clip != null && clip.getItemCount() > 0) {
            ClipData.Item item = clip.getItemAt(0);
            if (item == null)
                return;
            Uri uri = item.getUri();
            CharSequence text = item.getText();
            if (!textOnly) {
                // определяем тип объекта в буфере
                if (uri != null) {
                    readURL(uri, context, callback);
                } else {
                    // uri == null
                    String html;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        html = item.getHtmlText();
                    } else {
                        html = item.coerceToHtmlText(context);
                    }
                    if (html != null) {
                        callback.pasteHtml(html);
                    } else if (text != null) {
                        String textString = text.toString();
                        if (UriUtils.isValidURL(textString)) {
                            uri = Uri.parse(textString);
                            readURL(uri, context, callback);
                        } else {
//                            item.coerceToHtmlText(context);
                            callback.pasteText(textString);
                        }
                    }
                }
            } else {
                // textOnly == true, нужен только текст
                if (text != null) {
//                return item.coerceToText(context).toString();
                    callback.pasteText(text.toString());
                } else if (uri != null) {
                    callback.pasteText(uri.toString());
                } else {
                    String html;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        html = item.getHtmlText();
                    } else {
                        html = item.coerceToHtmlText(context);
                    }
                    if (html != null) {
                        callback.pasteText(html);
                    }
                }
            }
        }
    }

    protected static void readURL(Uri uri, Context context, IClipboardResult callback) {
        if (uri == null) {
            return;
        }
        String url = uri.toString().toLowerCase();

        if (url.startsWith(TetroidObject.MYTETRA_LINK_PREFIX)) {
            // объект хранилища
            TetroidObject obj = TetroidObject.parseUrl(url);
            callback.pasteTetroidObject(uri, obj);
        } else {
            // это web-ссылка? http или https
            boolean isNetworkURL = UriUtils.isNetworkURL(url);
            // пробуем определить тип по окончанию ссылки
            String mimeType = URLConnection.guessContentTypeFromName(url);
            if (mimeType != null) {
                if (mimeType.startsWith("text")) {
//                    callback.pasteUri(uri, !isNetworkURL);
                    if (isNetworkURL) {
                        // например, если URL заканчивается на "/"
                        callback.pasteUri(uri);
                    } else {
                        // текстовый файл, например, html
                        callback.pasteFile(uri, true);
                    }
                } else if (mimeType.startsWith("image")) {
                    // изображение
                    callback.pasteImage(uri, !isNetworkURL);
                } else if (mimeType.startsWith("audio")) {
                    // аудиофайл
                    callback.pasteAudio(uri, !isNetworkURL);
                } else if (mimeType.startsWith("video")) {
                    // видеофайл
                    callback.pasteVideo(uri, !isNetworkURL);
                } else if (YoutubeHelper.getVideoId(url) != null) {
                    // видео Youtube
                    callback.pasteVideo(uri, false);
                } else {
                    // тип определен, но он не входит в список самых распространенных,
                    //  тогда обрабатываем просто как файл
                    callback.pasteFile(uri, !isNetworkURL);
                }
            } else {
                // если тип не определен, то обрабатываем просто как ссылку
                callback.pasteUri(uri);
            }
        }
    }
}
