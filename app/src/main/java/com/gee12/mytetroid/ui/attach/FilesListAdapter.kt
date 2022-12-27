package com.gee12.mytetroid.ui.attach

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidFile
import java.util.ArrayList

class FilesListAdapter(
    private val context: Context,
    private val getAttachedFileSize: (TetroidFile) -> String?,
) : BaseAdapter() {

    private inner class FileViewHolder {
        lateinit var iconView: ImageView
        lateinit var lineNumView: TextView
        lateinit var nameView: TextView
        lateinit var sizeView: TextView
    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var dataSet: List<TetroidFile> = emptyList()
        private set

    fun reset(data: List<TetroidFile> = emptyList()) {
        dataSet = data
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): Any {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: FileViewHolder
        if (view == null) {
            viewHolder = FileViewHolder()
            view = inflater.inflate(R.layout.list_item_file, null)
            viewHolder.iconView = view.findViewById(R.id.file_view_icon)
            viewHolder.lineNumView = view.findViewById(R.id.file_view_line_num)
            viewHolder.nameView = view.findViewById(R.id.file_view_name)
            viewHolder.sizeView = view.findViewById(R.id.file_view_size)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as FileViewHolder
        }
        val file = dataSet[position]
        // номер строки
        viewHolder.lineNumView.text = (position + 1).toString()
        // название файла
        viewHolder.nameView.text = file.name
        // размер
        val sizeString = getAttachedFileSize(file)
        if (sizeString != null) {
            viewHolder.iconView.setImageResource(R.drawable.ic_file)
            viewHolder.sizeView.text = sizeString
        } else {
            viewHolder.iconView.setImageResource(R.drawable.ic_file_missing)
            viewHolder.sizeView.setText(R.string.title_file_is_missing)
        }
        return view!!
    }
}