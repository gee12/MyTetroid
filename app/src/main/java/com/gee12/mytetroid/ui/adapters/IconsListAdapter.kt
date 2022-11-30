package com.gee12.mytetroid.ui.adapters

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
import java.util.ArrayList

class IconsListAdapter(
    private val context: Context,
    private val onLoadIconIfNeed: (TetroidIcon) -> Unit,
) : BaseAdapter() {
    private inner class IconViewHolder {
        var iconView: ImageView? = null
        var nameView: TextView? = null
    }

    private val inflater: LayoutInflater
    private var dataSet: List<TetroidIcon>
    var selectedIcon: TetroidIcon? = null
    private var selectedView: View? = null

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        dataSet = ArrayList()
    }

    fun setData(data: List<TetroidIcon>) {
        dataSet = data
        notifyDataSetChanged()
    }

    fun setSelectedView(view: View?) {
        // сбрасываем выделение у прошлого view
        if (selectedView != null && selectedView !== view) {
            setSelected(selectedView, isSelected = false)
        }
        selectedView = view
        setSelected(selectedView, isSelected = true)
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
        var view = convertView
        val viewHolder: IconViewHolder
        if (view == null) {
            viewHolder = IconViewHolder()
            view = inflater.inflate(R.layout.list_item_icon, null)
            viewHolder.iconView = view.findViewById(R.id.icon_view_image)
            viewHolder.nameView = view.findViewById(R.id.icon_view_name)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as IconViewHolder
        }
        val icon = dataSet[position]
        // иконка
        onLoadIconIfNeed(icon)
        viewHolder.iconView!!.setImageDrawable(icon.icon)
        // имя
        viewHolder.nameView!!.text = icon.name

        // выделение
        val isSelected = icon === selectedIcon
        setSelected(view, isSelected)
        if (isSelected && selectedView == null) {
            // получаем выделенный view в первый раз, т.к. listView.getChildAt(pos) позвращает null на старте
            selectedView = view
        }
        return view!!
    }

    private fun setSelected(view: View?, isSelected: Boolean) {
        if (view == null) return
        val color = if (isSelected) context.getColor(R.color.colorCurNode) else Color.TRANSPARENT
        view.setBackgroundColor(color)
    }

}