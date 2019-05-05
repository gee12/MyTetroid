package com.gee12.mytetroid.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;

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

    private static LayoutInflater inflater = null;
    private List<TetroidFile> dataSet;
    private TetroidRecord record;
    private Context context;

    public FilesListAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void reset(TetroidRecord record) {
        this.dataSet = record.getAttachedFiles();
        this.record = record;
        notifyDataSetChanged();
    }

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
        return dataSet.get(position).getId().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FileViewHolder();
            convertView = inflater.inflate(R.layout.list_item_file, null);
            viewHolder.lineNumView = convertView.findViewById(R.id.file_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.file_view_name);
            viewHolder.sizeView = convertView.findViewById(R.id.file_view_size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) convertView.getTag();
        }

        final TetroidFile file = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название файла
        viewHolder.nameView.setText(file.getName());
        // размер файла
        viewHolder.sizeView.setText(DataManager.getFileSize(context, record, file));

        return convertView;
    }
}
