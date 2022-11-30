package com.gee12.mytetroid.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.interactors.IconsInteractor;
import com.gee12.mytetroid.model.TetroidIcon;

import java.util.ArrayList;
import java.util.List;

public class IconsListAdapter extends BaseAdapter {

    /**
     *
     */
    private class IconViewHolder {
        ImageView iconView;
        TextView nameView;
    }

    private LayoutInflater mInflater;
    private List<TetroidIcon> mDataSet;
    private Context mContext;
    private TetroidIcon mSelectedIcon;
    private View mSelectedView;
    private IconsInteractor mIconsInteractor;

    public IconsListAdapter(Context context, IconsInteractor interactor) {
        this.mContext = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mDataSet = new ArrayList<>();
        this.mIconsInteractor = interactor;
    }

    public void setData(List<TetroidIcon> data) {
        this.mDataSet = data;
        notifyDataSetChanged();
    }

    public void setSelectedIcon(TetroidIcon icon) {
        this.mSelectedIcon = icon;
    }

    public void setSelectedView(View view) {
        // сбрасываем выделение у прошлого view
        if (mSelectedView != null && mSelectedView != view) {
            setSelected(mSelectedView, false);
        }
        this.mSelectedView = view;
        setSelected(mSelectedView, true);
    }

    public TetroidIcon getSelectedIcon() {
        return mSelectedIcon;
    }

    @Override
    public int getCount() {
        return mDataSet.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mDataSet.get(position).getPath().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IconViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new IconViewHolder();
            convertView = mInflater.inflate(R.layout.list_item_icon, null);
            viewHolder.iconView = convertView.findViewById(R.id.icon_view_image);
            viewHolder.nameView = convertView.findViewById(R.id.icon_view_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (IconViewHolder) convertView.getTag();
        }

        final TetroidIcon icon = mDataSet.get(position);
        // иконка
        mIconsInteractor.loadIconIfNull(icon);
        viewHolder.iconView.setImageDrawable(icon.getIcon());
        // имя
        viewHolder.nameView.setText(icon.getName());

        // выделение
        boolean isSelected = icon == mSelectedIcon;
        setSelected(convertView, isSelected);
        if (isSelected && mSelectedView == null) {
            // получаем выделенный view в первый раз, т.к. listView.getChildAt(pos) позвращает null на старте
            mSelectedView = convertView;
        }

        return convertView;
    }

    private void setSelected(View view, boolean isSelected) {
        if (view == null)
            return;
        int color = (isSelected) ? mContext.getColor(R.color.colorCurNode) : Color.TRANSPARENT;
        view.setBackgroundColor(color);
    }
}
