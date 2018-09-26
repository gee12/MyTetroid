package com.gee12.mytetroid.views;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidNode;

import java.util.List;

import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListAdapter;

public class NodesListAdapter extends MultiLevelListAdapter {

    public interface OnNodeNameClickListener {
        /**
         * Вызывается при клике на имени ветки
         *
         * @param node Объект ветки.
         */
        void onClick(TetroidNode node);
    }

    /**
     *
     */
    private class NodeViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView recordsCountView;
        ImageView arrowView;
        LinearLayout headerView;
    }

    private Context context;
    private static LayoutInflater inflater = null;
    private OnNodeNameClickListener onNodeNameClickListener;

    public NodesListAdapter(Context context, OnNodeNameClickListener onNodeNameClickListener) {
        super();
        this.context = context;
        this.onNodeNameClickListener = onNodeNameClickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    protected boolean isExpandable(Object object) {
        return ((TetroidNode)object).isHaveSubNodes();
    }

    @Override
    protected List<?> getSubObjects(Object object) {
        return ((TetroidNode)object).getSubNodes();
    }

    @Override
    protected View getViewForObject(Object object, View convertView, ItemInfo itemInfo) {
        NodeViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new NodeViewHolder();
            convertView = inflater.inflate(R.layout.list_item_node, null);
            viewHolder.iconView = (ImageView) convertView.findViewById(R.id.node_view_icon);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.node_view_name);
            viewHolder.recordsCountView = (TextView) convertView.findViewById(R.id.node_view_records_count);
            viewHolder.arrowView = (ImageView) convertView.findViewById(R.id.node_view_arrow);
            viewHolder.headerView = (LinearLayout) convertView.findViewById(R.id.node_view_header);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (NodeViewHolder) convertView.getTag();
        }

        final TetroidNode node = (TetroidNode) object;
        // иконка
//        viewHolder.iconView.setImageResource(node.getIcon());
        // имя
        viewHolder.nameView.setText(node.getName());
        // количество записей в ветке
        if (node.getRecordsCount() > 0) {
            viewHolder.recordsCountView.setVisibility(View.VISIBLE);
            viewHolder.recordsCountView.setText(String.format("[%d]", node.getRecordsCount()));
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorNodeName));
        }
        else {
            viewHolder.recordsCountView.setVisibility(View.GONE);
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorNodeNameWithoutRecords));
        }
        // вьюшка всего заголовка ветки (с иконкой и именем)
        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
        viewHolder.headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNodeNameClickListener.onClick(node);
            }
        });
        // стрелка раскрытия/закрытия ветки
        if (itemInfo.isExpandable()) {
            viewHolder.arrowView.setVisibility(View.VISIBLE);
            viewHolder.arrowView.setImageResource(itemInfo.isExpanded() ?
                    R.drawable.arrow_up : R.drawable.arrow_down);
        } else {
            viewHolder.arrowView.setVisibility(View.GONE);
        }

        return convertView;
    }
}
