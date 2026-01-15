package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointable.R;
import com.example.appointable.SummaryItem;

import java.util.Collections;
import java.util.List;

public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder> {

    private List<SummaryItem> items;

    public SummaryAdapter(List<SummaryItem> items) {
        this.items = items;
    }

    public void updateItems(List<SummaryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void sortByProgress(boolean ascending) {
        Collections.sort(items, (a, b) -> {
            return ascending
                    ? Integer.compare(a.getProgress(), b.getProgress())
                    : Integer.compare(b.getProgress(), a.getProgress());
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        SummaryItem item = items.get(position);

        holder.tvName.setText(item.getChildName());
        holder.tvService.setText(item.getService());
        holder.progressBar.setProgress(item.getProgress());
        holder.tvPercentage.setText(item.getProgress() + "%");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvService, tvPercentage;
        ProgressBar progressBar;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvSummaryChildName);
            tvService = itemView.findViewById(R.id.tvSummaryService);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
        }
    }
}
