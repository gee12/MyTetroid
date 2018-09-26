package com.gee12.mytetroid;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
    private class ViewHolder {
        ImageView iconView;
        TextView nameView;
        ImageView arrowView;
        LinearLayout headerView;
    }

    private Context context;
    private OnNodeNameClickListener onNodeNameClickListener;

    public NodesListAdapter(Context context, OnNodeNameClickListener onNodeNameClickListener) {
        super();
        this.context = context;
        this.onNodeNameClickListener = onNodeNameClickListener;
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
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.node_view, null);
            viewHolder.iconView = (ImageView) convertView.findViewById(R.id.node_view_icon);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.node_view_name);
            viewHolder.arrowView = (ImageView) convertView.findViewById(R.id.node_view_arrow);
            viewHolder.headerView = (LinearLayout) convertView.findViewById(R.id.node_view_header);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final TetroidNode node = (TetroidNode) object;
        // иконка
//        viewHolder.iconView.setImageResource(node.getIcon());
        // имя
        viewHolder.nameView.setText(node.getName());
        // вьюшка всего заголовка ветки (с иконкой и именем)
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
//        viewHolder.headerView.setLayoutParams(layoutParams);
        viewHolder.headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNodeNameClickListener.onClick(node);
            }
        });
        // expand/collapse arrows
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
