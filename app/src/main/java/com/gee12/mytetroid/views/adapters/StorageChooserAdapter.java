package com.gee12.mytetroid.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class StorageChooserAdapter extends BaseAdapter {

    class TitleData {
        String title;
        String summ;

        public TitleData(String title, String summ) {
            this.title = title;
            this.summ = summ;
        }
    }

    /**
     *
     */
    class TitlesViewHolder {
        TextView titleView;
        TextView summView;

        TitlesViewHolder(@NonNull View itemView) {
            this.titleView = itemView.findViewById(R.id.text_view_title);
            this.summView = itemView.findViewById(R.id.text_view_summ);
        }

        void bind(int pos) {
            TitleData item = mDataSet.get(pos);
            titleView.setText(item.title);
            summView.setText(Utils.fromHtml(item.summ));
        }
    }

    private LayoutInflater mInflater;
    private Context mContext;
    private List<TitleData> mDataSet;

    /**
     *
     * @param context
     */
    public StorageChooserAdapter(Context context) {
        this.mContext = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mDataSet = new ArrayList<>();
        // заполняем данными
        mDataSet.add(new TitleData(context.getString(R.string.title_open_storage),
                context.getString(R.string.title_open_storage_summ)));
        mDataSet.add(new TitleData(context.getString(R.string.title_create_storage),
                context.getString(R.string.text_create_storage_summ_html)));
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
        return mDataSet.get(position).summ.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TitlesViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_dialog, null);
            viewHolder = new TitlesViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (TitlesViewHolder) convertView.getTag();
        }
        viewHolder.bind(position);
        return convertView;
    }
}
