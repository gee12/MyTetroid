package com.gee12.mytetroid.ui.history

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.HistoryEntity
import com.gee12.mytetroid.model.enums.TetroidObjectType

class HistoryAdapter(
    context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val dateTimeFormat: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ItemViewType(val id: Int) {
        HistoryItem(0),
        Footer(1)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    val data = mutableListOf<Any>()

    var onItemClickListener: ((HistoryEntity, View) -> Unit)? = null
    var onItemLongClickListener: ((HistoryEntity, View) -> Boolean)? = null
    var onItemMenuClickListener: ((HistoryEntity, View) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<HistoryEntity>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemViewType.HistoryItem.id -> {
                val view = inflater.inflate(R.layout.list_item_history, parent, false)
                HistoryViewHolder(view)
            }
            ItemViewType.Footer.id -> {
                val view = inflater.inflate(R.layout.recycler_view_empty_footer, parent, false)
                FooterViewHolder(view)
            }
            else -> throw Exception("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HistoryViewHolder -> {
                (getItem(position) as? HistoryEntity)?.also {
                    holder.bind(it)
                }
            }
            is FooterViewHolder -> Unit
        }
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            data.size -> {
                ItemViewType.Footer.id
            }
            else -> {
                ItemViewType.HistoryItem.id
            }
        }
    }

    fun getItem(position: Int): Any? {
        return data.getOrNull(position)
    }

    inner class HistoryViewHolder internal constructor(
        private val view: View,
    ) : RecyclerView.ViewHolder(view) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.image_view_icon)
        private val tvName: TextView = itemView.findViewById(R.id.text_view_name)
        private val tvType: TextView = itemView.findViewById(R.id.text_view_type)
        private val ivMenu: ImageView = itemView.findViewById(R.id.image_view_menu)
        private val tvCreated: TextView = itemView.findViewById(R.id.text_view_created)

        fun bind(item: HistoryEntity) {
            view.setOnClickListener {
                onItemClickListener?.invoke(item, view)
            }
            view.setOnLongClickListener {
                onItemLongClickListener?.invoke(item, view) ?: false
            }
            ivMenu.setOnClickListener {
                onItemMenuClickListener?.invoke(item, ivMenu)
            }

            ivIcon.setImageResource(item.type.iconResId())

            tvName.text = item.obj.name
            tvType.text = item.type.getString(resourcesProvider).uppercase()
            tvCreated.text = item.createdDate?.let { Utils.dateToString(it, dateTimeFormat) }
        }

        private fun TetroidObjectType.iconResId(): Int {
            return when (this) {
                TetroidObjectType.NONE -> R.drawable.ic_history
                TetroidObjectType.RECORD -> R.drawable.ic_record
                TetroidObjectType.NODE -> R.drawable.ic_tree
                TetroidObjectType.ATTACH -> R.drawable.ic_attachment
                TetroidObjectType.TAG -> R.drawable.ic_tag_2
            }
        }
    }

    inner class FooterViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)

}