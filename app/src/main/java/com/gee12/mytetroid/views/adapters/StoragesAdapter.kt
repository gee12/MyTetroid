package com.gee12.mytetroid.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.views.adapters.StoragesAdapter.StorageViewHolder
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.gee12.mytetroid.R

class StoragesAdapter(
    context: Context,
    private val currentStorageId: Int?
) : ListAdapter<TetroidStorage, StorageViewHolder>(DIFF_CALLBACK) {

    interface IItemMenuClickListener {
        fun onClick(itemView: View, anchorView: View)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var onItemClickListener: View.OnClickListener? = null
    var onItemLongClickListener: View.OnLongClickListener? = null
    var onItemMenuClickListener: IItemMenuClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageViewHolder {
        val view = inflater.inflate(R.layout.list_item_storage, parent, false)
        onItemClickListener?.let {
            view.setOnClickListener(it)
        }
        onItemLongClickListener?.let {
            view.setOnLongClickListener(it)
        }
        val viewHolder = StorageViewHolder(view, currentStorageId)
        onItemMenuClickListener?.let { listener ->
            viewHolder.ivMenu.setOnClickListener { listener.onClick(view, viewHolder.ivMenu) }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: StorageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): TetroidStorage {
        return super.getItem(position)
    }

    /**
     *
     */
    class StorageViewHolder internal constructor(
        view: View?,
        private val currentStorageId: Int?
    ) : RecyclerView.ViewHolder(view!!) {

        private val tvName: TextView = itemView.findViewById(R.id.text_view_name)
        private val tvPath: TextView = itemView.findViewById(R.id.text_view_path)
        private val ivError: ImageView = itemView.findViewById(R.id.image_view_error)
        private val tvError: TextView = itemView.findViewById(R.id.text_view_error)
        private val ivCurrent: ImageView = itemView.findViewById(R.id.image_view_current)
        private val ivDefault: ImageView = itemView.findViewById(R.id.image_view_default)
        val ivMenu: ImageView = itemView.findViewById(R.id.image_view_menu)

        fun bind(storage: TetroidStorage) {
            tvName.text = storage.name
            tvPath.text = storage.path
            ivCurrent.visibility = if (storage.id == currentStorageId) VISIBLE else GONE
            ivDefault.visibility = if (storage.isDefault) VISIBLE else GONE
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

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<TetroidStorage> = object : DiffUtil.ItemCallback<TetroidStorage>() {
            override fun areItemsTheSame(
                oldUser: TetroidStorage, newItem: TetroidStorage
            ): Boolean {
                // User properties may have changed if reloaded from the DB, but ID is fixed
                return oldUser.id == newItem.id
            }

            override fun areContentsTheSame(
                oldUser: TetroidStorage, newItem: TetroidStorage
            ): Boolean {
                // NOTE: if you use equals, your object must properly override Object#equals()
                // Incorrectly returning false here will result in too many animations.
                return oldUser == newItem
            }
        }
    }

}