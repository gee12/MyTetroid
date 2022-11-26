package com.gee12.mytetroid.ui.dialogs

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.FoundType
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.R

object ClipboardDialogs {

    interface IDialogListResult {
        fun itemSelected(item: String?)
    }

    interface IDialogResult {
        fun insertHyperlink(uri: Uri?)
        fun insertAsText(uri: Uri?)
    }

    interface IDialogURLResult : IDialogResult {
        fun downloadAndInsertWebPage(uri: Uri?)
        fun downloadAndInsertWebPageAsText(uri: Uri?)
        fun downloadAndAttachWebPage(uri: Uri?)
    }

    interface IDialogFileURLResult : IDialogResult {
        fun attachLocalFile(uri: Uri?)
        fun downloadAndAttachWebFile(uri: Uri?)
    }

    interface IDialogImageURLResult : IDialogFileURLResult {
        fun insertLocalImage(uri: Uri?)
        fun downloadAndInsertWebImage(uri: Uri?)
    }

    interface IDialogTetroidObjectURLResult : IDialogResult {
        fun insertRecordContent(uri: Uri?, record: TetroidObject?)
        fun attachFile(uri: Uri?, attach: TetroidObject?)
    }

    fun createSimpleURLDialog(context: Context, uri: Uri?, callback: IDialogResult) {
        val dataSet = context.resources.getStringArray(R.array.clipboard_simple_url)
        createListDialog(context, dataSet, object : IDialogListResult {
            override fun itemSelected(item: String?) {
                when (item) {
                    context.getString(R.string.title_insert_hyperlink) -> {
                        callback.insertHyperlink(uri)
                    }
                    context.getString(R.string.title_insert_url_as_text) -> {
                        callback.insertAsText(uri)
                    }
                }
            }
        })
    }

    @JvmStatic
    fun createURLDialog(context: Context, uri: Uri?, callback: IDialogURLResult) {
        val dataSet = context.resources.getStringArray(R.array.clipboard_url)
        createListDialog(context, dataSet, object : IDialogListResult {
            override fun itemSelected(item: String?) {
                when (item) {
                    context.getString(R.string.title_insert_hyperlink) -> {
                        callback.insertHyperlink(uri)
                    }
                    context.getString(R.string.title_insert_url_as_text) -> {
                        callback.insertAsText(uri)
                    }
                    context.getString(R.string.title_attach_web_page_as_file) -> {
                        callback.downloadAndAttachWebPage(uri)
                    }
                    context.getString(R.string.title_insert_web_page) -> {
                        callback.downloadAndInsertWebPage(uri)
                    }
                    context.getString(R.string.title_insert_web_page_as_text) -> {
                        callback.downloadAndInsertWebPageAsText(uri)
                    }
                }
            }
        })
    }

    @JvmStatic
    fun createFileDialog(context: Context, uri: Uri?, isLocal: Boolean, callback: IDialogFileURLResult) {
        val dataSet = context.resources.getStringArray(
            if (isLocal) R.array.clipboard_local_file else R.array.clipboard_web_file
        )
        createListDialog(context, dataSet, object : IDialogListResult {
            override fun itemSelected(item: String?) {
                when (item) {
                    context.getString(R.string.title_insert_hyperlink) -> {
                        callback.insertHyperlink(uri)
                    }
                    context.getString(R.string.title_insert_url_as_text) -> {
                        callback.insertAsText(uri)
                    }
                    context.getString(R.string.title_attach_file) -> {
                        callback.attachLocalFile(uri)
                    }
                    context.getString(R.string.title_download_attach_file) -> {
                        callback.downloadAndAttachWebFile(uri)
                    }
                }
            }
        })
    }

    @JvmStatic
    fun createImageDialog(context: Context, uri: Uri?, isLocal: Boolean, callback: IDialogImageURLResult) {
        val dataSet = context.resources.getStringArray(
            if (isLocal) R.array.clipboard_local_image else R.array.clipboard_web_image
        )
        createListDialog(context, dataSet, object : IDialogListResult {
            override fun itemSelected(item: String?) {
                when (item) {
                    context.getString(R.string.title_insert_hyperlink) -> {
                        callback.insertHyperlink(uri)
                    }
                    context.getString(R.string.title_insert_url_as_text) -> {
                        callback.insertAsText(uri)
                    }
                    context.getString(R.string.title_insert_image) -> {
                        callback.insertLocalImage(uri)
                    }
                    context.getString(R.string.title_download_insert_image) -> {
                        callback.downloadAndInsertWebImage(uri)
                    }
                    context.getString(R.string.title_attach_image) -> {
                        callback.attachLocalFile(uri)
                    }
                    context.getString(R.string.title_download_attach_image) -> {
                        callback.downloadAndAttachWebFile(uri)
                    }
                }
            }
        })
    }

    @JvmStatic
    fun createTetroidObjectURLDialog(
        context: Context, uri: Uri?, obj: TetroidObject,
        callback: IDialogTetroidObjectURLResult
    ) {
        val dataSet = context.resources.getStringArray(
            if (obj.type == FoundType.TYPE_RECORD) R.array.clipboard_tetroid_record else if (obj.type == FoundType.TYPE_FILE) R.array.clipboard_tetroid_file else R.array.clipboard_simple_url
        )
        createListDialog(context, dataSet, object : IDialogListResult {
            override fun itemSelected(item: String?) {
                when (item) {
                    context.getString(R.string.title_insert_hyperlink) -> {
                        callback.insertHyperlink(uri)
                    }
                    context.getString(R.string.title_insert_url_as_text) -> {
                        callback.insertAsText(uri)
                    }
                    context.getString(R.string.title_insert_record_content) -> {
                        callback.insertRecordContent(uri, obj)
                    }
                    context.getString(R.string.title_attach_file) -> {
                        callback.attachFile(uri, obj)
                    }
                }
            }
        })
    }

    private fun createListDialog(context: Context, dataSet: Array<String>, callback: IDialogListResult?) {
        val builder = AskDialogBuilder.create(context, R.layout.dialog_list_view)
        builder.setTitle(R.string.title_insert_as)
        val dialog = builder.create()
        val listView = builder.view.findViewById<ListView>(R.id.list_view)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, dataSet)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val item = adapter.getItem(position)
            callback?.itemSelected(item)
            dialog.cancel()
        }
        listView.adapter = adapter
        dialog.show()
    }

}