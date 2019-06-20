package com.gee12.mytetroid.views;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.data.FoundObject;
import com.gee12.mytetroid.data.TetroidRecord;

import java.util.ArrayList;
import java.util.List;

public class FoundListAdapter extends BaseAdapter {

    /**
     *
     */
    private class FoundViewHolder {
        TextView lineNumView;
        TextView nameView;
        TextView typeView;
    }

    private LayoutInflater inflater;
    private List<FoundObject> dataSet;
    private Context context;

    public FoundListAdapter(Context context, List<FoundObject> dataSet) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.dataSet = dataSet;
    }

//    public void setDataItems(List<FoundObject> dataSet) {
//        this.dataSet = dataSet;
//        notifyDataSetChanged();
//    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public Object getItem(int position) {
        return dataSet.get(position);
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).getDisplayName().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FoundViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FoundViewHolder();
            convertView = inflater.inflate(R.layout.list_item_found, null);
            viewHolder.lineNumView = convertView.findViewById(R.id.text_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.text_view_name);
            viewHolder.typeView = convertView.findViewById(R.id.text_view_type);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FoundViewHolder)convertView.getTag();
        }

        final FoundObject found = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название найденного объекта
        viewHolder.nameView.setText(found.getDisplayName());
        // тип найденного объекта
        viewHolder.typeView.setText(found.getFoundTypeString(context));

        return convertView;
    }
}
