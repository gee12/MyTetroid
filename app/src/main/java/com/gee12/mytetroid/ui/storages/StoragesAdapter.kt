package com.gee12.mytetroid.ui.storages

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.uriToAbsolutePath
import com.gee12.mytetroid.model.TetroidStorage

class StoragesAdapter(
    private val context: Context,
    private val currentStorageId: Int?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ItemViewType(val id: Int) {
        Basic(0),
        Footer(1)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    val data = mutableListOf<TetroidStorage>()
    var onItemClickListener: ((TetroidStorage, View) -> Unit)? = null
    var onItemLongClickListener: ((TetroidStorage, View) -> Boolean)? = null
    var onItemMenuClickListener: ((TetroidStorage, View) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<TetroidStorage>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemViewType.Footer.id -> {
                val view = inflater.inflate(R.layout.list_view_empty_footer, parent, false)
                FooterViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.list_item_storage, parent, false)
                StorageViewHolder(context, view, currentStorageId)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StorageViewHolder -> {
                getItem(position)?.also {
                    holder.bind(it)
                }
            }
            is FooterViewHolder -> Unit
        }
    }

    override fun getItemCount(): Int {
        val size = data.size
        return if (size == 0) 1 else size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            data.size -> {
                ItemViewType.Footer.id
            }
            else -> {
                ItemViewType.Basic.id
            }
        }
    }

    fun getItem(position: Int): TetroidStorage? {
        return data.getOrNull(position)
    }

    inner class StorageViewHolder internal constructor(
        private val context: Context,
        private val view: View,
        private val currentStorageId: Int?
    ) : RecyclerView.ViewHolder(view) {

        private val tvName: TextView = itemView.findViewById(R.id.text_view_name)
        private val tvPath: TextView = itemView.findViewById(R.id.text_view_path)
        private val ivError: ImageView = itemView.findViewById(R.id.image_view_error)
        private val tvError: TextView = itemView.findViewById(R.id.text_view_error)
        private val ivCurrent: ImageView = itemView.findViewById(R.id.image_view_current)
        private val ivDefault: ImageView = itemView.findViewById(R.id.image_view_default)
        private val ivMenu: ImageView = itemView.findViewById(R.id.image_view_menu)

        fun bind(storage: TetroidStorage) {
            view.setOnClickListener {
                onItemClickListener?.invoke(storage, view)
            }
            view.setOnLongClickListener {
                onItemLongClickListener?.invoke(storage, view) ?: false
            }
            ivMenu.setOnClickListener {
                onItemMenuClickListener?.invoke(storage, ivMenu)
            }

            tvName.text = storage.name
            tvPath.text = storage.uri.uriToAbsolutePath(context)
            ivCurrent.isVisible = storage.id == currentStorageId
            ivDefault.isVisible = storage.isDefault
            if (storage.error != null) {
                ivError.visibility = VISIBLE
                tvError.visibility = VISIBLE
                tvError.text = storage.error
            } else {
                ivError.visibility = GONE
                tvError.visibility = GONE
            }
        }
    }

    inner class FooterViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)

}