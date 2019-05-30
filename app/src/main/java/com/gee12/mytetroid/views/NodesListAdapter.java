package com.gee12.mytetroid.views;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gee12.mytetroid.BuildConfig;
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
    private LayoutInflater inflater;
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
            viewHolder.iconView = convertView.findViewById(R.id.node_view_icon);
            viewHolder.nameView = convertView.findViewById(R.id.node_view_name);
            viewHolder.recordsCountView = convertView.findViewById(R.id.node_view_records_count);
            viewHolder.arrowView = convertView.findViewById(R.id.node_view_arrow);
            viewHolder.headerView = convertView.findViewById(R.id.node_view_header);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (NodeViewHolder) convertView.getTag();
        }

        final TetroidNode node = (TetroidNode) object;
        // иконка
        if (node.getIcon() != null || node.isCrypted()) {
            viewHolder.iconView.setVisibility(View.VISIBLE);
            viewHolder.iconView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            if (!node.isNonCryptedOrDecrypted()) {
                viewHolder.iconView.setImageResource(R.drawable.ic_crypted_node);
            }
            else if (node.getIcon() != null) {
                viewHolder.iconView.setImageDrawable(node.getIcon());
            } else {
                viewHolder.iconView.setImageResource(R.drawable.ic_decrypted_node);
            }
        } else {
            viewHolder.iconView.setVisibility(View.GONE);
        }
        // имя
        viewHolder.nameView.setText(node.getCryptedName());
        // количество записей в ветке
            viewHolder.recordsCountView.setText(String.format("[%d]", node.getRecordsCount()));
        if (node.getRecordsCount() > 0 && node.isNonCryptedOrDecrypted()) {
            viewHolder.recordsCountView.setVisibility(View.VISIBLE);
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorBaseText));
        }
        else {
//            viewHolder.recordsCountView.setVisibility(View.GONE);
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorLightText));
        }
        // вьюшка всего заголовка ветки (с иконкой и именем)
//        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
        viewHolder.headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNodeHeaderClickListener.onClick(node);
            }
        });
        // стрелка раскрытия/закрытия ветки
        if (itemInfo.isExpandable() && node.isNonCryptedOrDecrypted()) {
            viewHolder.arrowView.setVisibility(View.VISIBLE);
            viewHolder.arrowView.setImageResource(itemInfo.isExpanded() ?
                    R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                    ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams())
                            .setMargins(20 * node.getLevel(),0,60,0);

        } else {
            viewHolder.arrowView.setVisibility(View.GONE);
            ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams())
                    .setMargins(20 * node.getLevel(),0,0,0);

        }

        return convertView;
    }
}
