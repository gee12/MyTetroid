package com.gee12.mytetroid.ui.tag

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.TetroidTag

class SelectedTagsListAdapter(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val onCancelTag: (TetroidTag) -> Unit,
) : BaseAdapter() {

    private inner class TagsViewHolder {
        lateinit var nameCountView: TextView
        lateinit var cancelImageButton: ImageButton
    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var dataSet: MutableList<TetroidTag> = mutableListOf()

    fun reset() {
        dataSet = mutableListOf()
        notifyDataSetChanged()
    }

    fun setDataItems(data: List<TetroidTag>) {
        dataSet = ArrayList(data)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): TetroidTag {
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
            view = inflater.inflate(R.layout.list_item_selected_tag, null)
            viewHolder.nameCountView = view.findViewById(R.id.tag_view_name_count)
            viewHolder.cancelImageButton = view.findViewById(R.id.button_cancel_tag)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as TagsViewHolder
        }
        val tag = getItem(position)

        if (tag.isEmpty) {
            val emptyTag = resourcesProvider.getString(R.string.title_empty_tag)
            viewHolder.nameCountView.text = emptyTag
            viewHolder.nameCountView.setTextColor(ContextCompat.getColor(context, R.color.text_3))
        } else {
            viewHolder.nameCountView.text = tag.name
            viewHolder.nameCountView.setTextColor(ContextCompat.getColor(context, R.color.text_1))
        }

        viewHolder.cancelImageButton.setOnClickListener {
            onCancelTag(tag)
        }

        return view
    }

}