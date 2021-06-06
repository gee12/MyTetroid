package com.gee12.mytetroid.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SortHelper;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private final LayoutInflater mInflater;
    private final Context mContext;
    private ArrayList mData;

    public TagsListAdapter(Context context) {
        this.mContext = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mData = new ArrayList<>();
    }

    public void setDataItems(Map<String,TetroidTag> data, SortHelper sortHelper) {
        this.mData = new ArrayList<>(data.entrySet());
        sort(sortHelper);
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
        // название метки
        viewHolder.nameView.setText(item.getKey());
        // количество записей
        final List<TetroidRecord> records = item.getValue().getRecords();
        viewHolder.recordsCountView.setText(String.format(Locale.getDefault(), "[%d]", records.size()));

        return convertView;
    }

    private void sort(SortHelper sortHelper) {
        if (sortHelper == null)
            return;
        sort(sortHelper.isByName(), sortHelper.isAscent());
    }

    public void sort(boolean byName, boolean isAscent) {
        Collections.sort(mData, new Comparator<Map.Entry<String, TetroidTag>>() {
            @Override
            public int compare(Map.Entry<String, TetroidTag> o1, Map.Entry<String, TetroidTag> o2) {
                if (o1 == o2) {
                    return 0;
                } else if (o1 == null) {
                    return (isAscent) ? 1 : -1;
                } else if (o2 == null) {
                    return (isAscent) ? -1 : 1;
                } else {
                    int res;
                    if (byName) {
                        res = o1.getKey().compareTo(o2.getKey());
                    } else {
                        res = Integer.compare(o1.getValue().getRecords().size(), o2.getValue().getRecords().size());
                        if (res == 0) {
                            res = o1.getKey().compareTo(o2.getKey());
                        }
                    }
                    return (isAscent) ? res : res * -1;
                }
            }
        });
        notifyDataSetChanged();
    }

}
