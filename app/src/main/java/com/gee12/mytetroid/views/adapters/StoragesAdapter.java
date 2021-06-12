package com.gee12.mytetroid.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidStorage;

public class StoragesAdapter extends ListAdapter<TetroidStorage, StoragesAdapter.StorageViewHolder> {

//    public interface IStoragesListener {
//        void onSelected(TetroidStorage storage);
//    }

//    private Context mContext;
    private LayoutInflater mInflater;
    private View.OnClickListener mOnClickListener;
    private View.OnLongClickListener mOnLongClickListener;

    public StoragesAdapter(Context context) {
        super(DIFF_CALLBACK);
//        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    public void setOnItemClickListener(View.OnClickListener listener) {
        this.mOnClickListener = listener;
    }

    public void setOnItemLongClickListener(View.OnLongClickListener listener) {
        this.mOnLongClickListener = listener;
    }

    @NonNull
    @Override
    public StoragesAdapter.StorageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_storage, parent, false);
        if (mOnClickListener != null) {
            view.setOnClickListener(mOnClickListener);
            view.setOnLongClickListener(mOnLongClickListener);
        }
        return new StorageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoragesAdapter.StorageViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public TetroidStorage getItem(int position) {
        return super.getItem(position);
    }

    /**
     *
     */
    public static class StorageViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName;
        private TextView tvPath;

        StorageViewHolder(View view) {
            super(view);
            this.tvName = itemView.findViewById(R.id.text_view_name);
            this.tvPath = itemView.findViewById(R.id.text_view_path);

        }

        private void bind(TetroidStorage storage) {
            if (storage == null) {
                return;
            }
            tvName.setText(storage.getName());
            tvPath.setText(storage.getPath());
        }
    }

    /**
     *
     */
    public static final DiffUtil.ItemCallback<TetroidStorage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TetroidStorage>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull TetroidStorage oldUser, @NonNull TetroidStorage newUser) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldUser.getId() == newUser.getId();
                }
                @Override
                public boolean areContentsTheSame(
                        @NonNull TetroidStorage oldUser, @NonNull TetroidStorage newUser) {
                    // NOTE: if you use equals, your object must properly override Object#equals()
                    // Incorrectly returning false here will result in too many animations.
                    return oldUser.equals(newUser);
                }
            };
}
