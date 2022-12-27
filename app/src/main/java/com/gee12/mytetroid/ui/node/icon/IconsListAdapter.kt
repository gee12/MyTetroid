package com.gee12.mytetroid.ui.node.icon

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidIcon

class IconsListAdapter(
    private val context: Context,
    private val onLoadIconIfNeed: (TetroidIcon) -> Unit,
) : BaseAdapter() {
    private inner class IconViewHolder {
        lateinit var iconView: ImageView
        lateinit var nameView: TextView
    }

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var dataSet: List<TetroidIcon> = emptyList()
    var selectedIcon: TetroidIcon? = null
    private var selectedView: View? = null

    fun setData(data: List<TetroidIcon>) {
        dataSet = data
        notifyDataSetChanged()
    }

    fun setSelectedView(view: View?) {
        // сбрасываем выделение у прошлого view
        selectedView?.let {
            if (it != view) {
                setSelected(it, isSelected = false)
            }
        }
        selectedView = view
        selectedView?.let {
            setSelected(it, isSelected = true)
        }
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): Any {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].path.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: IconViewHolder
        if (convertView == null) {
            viewHolder = IconViewHolder()
            view = inflater.inflate(R.layout.list_item_icon, null)
            viewHolder.iconView = view.findViewById(R.id.icon_view_image)
            viewHolder.nameView = view.findViewById(R.id.icon_view_name)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as IconViewHolder
        }
        val icon = dataSet[position]
        // иконка
        onLoadIconIfNeed(icon)
        viewHolder.iconView.setImageDrawable(icon.icon)
        // имя
        viewHolder.nameView.text = icon.name

        // выделение
        val isSelected = icon === selectedIcon
        setSelected(view, isSelected)
        if (isSelected && selectedView == null) {
            // получаем выделенный view в первый раз, т.к. listView.getChildAt(pos) позвращает null на старте
            selectedView = view
        }
        return view
    }

    private fun setSelected(view: View, isSelected: Boolean) {
        val color = if (isSelected) context.getColor(R.color.colorCurNode) else Color.TRANSPARENT
        view.setBackgroundColor(color)
    }

}