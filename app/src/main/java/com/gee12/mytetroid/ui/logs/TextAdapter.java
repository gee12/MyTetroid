package com.gee12.mytetroid.ui.logs;

import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.PrecomputedTextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gee12.mytetroid.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

//import android.view.View;

public class TextAdapter extends RecyclerView.Adapter<TextAdapter.TextViewHolder> {

    class TextViewHolder extends RecyclerView.ViewHolder {

        private AppCompatTextView textView;
        PrecomputedTextCompat.Params params;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            this.textView = (AppCompatTextView) itemView;
            // убираем перенос слов, замедляющий работу
            if (Build.VERSION.SDK_INT >= 23) {
                textView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
                textView.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
            }
            this.params = textView.getTextMetricsParamsCompat();
        }

        void bind(String text) {
//            textView.setText(text);
            // запускаем расчет заранее
            Future<PrecomputedTextCompat> future = PrecomputedTextCompat.getTextFuture(
                    text, textView.getTextMetricsParamsCompat(), null);
//                    text, textView.getTextMetricsParamsCompat(), command ->
//                    {
//                        PrecomputedTextCompat precomputedText = PrecomputedTextCompat.create(text, params);
//                        textView.post(() -> textView.setText(precomputedText));
//                    });
            // передадим future в TextView, который будет ждать результат до onMeasure()
            textView.setTextFuture(future);
        }
    }

    private List<String> dataSet = new ArrayList<>();

    public void setItems(Collection<String> items) {
        dataSet.addAll(items);
        notifyDataSetChanged();
    }

    public void setItem(String item) {
        dataSet.clear();
        dataSet.add(item);
        notifyDataSetChanged();
    }

    public void clearItems() {
        dataSet.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_logs, parent, false);
        return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
        holder.bind(dataSet.get(position));
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}
