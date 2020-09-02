package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TagsListAdapter extends BaseAdapter {

    /**
     *
     */
    private class TagsViewHolder {
        TextView nameView;
        TextView recordsCountView;
    }

    private LayoutInflater mInflater;
    private ArrayList mData;
    private Context mContext;

    public TagsListAdapter(Context context) {
        this.mContext = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mData = new ArrayList<>();
    }

    public void setDataItems(Map<String,TetroidTag> data) {
        this.mData = new ArrayList<>(data.entrySet());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Map.Entry<String, TetroidTag> getItem(int position) {
        return (Map.Entry) mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mData.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TagsViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new TagsViewHolder();
            convertView = mInflater.inflate(R.layout.list_item_tag, null);
            viewHolder.nameView = convertView.findViewById(R.id.tag_view_name);
            viewHolder.recordsCountView = convertView.findViewById(R.id.tag_view_records_count);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (TagsViewHolder) convertView.getTag();
        }

        Map.Entry<String, TetroidTag> item = getItem(position);
        final List<TetroidRecord> records = item.getValue().getRecords();
        // название метки
        viewHolder.nameView.setText(item.getValue().getName());
        // количество записей
        viewHolder.recordsCountView.setText(String.format(Locale.getDefault(), "[%d]", records.size()));

        return convertView;
    }
}
