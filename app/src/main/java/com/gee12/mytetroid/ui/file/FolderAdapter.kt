package com.gee12.mytetroid.ui.file

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.gee12.mytetroid.R

class FolderAdapter(
    context: Context,
    private val dataList: MutableList<FileItem>,
) : ArrayAdapter<FileItem>(context, R.layout.list_item_folder_picker, dataList) {

    private inner class FileViewHolder(
        val iconView: ImageView,
        val nameView: TextView,
    )

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun setData(data: List<FileItem>) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: FileViewHolder
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_folder_picker, null)
            viewHolder = FileViewHolder(
                iconView = view.findViewById(R.id.image_view_icon),
                nameView = view.findViewById(R.id.text_view_name),
            )
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as FileViewHolder
        }
        val (name, isFolder) = dataList[position]
        val iconRes = if (isFolder) R.drawable.ic_folder_2 else R.drawable.ic_file_2
        viewHolder.iconView.setImageResource(iconRes)
        viewHolder.nameView.text = name

        return view!!
    }
}