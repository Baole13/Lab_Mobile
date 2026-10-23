package com.example.studentmanager.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanager.databinding.ItemFruitBinding;
import com.example.studentmanager.utils.ExpiryBadge;
import com.example.studentmanager.viewmodels.FruitListViewModel;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class FruitAdapter extends ListAdapter<FruitListViewModel.Item, FruitAdapter.FruitViewHolder> {
    public interface OnFruitClickListener { void onFruitClick(int fruitId); }
    private final OnFruitClickListener listener;
    private static final DiffUtil.ItemCallback<FruitListViewModel.Item> DIFF = new DiffUtil.ItemCallback<FruitListViewModel.Item>() {
        @Override public boolean areItemsTheSame(@NonNull FruitListViewModel.Item oldItem, @NonNull FruitListViewModel.Item newItem) {
            return oldItem.fruit.id.equals(newItem.fruit.id);
        }
        @Override public boolean areContentsTheSame(@NonNull FruitListViewModel.Item oldItem, @NonNull FruitListViewModel.Item newItem) {
            return oldItem.fruit.name.equals(newItem.fruit.name) && oldItem.fruit.category.equals(newItem.fruit.category)
                    && Double.compare(oldItem.fruit.price, newItem.fruit.price) == 0
                    && oldItem.fruit.quantity == newItem.fruit.quantity
                    && oldItem.fruit.expiryDate.equals(newItem.fruit.expiryDate)
                    && oldItem.expiryStatus.equals(newItem.expiryStatus);
        }
    };

    public FruitAdapter(OnFruitClickListener listener) { super(DIFF); this.listener = listener; }

    @NonNull @Override public FruitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FruitViewHolder(ItemFruitBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull FruitViewHolder holder, int position) {
        FruitListViewModel.Item item = getItem(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> listener.onFruitClick(item.fruit.id));
    }

    static final class FruitViewHolder extends RecyclerView.ViewHolder {
        private final ItemFruitBinding binding;
        FruitViewHolder(ItemFruitBinding binding) { super(binding.getRoot()); this.binding = binding; }
        void bind(FruitListViewModel.Item item) {
            binding.tvName.setText(item.fruit.name);
            binding.tvCategory.setText(item.fruit.category);
            binding.tvPrice.setText(formatPrice(item.fruit.price));
            binding.tvQuantity.setText("Tồn kho: " + item.fruit.quantity);
            binding.tvExpiryDate.setText("Hạn: " + formatDate(item.fruit.expiryDate));
            ExpiryBadge.bind(binding.tvExpiryStatus, item.expiryStatus);
        }
        private String formatPrice(double price) {
            NumberFormat format = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            format.setMaximumFractionDigits(2);
            return format.format(price) + " ₫";
        }
        private String formatDate(String value) {
            if (value == null) return "Ngày không hợp lệ";
            SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            source.setLenient(false);
            source.setTimeZone(TimeZone.getTimeZone("UTC"));
            ParsePosition position = new ParsePosition(0);
            Date date = source.parse(value, position);
            if (date == null || position.getIndex() != value.length()) return "Ngày không hợp lệ";
            SimpleDateFormat display = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            display.setTimeZone(TimeZone.getTimeZone("UTC"));
            return display.format(date);
        }
    }
}
