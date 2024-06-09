package com.gee12.mytetroid.ui.main.records

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.anggrayudi.storage.extension.launchOnUiThread
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.format
import com.gee12.mytetroid.domain.RecordFieldsSelector
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.obj.TetroidRecord
import java.util.*

abstract class RecordsBaseListAdapter(
    protected val context: Context,
    protected val resourcesProvider: IResourcesProvider,
    protected val buildInfoProvider: BuildInfoProvider,
    private val settingsManager: CommonSettingsManager,
    protected var dateTimeFormat: String = settingsManager.getDateFormatString(),
    protected var isHighlightAttach: Boolean = settingsManager.isHighlightRecordWithAttach(),
    protected var highlightAttachColor: Int = settingsManager.getHighlightColor(),
    protected var fieldsSelector: RecordFieldsSelector = settingsManager.getRecordFieldsSelector(),
    protected var isShowNodeName: Boolean = false,
    protected val getEditedDateCallback: suspend (record: TetroidRecord) -> Date?,
    protected val onClick: (record: TetroidRecord) -> Unit,
) : BaseAdapter() {

    var lastOpenedRecordId: String? = null

    open class RecordViewHolder {
        lateinit var lineNumView: TextView
        lateinit var iconView: ImageView
        lateinit var nameView: TextView
        lateinit var nodeNameView: TextView
        lateinit var authorView: TextView
        lateinit var tagsView: TextView
        lateinit var createdView: TextView
        lateinit var editedView: TextView
        lateinit var attachedView: ImageView
    }

    protected val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun notifyDataSetChanged() {
        dateTimeFormat = settingsManager.getDateFormatString()
        isHighlightAttach = settingsManager.isHighlightRecordWithAttach()
        highlightAttachColor = settingsManager.getHighlightColor()
        fieldsSelector = settingsManager.getRecordFieldsSelector()
        super.notifyDataSetChanged()
    }

    fun prepareView(position: Int, viewHolder: RecordViewHolder, convertView: View, record: TetroidRecord) {
        val nonCryptedOrDecrypted = record.isNonEncryptedOrDecrypted
        // иконка
        if (!nonCryptedOrDecrypted) {
            viewHolder.iconView.visibility = View.VISIBLE
            viewHolder.iconView.setImageResource(R.drawable.ic_node_encrypted)
        } else if (record.isFavorite) {
            viewHolder.iconView.visibility = View.VISIBLE
            viewHolder.iconView.setImageResource(R.drawable.ic_favorites_yellow)
        } else {
            viewHolder.iconView.visibility = View.GONE
        }
        // номер строки
        viewHolder.lineNumView.text = (position + 1).toString()
        // название
        val nameWhenEncrypted = resourcesProvider.getString(R.string.title_crypted_node_name)
        viewHolder.nameView.text = if (record.isNonEncryptedOrDecrypted) record.name else nameWhenEncrypted
        viewHolder.nameView.setTextColor(
            ContextCompat.getColor(
                context,
                if (nonCryptedOrDecrypted) R.color.text_1 else R.color.text_3
            )
        )
        // ветка
        val node = record.node
        if (isShowNodeName && nonCryptedOrDecrypted) {
            viewHolder.nodeNameView.visibility = View.VISIBLE
            viewHolder.nodeNameView.text = node.name
        } else {
            viewHolder.nodeNameView.visibility = View.GONE
        }
        // автор
        val author = record.author
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsAuthor() && !TextUtils.isEmpty(author)) {
            viewHolder.authorView.visibility = View.VISIBLE
            viewHolder.authorView.text = author
        } else {
            viewHolder.authorView.visibility = View.GONE
        }
        // метки
        val tags = record.tagsString
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsTags() && !TextUtils.isEmpty(tags)) {
            viewHolder.tagsView.visibility = View.VISIBLE
            viewHolder.tagsView.text = tags
        } else {
            viewHolder.tagsView.visibility = View.GONE
        }
        // дата создания
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsCreatedDate()) {
            viewHolder.createdView.visibility = View.VISIBLE
            viewHolder.createdView.text = record.created?.format(dateTimeFormat)
        } else {
            viewHolder.createdView.visibility = View.GONE
        }
        // дата изменения
        if (buildInfoProvider.isFullVersion()
            && nonCryptedOrDecrypted
            && fieldsSelector.checkIsEditedDate()
        ) {
            viewHolder.editedView.visibility = View.VISIBLE
            launchOnUiThread {
                val edited = getEditedDateCallback(record)?.format(dateTimeFormat) ?: "-"
                viewHolder.editedView.text = edited
            }
        } else {
            viewHolder.editedView.visibility = View.GONE
        }
        // выделение записи
        var backgroundColor = if (lastOpenedRecordId == record.id) {
            ContextCompat.getColor(context, R.color.background_last_opened_record)
        } else {
            null
        }
        // прикрепленные файлы
        val nameParams = viewHolder.nameView.layoutParams as RelativeLayout.LayoutParams
        when {
            record.attachedFilesCount > 0 && nonCryptedOrDecrypted -> {
                // если установлено в настройках и запись не была выделена, меняем фон
                if (isHighlightAttach && backgroundColor == null) {
                    backgroundColor = highlightAttachColor
                }
                viewHolder.attachedView.visibility = View.VISIBLE
                viewHolder.attachedView.setOnClickListener {
                    onClick(record)
                }
                nameParams.setMargins(0, 0, context.resources.getDimensionPixelOffset(R.dimen.record_attached_image_width), 0)
            }
            else -> {
                viewHolder.attachedView.visibility = View.GONE
                nameParams.setMargins(0, 0, 0, 0)
            }
        }
        convertView.setBackgroundColor(backgroundColor ?: Color.TRANSPARENT)
    }
}