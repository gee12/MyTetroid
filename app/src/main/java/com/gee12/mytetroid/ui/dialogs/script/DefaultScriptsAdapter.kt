package com.gee12.mytetroid.ui.dialogs.script

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.DefaultScript

class DefaultScriptsAdapter(
    context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val data: List<DefaultScript>,
) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): DefaultScript {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return data[position].fileName.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: DefaultScriptsViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_dialog, null)
            viewHolder = DefaultScriptsViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as DefaultScriptsViewHolder
        }
        viewHolder.bind(position)
        return view
    }

    internal inner class DefaultScriptsViewHolder(itemView: View) {
        private var tvTitle: TextView
        private var tvDescription: TextView

        init {
            tvTitle = itemView.findViewById(R.id.text_view_title)
            tvDescription = itemView.findViewById(R.id.text_view_description)
        }

        fun bind(pos: Int) {
            val item = getItem(pos)

            tvTitle.text = item.getTitle(resourcesProvider)
            tvDescription.text = item.getDescription(resourcesProvider)
        }
    }

}
