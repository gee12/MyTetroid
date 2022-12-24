package com.gee12.mytetroid.ui.node

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidNode
import pl.openrnd.multilevellistview.ItemInfo
import pl.openrnd.multilevellistview.MultiLevelListAdapter
import java.util.*

class NodesListAdapter(
    private val context: Context,
    private val isHighlightCryptedNodes: Boolean,
    private val highlightColor: Int,
    private val onClick: (TetroidNode, Int) -> Unit,
    private val onLongClick: (View, TetroidNode, Int) -> Boolean,
) : MultiLevelListAdapter() {

    private inner class NodeViewHolder {
        lateinit var iconView: ImageView
        lateinit var nameCountView: TextView
        lateinit var arrowView: ImageView
        lateinit var headerView: ConstraintLayout
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var curNode: TetroidNode? = null

    fun reset() {
        super.setDataItems(ArrayList<Any?>())
        curNode = null
        notifyDataSetChanged()
    }

    override fun isExpandable(obj: Any): Boolean {
        return (obj as TetroidNode).isExpandable
    }

    override fun getSubObjects(obj: Any): List<*> {
        return (obj as TetroidNode).subNodes
    }

    override fun getParent(obj: Any): Any {
        return (obj as TetroidNode).parentNode
    }

    override fun getViewForObject(obj: Any, convertView: View?, itemInfo: ItemInfo, pos: Int): View {
        val view: View
        val viewHolder: NodeViewHolder
        if (convertView == null) {
            viewHolder = NodeViewHolder()
            view = inflater.inflate(R.layout.list_item_node, null)
            viewHolder.iconView = view.findViewById(R.id.node_view_icon)
            viewHolder.nameCountView = view.findViewById(R.id.node_view_name_count)
            viewHolder.arrowView = view.findViewById(R.id.node_view_arrow)
            viewHolder.headerView = view.findViewById(R.id.node_view_header)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as NodeViewHolder
        }
        val node = obj as TetroidNode
        // иконка
        if (node.icon != null || node.isCrypted) {
            viewHolder.iconView.visibility = View.VISIBLE
            viewHolder.iconView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (!node.isNonCryptedOrDecrypted) {
                viewHolder.iconView.setImageResource(R.drawable.ic_node_encrypted)
            } else if (node.icon != null) {
                viewHolder.iconView.setImageDrawable(node.icon)
            } else {
                viewHolder.iconView.setImageResource(R.drawable.ic_node_decrypted)
            }
        } else {
            viewHolder.iconView.visibility = View.GONE
        }
        // название
        val nameWhenEncrypted = context.getString(R.string.title_crypted_node_name)
        val name = node.getCryptedName(nameWhenEncrypted)

        // количество записей в ветке
        if (node.recordsCount > 0 && node.isNonCryptedOrDecrypted) {
            val recordsCount = "[%d]".format(node.recordsCount)
            val nameCount = SpannableString("$name $recordsCount")
            nameCount.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorBase2Text)),
                name.length + 1, nameCount.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            nameCount.setSpan(
                AbsoluteSizeSpan(context.resources.getDimension(R.dimen.font_small).toInt()),
                name.length + 1, nameCount.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            viewHolder.nameCountView.text = nameCount
            viewHolder.nameCountView.setTextColor(ContextCompat.getColor(context, R.color.colorBaseText))
        } else {
            viewHolder.nameCountView.text = name
            viewHolder.nameCountView.setTextColor(ContextCompat.getColor(context, R.color.colorLightText))
        }
        // вьюшка всего заголовка ветки (с иконкой и именем)
//        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
        viewHolder.headerView.setOnClickListener { onClick(node, pos) }
        viewHolder.headerView.setOnLongClickListener { onLongClick(view, node, pos) }
        // стрелка раскрытия/закрытия ветки
        if (itemInfo.isExpandable && node.isNonCryptedOrDecrypted) {
            viewHolder.arrowView.visibility = View.VISIBLE
            viewHolder.arrowView.setImageResource(if (itemInfo.isExpanded) R.drawable.ic_arrow_collapse else R.drawable.ic_arrow_expand)
        } else {
            viewHolder.arrowView.visibility = View.GONE
        }
        val layoutParams = viewHolder.headerView.layoutParams as RelativeLayout.LayoutParams
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layoutParams.marginStart = 20 * node.level
        } else {
            layoutParams.setMargins(20 * node.level, 0, 2, 0)
        }
        viewHolder.headerView.layoutParams = layoutParams
//        layoutParams.leftMargin = 20 * node.getLevel();

        // подсветка
        if (curNode == node) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCurNode))
        } else if (node.isCrypted && isHighlightCryptedNodes) {
            view.setBackgroundColor(highlightColor)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        return view
    }

    fun getItem(position: Int): TetroidNode {
        return listView.adapter.getItem(position) as TetroidNode
    }

    override fun setDataItems(dataItems: List<*>?) {
        if (dataItems != null) {
            super.setDataItems(dataItems)
        } else {
            super.setDataItems(ArrayList<Any?>())
        }
    }

}