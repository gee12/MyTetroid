package com.gee12.mytetroid.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.ReceivedData;

public class IntentsAdapter extends BaseAdapter {

    /**
     *
     */
    class IntentsViewHolder {
        TextView titleView;
        TextView summView;

        IntentsViewHolder(@NonNull View itemView) {
            this.titleView = itemView.findViewById(R.id.text_view_title);
            this.summView = itemView.findViewById(R.id.text_view_summ);
        }

        void bind(int pos) {
            ReceivedData item = (ReceivedData) getItem(pos);
            String[] parts = item.splitTitles(mContext);
            if (parts.length < 2)
                return;

            // TODO: пока активны пункты только с созданием новых записей
            boolean active = item.isCreate();

            if (!active) {
                String s = mContext.getString(R.string.title_not_implemented_yet);
                Spannable text = new SpannableString(s);
                text.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new RelativeSizeSpan(0.8f), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleView.setText(text);
                titleView.append("\n" + parts[0]);
            } else {
                titleView.setText(Html.fromHtml(parts[0]));
            }
            summView.setText(parts[1]);

            int titleColorId = (active) ? R.color.colorBaseText : R.color.colorLightText;
            titleView.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), titleColorId));
            int summColorId = (active) ? R.color.colorBase2Text : R.color.colorLightText;
            summView.setTextColor(ContextCompat.getColor(mContext.getApplicationContext(), summColorId));
        }
    }

    private LayoutInflater mInflater;
    private Context mContext;
    private ReceivedData[] mDataSet;

    /**
     *
     * @param context
     * @param data
     */
    public IntentsAdapter(Context context, ReceivedData[] data) {
        this.mContext = context;
        this.mDataSet = data;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return mDataSet.length;
    }

    @Override
    public Object getItem(int position) {
        return mDataSet[position];
    }

    @Override
    public long getItemId(int position) {
        return mDataSet[position].getStringId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IntentsViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_dialog, null);
            viewHolder = new IntentsViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (IntentsViewHolder) convertView.getTag();
        }
        viewHolder.bind(position);
        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return mDataSet[position].isCreate();
    }
}
