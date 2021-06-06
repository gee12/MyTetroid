package com.gee12.mytetroid.views.dialogs;

import android.content.Context;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidObject;

import java.util.Objects;

public class ClipboardDialogs {

    public interface IDialogListResult {
        void itemSelected(String item);
    }

    public interface IDialogResult {
        void insertHyperlink(Uri uri);
        void insertAsText(Uri uri);
    }

    public interface IDialogURLResult extends IDialogResult {
        void downloadAndInsertWebPage(Uri uri);
        void downloadAndInsertWebPageAsText(Uri uri);
        void downloadAndAttachWebPage(Uri uri);
    }

    public interface IDialogFileURLResult extends IDialogResult {
        void attachLocalFile(Uri uri);
        void downloadAndAttachWebFile(Uri uri);
    }

    public interface IDialogImageURLResult extends IDialogFileURLResult {
        void insertLocalImage(Uri uri);
        void downloadAndInsertWebImage(Uri uri);
    }

    public interface IDialogTetroidObjectURLResult extends IDialogResult {
        void insertRecordContent(Uri uri, TetroidObject record);
        void attachFile(Uri uri, TetroidObject attach);
    }

    public static void createSimpleURLDialog(Context context, Uri uri, IDialogResult callback) {
        String[] dataSet = context.getResources().getStringArray(R.array.clipboard_simple_url);
        createListDialog(context, dataSet, item -> {
            if (Objects.equals(item, context.getString(R.string.title_insert_hyperlink))) {
                callback.insertHyperlink(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_url_as_text))) {
                callback.insertAsText(uri);
            }
        });
    }

    public static void createURLDialog(Context context, Uri uri, IDialogURLResult callback) {
        String[] dataSet = context.getResources().getStringArray(R.array.clipboard_url);
        createListDialog(context, dataSet, item -> {
            if (Objects.equals(item, context.getString(R.string.title_insert_hyperlink))) {
                callback.insertHyperlink(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_url_as_text))) {
                callback.insertAsText(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_attach_web_page_as_file))) {
                callback.downloadAndAttachWebPage(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_web_page))) {
                callback.downloadAndInsertWebPage(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_web_page_as_text))) {
                callback.downloadAndInsertWebPageAsText(uri);
            }
        });
    }

    public static void createFileDialog(Context context, Uri uri, boolean isLocal, IDialogFileURLResult callback) {
        String[] dataSet = context.getResources().getStringArray(
                (isLocal) ? R.array.clipboard_local_file : R.array.clipboard_web_file);
        createListDialog(context, dataSet, item -> {
            if (Objects.equals(item, context.getString(R.string.title_insert_hyperlink))) {
                callback.insertHyperlink(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_url_as_text))) {
                callback.insertAsText(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_attach_file))) {
                callback.attachLocalFile(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_download_attach_file))) {
                callback.downloadAndAttachWebFile(uri);
            }
        });
    }

    public static void createImageDialog(Context context, Uri uri, boolean isLocal, IDialogImageURLResult callback) {
        String[] dataSet = context.getResources().getStringArray(
                (isLocal) ? R.array.clipboard_local_image : R.array.clipboard_web_image);
        createListDialog(context, dataSet, item -> {
            if (Objects.equals(item, context.getString(R.string.title_insert_hyperlink))) {
                callback.insertHyperlink(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_url_as_text))) {
                callback.insertAsText(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_image))) {
                callback.insertLocalImage(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_download_insert_image))) {
                callback.downloadAndInsertWebImage(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_attach_image))) {
                callback.attachLocalFile(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_download_attach_image))) {
                callback.downloadAndAttachWebFile(uri);
            }
        });
    }

    public static void createTetroidObjectURLDialog(Context context, Uri uri, TetroidObject obj,
                                                    IDialogTetroidObjectURLResult callback) {
        String[] dataSet = context.getResources().getStringArray(
                (obj.getType() == FoundType.TYPE_RECORD) ? R.array.clipboard_tetroid_record
                        : (obj.getType() == FoundType.TYPE_FILE) ? R.array.clipboard_tetroid_file
                        : R.array.clipboard_simple_url);
        createListDialog(context, dataSet, item -> {
            if (Objects.equals(item, context.getString(R.string.title_insert_hyperlink))) {
                callback.insertHyperlink(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_url_as_text))) {
                callback.insertAsText(uri);
            } else if (Objects.equals(item, context.getString(R.string.title_insert_record_content))) {
                callback.insertRecordContent(uri, obj);
            } else if (Objects.equals(item, context.getString(R.string.title_attach_file))) {
                callback.attachFile(uri, obj);
            }
        });
    }

    private static void createListDialog(Context context, String[] dataSet, IDialogListResult callback) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_list_view);
        builder.setTitle(R.string.title_insert_as);
        final AlertDialog dialog = builder.create();

        ListView listView = builder.getView().findViewById(R.id.list_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, dataSet);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String item = adapter.getItem(position);
            if (callback != null)
                callback.itemSelected(item);
            dialog.cancel();
        });
        listView.setAdapter(adapter);
        dialog.show();
    }
}
