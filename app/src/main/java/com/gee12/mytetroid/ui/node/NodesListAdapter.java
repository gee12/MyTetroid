package com.gee12.mytetroid.ui.node;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListAdapter;

public class NodesListAdapter extends MultiLevelListAdapter {

    public interface OnNodeHeaderClickListener {

        /**
         * Обработчик клика на имени ветки.
         *
         * @param node Объект ветки
         */
        void onClick(TetroidNode node, int pos);

        /**
         * Обработчик долгого клика на имени ветки.
         * @param view
         * @param node
         */
        boolean onLongClick(View view, TetroidNode node, int pos);
    }

    private class NodeViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView recordsCountView;
        ImageView arrowView;
        ConstraintLayout headerView;
    }

    private final Context context;
    private final LayoutInflater inflater;
    private OnNodeHeaderClickListener onNodeHeaderClickListener;
    private TetroidNode curNode;

    public NodesListAdapter(Context context, OnNodeHeaderClickListener onNodeHeaderClickListener) {
        super();
        this.context = context;
        this.onNodeHeaderClickListener = onNodeHeaderClickListener;
        this.inflater = LayoutInflater.from(context);
    }

    public void reset() {
        super.setDataItems(new ArrayList<>());
        this.curNode = null;
        notifyDataSetChanged();
    }

    public void setNodeHeaderClickListener(OnNodeHeaderClickListener onNodeHeaderClickListener) {
        this.onNodeHeaderClickListener = onNodeHeaderClickListener;
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
    protected Object getParent(Object object) {
        return ((TetroidNode)object).getParentNode();
    }

    @Override
    protected View getViewForObject(Object object, View convertView, ItemInfo itemInfo, int pos) {
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
        final View view = convertView;

        final TetroidNode node = (TetroidNode) object;
        // иконка
        if (node.getIcon() != null || node.isCrypted()) {
            viewHolder.iconView.setVisibility(View.VISIBLE);
            viewHolder.iconView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            if (!node.isNonCryptedOrDecrypted()) {
                viewHolder.iconView.setImageResource(R.drawable.ic_node_encrypted);
            }
            else if (node.getIcon() != null) {
                viewHolder.iconView.setImageDrawable(node.getIcon());
            } else {
                viewHolder.iconView.setImageResource(R.drawable.ic_node_decrypted);
            }
        } else {
            viewHolder.iconView.setVisibility(View.GONE);
        }
        // название
        String cryptedName = context.getString(R.string.title_crypted_node_name);
        viewHolder.nameView.setText(node.getCryptedName(cryptedName));
        // количество записей в ветке
        if (node.getRecordsCount() > 0 && node.isNonCryptedOrDecrypted()) {
            viewHolder.recordsCountView.setVisibility(View.VISIBLE);
            viewHolder.recordsCountView.setText(String.format(Locale.getDefault(), "[%d]", node.getRecordsCount()));
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorBaseText));
        } else {
            viewHolder.recordsCountView.setVisibility(View.GONE);
            viewHolder.nameView.setTextColor(ContextCompat.getColor(context, R.color.colorLightText));
        }
        // вьюшка всего заголовка ветки (с иконкой и именем)
//        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
        viewHolder.headerView.setOnClickListener(v -> onNodeHeaderClickListener.onClick(node, pos));
        viewHolder.headerView.setOnLongClickListener(v -> onNodeHeaderClickListener.onLongClick(view, node, pos));
        // стрелка раскрытия/закрытия ветки
        if (itemInfo.isExpandable() && node.isNonCryptedOrDecrypted()) {
            viewHolder.arrowView.setVisibility(View.VISIBLE);
            viewHolder.arrowView.setImageResource(itemInfo.isExpanded() ?
                    R.drawable.ic_arrow_collapse : R.drawable.ic_arrow_expand);
        } else {
            viewHolder.arrowView.setVisibility(View.GONE);
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.headerView.getLayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layoutParams.setMarginStart(20 * node.getLevel());
        } else {
            layoutParams.setMargins(20 * node.getLevel(),0, 2,0);
        }
        viewHolder.headerView.setLayoutParams(layoutParams);
//        layoutParams.leftMargin = 20 * node.getLevel();

        // подсветка
        if (curNode == node) {
            convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCurNode));
        } else if (node.isCrypted() && App.IsHighlightCryptedNodes) {
            convertView.setBackgroundColor(App.HighlightAttachColor);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    public void setCurNode(TetroidNode node) {
        this.curNode = node;
    }

    public TetroidNode getCurNode() {
        return curNode;
    }

    public TetroidNode getItem(int position) {
        return (TetroidNode) getListView().getAdapter().getItem(position);
    }

    public void setDataItems(List<?> dataItems) {
        if (dataItems != null) {
            super.setDataItems(dataItems);
        } else {
            super.setDataItems(new ArrayList<>());
        }
    }
}
