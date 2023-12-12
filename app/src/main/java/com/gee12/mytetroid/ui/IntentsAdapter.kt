package com.gee12.mytetroid.ui

import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.ReceivedData

class IntentsAdapter(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val dataSet: List<ReceivedData>,
) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): ReceivedData {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].stringResId.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: IntentsViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item_dialog, null)
            viewHolder = IntentsViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as IntentsViewHolder
        }
        viewHolder.bind(position)
        return view
    }

    override fun isEnabled(position: Int): Boolean {
        return dataSet[position].isCreateRecord
    }

    internal inner class IntentsViewHolder(itemView: View) {
        private var tvTitle: TextView
        private var tvDescription: TextView

        init {
            tvTitle = itemView.findViewById(R.id.text_view_title)
            tvDescription = itemView.findViewById(R.id.text_view_description)
        }

        fun bind(pos: Int) {
            val item = getItem(pos)
            val parts = item.splitTitles(resourcesProvider)

            // TODO: пока активны пункты только с созданием новых записей
            val isActive = item.isCreateRecord
            if (!isActive) {
                val isNotImplementText = resourcesProvider.getString(R.string.title_not_implemented_yet)
                tvTitle.text = SpannableString(isNotImplementText).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, isNotImplementText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(0.8f), 0, isNotImplementText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                tvTitle.append("\n")
                tvTitle.append(parts.first)
            } else {
                tvTitle.text = Html.fromHtml(parts.first)
            }
            tvDescription.text = parts.second

            val titleColorId = if (isActive) R.color.text_1 else R.color.text_3
            tvTitle.setTextColor(ContextCompat.getColor(context, titleColorId))

            val descriptionColorId = if (isActive) R.color.text_2 else R.color.text_3
            tvDescription.setTextColor(ContextCompat.getColor(context, descriptionColorId))
        }
    }

}
