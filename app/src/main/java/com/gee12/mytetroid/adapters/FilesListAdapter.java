package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.TetroidFile;

import java.util.ArrayList;
import java.util.List;

public class FilesListAdapter extends BaseAdapter {

    /**
     *
     */
    private class FileViewHolder {
        TextView lineNumView;
        TextView nameView;
        TextView sizeView;
    }

    private LayoutInflater mInflater;
    private List<TetroidFile> mDataSet;
    private Context mContext;

    public FilesListAdapter(Context context) {
        this.mContext = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mDataSet = new ArrayList<>();
    }

    public void reset(List<TetroidFile> data) {
        this.mDataSet = data;
        notifyDataSetChanged();
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
        return mDataSet.get(position).getId().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FileViewHolder();
            convertView = mInflater.inflate(R.layout.list_item_file, null);
            viewHolder.lineNumView = convertView.findViewById(R.id.file_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.file_view_name);
            viewHolder.sizeView = convertView.findViewById(R.id.file_view_size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) convertView.getTag();
        }

        final TetroidFile file = mDataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название файла
        viewHolder.nameView.setText(file.getName());
        // размер
        ImageView icon = convertView.findViewById(R.id.file_view_icon);
        String fileSize = DataManager.getFileSize(mContext, file);
        if (fileSize != null) {
            icon.setImageResource(R.drawable.ic_file);
            viewHolder.sizeView.setText(fileSize);
        } else {
            icon.setImageResource(R.drawable.ic_file_missing);
            viewHolder.sizeView.setText(R.string.file_is_missing);
        }

        return convertView;
    }

    public List<TetroidFile> getDataSet() {
        return mDataSet;
    }
}
