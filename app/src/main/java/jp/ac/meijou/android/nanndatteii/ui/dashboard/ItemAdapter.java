package jp.ac.meijou.android.nanndatteii.ui.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.ac.meijou.android.nanndatteii.R;
import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<Item> items;
    private final Context context;
    private final OnItemClickListener listener;

    public ItemAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileName;
        private final SimpleDateFormat dateFormat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        }

        public void bind(Item item, OnItemClickListener listener) {
            // アイテムの説明を表示（メモの内容）
            String displayText = item.getDescription();
            if (displayText == null || displayText.trim().isEmpty()) {
                displayText = "アイテム #" + item.getId();
            } else if (displayText.length() > 50) {
                displayText = displayText.substring(0, 50) + "...";
            }

            // 日時を追加
            String dateStr = dateFormat.format(new Date(item.getCreatedAt()));
            fileName.setText(displayText + "\n" + dateStr);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }
}
