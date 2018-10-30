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

    public interface OnNodeHeaderClickListener {
        /**
         * Вызывается при клике на имени ветки
         *
         * @param node Объект ветки
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
    private OnNodeHeaderClickListener onNodeHeaderClickListener;

    public NodesListAdapter(Context context, OnNodeHeaderClickListener onNodeHeaderClickListener) {
        super();
        this.context = context;
        this.onNodeHeaderClickListener = onNodeHeaderClickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    protected boolean isExpandable(Object object) {
        return ((TetroidNode)object).isExpandable();
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
//        if (node.getIconUri() != null) {
//            viewHolder.iconView.setVisibility(View.VISIBLE);
//            viewHolder.iconView.setImageURI(node.getIconUri());
//        }
        if (node.getIcon() != null || !node.isNonCryptedOrDecrypted()) {
            viewHolder.iconView.setVisibility(View.VISIBLE);
            viewHolder.iconView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            if (!node.isNonCryptedOrDecrypted()) {
                viewHolder.iconView.setImageResource(R.drawable.ic_crypted_node);
            }
            else {
                viewHolder.iconView.setImageDrawable(node.getIcon());
            }
        } else {
            viewHolder.iconView.setVisibility(View.GONE);
        }
        // имя
        viewHolder.nameView.setText(node.getCryptedName());
        // количество записей в ветке
        if (node.getRecordsCount() > 0 && node.isNonCryptedOrDecrypted()) {
            viewHolder.recordsCountView.setVisibility(View.VISIBLE);
            viewHolder.recordsCountView.setText(String.format("[%d]", node.getRecordsCount()));
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorBaseText));
        }
        else {
            viewHolder.recordsCountView.setVisibility(View.GONE);
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorLightText));
        }
        // вьюшка всего заголовка ветки (с иконкой и именем)
        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
        viewHolder.headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNodeHeaderClickListener.onClick(node);
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
