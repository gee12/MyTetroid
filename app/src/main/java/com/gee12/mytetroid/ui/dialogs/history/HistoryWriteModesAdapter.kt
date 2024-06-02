package com.gee12.mytetroid.ui.dialogs.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.HistoryWriteMode
import com.gee12.mytetroid.ui.dialogs.BaseDialogListAdapter

class HistoryWriteModesAdapter(
    context: Context,
    private val resourcesProvider: IResourcesProvider,
    selectedItem: HistoryWriteMode? = null,
    data: List<HistoryWriteMode>,
    private val onItemClickListener: ((HistoryWriteMode, View) -> Unit)? = null,
) : BaseDialogListAdapter<HistoryWriteMode>(
    data = data,
    selectedItem = selectedItem,
) {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: HistoryWriteModesViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_select_dialog, null)
            viewHolder = HistoryWriteModesViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as HistoryWriteModesViewHolder
        }
        viewHolder.bind(position)
        return view
    }

    internal inner class HistoryWriteModesViewHolder(
        private val itemView: View,
    ) {
        private var rbSelect: RadioButton
        private var tvTitle: TextView
        private var tvDescription: TextView

        init {
            rbSelect = itemView.findViewById(R.id.radio_button_select)
            tvTitle = itemView.findViewById(R.id.text_view_title)
            tvDescription = itemView.findViewById(R.id.text_view_description)
        }

        fun bind(pos: Int) {
            val item = getItem(pos)

            val listener = View.OnClickListener {
                selectedItem = item
                onItemClickListener?.invoke(item, itemView)
                notifyDataSetInvalidated()
            }
            itemView.setOnClickListener(listener)
            rbSelect.setOnClickListener(listener)

            rbSelect.isChecked = item == selectedItem
            tvTitle.text = item.getTitle(resourcesProvider)
            tvDescription.text = item.getDescription(resourcesProvider)
        }
    }

}
