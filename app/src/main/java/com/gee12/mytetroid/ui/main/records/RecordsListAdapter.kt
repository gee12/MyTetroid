package com.gee12.mytetroid.ui.main.records

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.RecordFieldsSelector
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.main.MainViewType
import java.util.*

class RecordsListAdapter(
    context: Context,
    resourcesProvider: IResourcesProvider,
    dateTimeFormat: String,
    isHighlightAttach: Boolean,
    highlightAttachColor: Int,
    fieldsSelector: RecordFieldsSelector,
    getEditedDateCallback: suspend (record: TetroidRecord) -> Date?,
    onClick: (record: TetroidRecord) -> Unit,
) : RecordsBaseListAdapter(
    context = context,
    resourcesProvider = resourcesProvider,
    dateTimeFormat = dateTimeFormat,
    isHighlightAttach = isHighlightAttach,
    highlightAttachColor = highlightAttachColor,
    fieldsSelector = fieldsSelector,
    getEditedDateCallback = getEditedDateCallback,
    onClick = onClick,
) {
    private var dataSet: List<TetroidRecord> = emptyList()

    fun reset() {
        dataSet = emptyList()
        notifyDataSetChanged()
    }

    fun setDataItems(dataSet: List<TetroidRecord>, viewType: MainViewType) {
        this.dataSet = dataSet.toList()
        isShowNodeName = viewType in arrayOf(MainViewType.TAG_RECORDS, MainViewType.FAVORITES)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): Any {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: RecordViewHolder
        if (convertView == null) {
            viewHolder = RecordViewHolder()
            view = inflater.inflate(R.layout.list_item_record, null)
            viewHolder.lineNumView = view.findViewById(R.id.record_view_line_num)
            viewHolder.iconView = view.findViewById(R.id.record_view_icon)
            viewHolder.nameView = view.findViewById(R.id.record_view_name)
            viewHolder.nodeNameView = view.findViewById(R.id.record_view_node)
            viewHolder.authorView = view.findViewById(R.id.record_view_author)
            viewHolder.tagsView = view.findViewById(R.id.record_view_tags)
            viewHolder.createdView = view.findViewById(R.id.record_view_created)
            viewHolder.editedView = view.findViewById(R.id.record_view_edited)
            viewHolder.attachedView = view.findViewById(R.id.record_view_attached)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as RecordViewHolder
        }
        prepareView(position, viewHolder, view, dataSet[position])

        return view
    }
}