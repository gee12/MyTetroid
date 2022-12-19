package com.gee12.mytetroid.ui.main

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.providers.IResourcesProvider
import com.gee12.mytetroid.model.TetroidRecord
import java.util.*
import kotlin.collections.ArrayList

class RecordsListAdapter(
    context: Context,
    resourcesProvider: IResourcesProvider,
    dateTimeFormat: String,
    getEditedDateCallback: (record: TetroidRecord) -> Date?,
    onClick: (record: TetroidRecord) -> Unit,
) : RecordsBaseListAdapter(
    context,
    resourcesProvider,
    dateTimeFormat,
    getEditedDateCallback,
    onClick
) {
    var dataSet: List<TetroidRecord>
        private set

    init {
        dataSet = ArrayList()
    }

    fun reset() {
        dataSet = ArrayList()
        notifyDataSetChanged()
    }

    fun setDataItems(dataSet: List<TetroidRecord>, viewId: Int) {
        this.dataSet = dataSet
        isShowNodeName = viewId in arrayOf(Constants.MAIN_VIEW_TAG_RECORDS, Constants.MAIN_VIEW_FAVORITES)
        notifyDataSetChanged()
    }

    fun delete(record: TetroidRecord) {
        (dataSet as MutableSet<*>).remove(record)
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
        var view = convertView
        val viewHolder: RecordViewHolder
        if (view == null) {
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
            viewHolder = view.tag as RecordViewHolder
        }
        prepareView(position, viewHolder, view!!, dataSet[position])

        return view
    }
}