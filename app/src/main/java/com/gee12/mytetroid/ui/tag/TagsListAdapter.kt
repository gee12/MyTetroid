package com.gee12.mytetroid.ui.tag

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.SortHelper
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.TetroidTag
import java.util.*

class TagsListAdapter(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
) : BaseAdapter() {

    private inner class TagsViewHolder {
        lateinit var nameView: TextView
        lateinit var recordsCountView: TextView
    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var dataSet: MutableList<Map.Entry<String, TetroidTag>> = mutableListOf()

    fun reset() {
        dataSet = mutableListOf()
        notifyDataSetChanged()
    }

    fun setDataItems(data: Map<String, TetroidTag>, sortHelper: SortHelper?) {
        dataSet = ArrayList(data.entries)
        sort(sortHelper)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): Map.Entry<String, TetroidTag> {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: TagsViewHolder
        if (convertView == null) {
            viewHolder = TagsViewHolder()
            view = inflater.inflate(R.layout.list_item_tag, null)
            viewHolder.nameView = view.findViewById(R.id.tag_view_name)
            viewHolder.recordsCountView = view.findViewById(R.id.tag_view_records_count)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as TagsViewHolder
        }
        val (key, value) = getItem(position)
        if (value.isEmpty) {
            viewHolder.nameView.text = resourcesProvider.getString(R.string.title_empty_tag)
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorLightText))
            viewHolder.recordsCountView.setTextColor(ContextCompat.getColor(context, R.color.colorLightText))
        } else {
            viewHolder.nameView.text = key
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorBaseText))
            viewHolder.recordsCountView.setTextColor(ContextCompat.getColor(context, R.color.colorBase2Text))
        }
        // количество записей
        val records = value.records
        viewHolder.recordsCountView.text = String.format(Locale.getDefault(), "[%d]", records.size)
        return view
    }

    private fun sort(sortHelper: SortHelper?) {
        if (sortHelper == null) return
        sort(sortHelper.isByName, sortHelper.isAscent)
    }

    fun sort(byName: Boolean, isAscent: Boolean) {
        Collections.sort(dataSet, Comparator { obj1, obj2 ->
            if (obj1 === obj2) {
                0
            } else if (obj1 == null) {
                if (isAscent) 1 else -1
            } else if (obj2 == null) {
                if (isAscent) -1 else 1
            } else {
                var res: Int
                if (byName) {
                    res = obj1.key.compareTo(obj2.key)
                } else {
                    res = obj1.value.records.size.compareTo(obj2.value.records.size)
                    if (res == 0) {
                        res = obj1.key.compareTo(obj2.key)
                    }
                }
                if (isAscent) res else res * -1
            }
        })
        notifyDataSetChanged()
    }
}