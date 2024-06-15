package com.gee12.mytetroid.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.gee12.mytetroid.R

class DialogRadioListAdapter<T>(
    context: Context,
    selectedItem: T? = null,
    data: List<T>,
    private val onItemClickListener: ((T, View) -> Unit)? = null,
    private val getTitle: (T) -> String,
    private val getDescription: (T) -> String,
) : BaseDialogListAdapter<T>(
    data = data,
    selectedItem = selectedItem,
) {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_select_dialog, null)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            @Suppress("UNCHECKED_CAST")
            viewHolder = convertView.tag as DialogRadioListAdapter<T>.ViewHolder
        }
        viewHolder.bind(position)
        return view
    }

    internal inner class ViewHolder(
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
            tvTitle.text = getTitle(item)
            tvDescription.text = getDescription(item)
        }
    }

}
