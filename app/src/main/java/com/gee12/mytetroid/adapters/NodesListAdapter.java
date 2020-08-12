package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.graphics.Color;
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
        void onClick(TetroidNode node);

        /**
         * Обработчик долгого клика на имени ветки.
         * @param view
         * @param node
         */
        boolean onLongClick(View view, TetroidNode node, int pos);
    }

    /**
     *
     */
    private class NodeViewHolder {
        ImageView iconView;
        TextView nameView;
        TextView recordsCountView;
        ImageView arrowView;
        ConstraintLayout headerView;
    }

    private Context mContext;
    private LayoutInflater mInflater;
    private OnNodeHeaderClickListener mOnNodeHeaderClickListener;
    private TetroidNode mCurNode;

    public NodesListAdapter(Context context, OnNodeHeaderClickListener onNodeHeaderClickListener) {
        super();
        this.mContext = context;
        this.mOnNodeHeaderClickListener = onNodeHeaderClickListener;
        this.mInflater = LayoutInflater.from(context);
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
            convertView = mInflater.inflate(R.layout.list_item_node, null);
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
        // название
        String cryptedName = mContext.getString(R.string.title_crypted_node_name);
        viewHolder.nameView.setText(node.getCryptedName(cryptedName));
        // количество записей в ветке
        if (node.getRecordsCount() > 0 && node.isNonCryptedOrDecrypted()) {
            viewHolder.recordsCountView.setVisibility(View.VISIBLE);
            viewHolder.recordsCountView.setText(String.format(Locale.getDefault(), "[%d]", node.getRecordsCount()));
            viewHolder.nameView.setTextColor(ContextCompat.getColor(mContext, R.color.colorBaseText));
        } else {
            viewHolder.recordsCountView.setVisibility(View.GONE);
            viewHolder.nameView.setTextColor(ContextCompat.getColor(mContext, R.color.colorLightText));
        }
        // вьюшка всего заголовка ветки (с иконкой и именем)
//        ((RelativeLayout.LayoutParams)viewHolder.headerView.getLayoutParams()).setMargins(20 * node.getLevel(),0,50,0);
        viewHolder.headerView.setOnClickListener(v -> mOnNodeHeaderClickListener.onClick(node));
        viewHolder.headerView.setOnLongClickListener(v -> mOnNodeHeaderClickListener.onLongClick(view, node, pos));
        // стрелка раскрытия/закрытия ветки
        int rightMargin = 0;
        if (itemInfo.isExpandable() && node.isNonCryptedOrDecrypted()) {
            viewHolder.arrowView.setVisibility(View.VISIBLE);
            viewHolder.arrowView.setImageResource(itemInfo.isExpanded() ?
                    R.drawable.ic_arrow_collapse : R.drawable.ic_arrow_expand);
            rightMargin = 60;
        } else {
            viewHolder.arrowView.setVisibility(View.GONE);
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.headerView.getLayoutParams();
        layoutParams.setMargins(20 * node.getLevel(),0,rightMargin,0);
        // подсветка
        if (mCurNode == node) {
            convertView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorCurNode));
        } else if (node.isCrypted() && App.IsHighlightCryptedNodes) {
            convertView.setBackgroundColor(App.HighlightAttachColor);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    public void setCurNode(TetroidNode node) {
        this.mCurNode = node;
    }

    public TetroidNode getItem(int position) {
        return (TetroidNode) getListView().getAdapter().getItem(position);
    }

}
