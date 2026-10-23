package com.example.studentmanager.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.studentmanager.activities.FruitManagementActivity;
import com.example.studentmanager.databinding.FragmentFruitDetailBinding;
import com.example.studentmanager.models.FruitEntity;
import com.example.studentmanager.utils.ExpiryBadge;
import com.example.studentmanager.utils.ExpiryRefreshScheduler;
import com.example.studentmanager.utils.ExpiryPolicy;
import com.example.studentmanager.viewmodels.FruitDetailViewModel;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class FruitDetailFragment extends Fragment {
    private static final String ARG_FRUIT_ID = "fruit_id";
    private FragmentFruitDetailBinding binding;
    private FruitDetailViewModel viewModel;
    private ExpiryRefreshScheduler expiryScheduler;
    private com.example.studentmanager.viewmodels.FruitEditorViewModel editor;
    private boolean deleting;

    public static FruitDetailFragment newInstance(int id) {
        FruitDetailFragment fragment = new FruitDetailFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_FRUIT_ID, id);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle state) {
        binding = FragmentFruitDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        FruitManagementActivity activity = (FruitManagementActivity) requireActivity();
        int fruitId = requireArguments().getInt(ARG_FRUIT_ID);
        viewModel = new ViewModelProvider(this,
                new FruitDetailViewModel.Factory(activity.getFruitRepository(), fruitId))
                .get(FruitDetailViewModel.class);
        binding.btnBack.setOnClickListener(v -> activity.getSupportFragmentManager().popBackStack());
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        editor = new ViewModelProvider(this, new com.example.studentmanager.viewmodels.FruitEditorViewModel.Factory(activity.getFruitRepository(), fruitId))
                .get(com.example.studentmanager.viewmodels.FruitEditorViewModel.class);
        binding.btnEdit.setOnClickListener(v -> activity.openFruitForm(fruitId));
        binding.btnDelete.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa trái cây?").setMessage("Bạn có chắc muốn xóa trái cây này? Thao tác không thể hoàn tác.")
                .setNegativeButton("Hủy", null).setPositiveButton("Xóa", (dialog, which) -> editor.delete(fruitId)).show());
        editor.getOperation().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            deleting = result.isLoading();
            binding.btnDelete.setEnabled(!deleting); binding.btnEdit.setEnabled(!deleting);
            if (result.getError() != null) {
                android.widget.Toast.makeText(requireContext(), "Không thể xóa: " + result.getError().getMessage(), android.widget.Toast.LENGTH_LONG).show();
                editor.consumeResult();
            } else if (result.isSuccess()) {
                editor.consumeResult();
                android.widget.Toast.makeText(requireContext(), "Đã xóa trái cây", android.widget.Toast.LENGTH_SHORT).show();
                activity.getSupportFragmentManager().popBackStack();
            }
        });
        expiryScheduler = new ExpiryRefreshScheduler(viewModel::refreshToday);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshToday();
            viewModel.retry();
        }
        if (expiryScheduler != null) expiryScheduler.start();
    }

    @Override public void onPause() {
        if (expiryScheduler != null) expiryScheduler.stop();
        super.onPause();
    }

    private void render(FruitDetailViewModel.State state) {
        if (binding == null || state == null) return;
        binding.progressLoading.setVisibility(state.status == FruitDetailViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.tvNotFound.setVisibility(state.status == FruitDetailViewModel.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(state.status == FruitDetailViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.detailContent.setVisibility(state.status == FruitDetailViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        binding.btnEdit.setVisibility(state.status == FruitDetailViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        binding.btnDelete.setVisibility(state.status == FruitDetailViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        binding.btnEdit.setEnabled(!deleting); binding.btnDelete.setEnabled(!deleting);
        if (state.status == FruitDetailViewModel.Status.CONTENT) bindFruit(state.fruit, state.expiryStatus);
    }

    private void bindFruit(FruitEntity fruit, ExpiryPolicy.Status expiryStatus) {
        binding.tvName.setText(fruit.name);
        binding.tvCategory.setText(fruit.category);
        binding.tvPrice.setText(formatPrice(fruit.price));
        binding.tvQuantity.setText(String.valueOf(fruit.quantity));
        binding.tvExpiryDate.setText(formatDate(fruit.expiryDate));
        ExpiryBadge.bind(binding.tvExpiryStatus, expiryStatus);
        String description = fruit.description == null ? "" : fruit.description.trim();
        binding.descriptionGroup.setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvDescription.setText(description);
    }

    private String formatPrice(double price) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        format.setMaximumFractionDigits(2);
        return format.format(price) + " ₫";
    }

    private String formatDate(String isoDate) {
        SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        source.setLenient(false);
        source.setTimeZone(TimeZone.getTimeZone("UTC"));
        ParsePosition position = new ParsePosition(0);
        Date date = source.parse(isoDate == null ? "" : isoDate, position);
        if (date == null || position.getIndex() != (isoDate == null ? 0 : isoDate.length())) return "Ngày không hợp lệ";
        SimpleDateFormat display = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
        display.setTimeZone(TimeZone.getTimeZone("UTC"));
        return display.format(date);
    }

    @Override public void onDestroyView() {
        if (expiryScheduler != null) expiryScheduler.stop();
        binding = null;
        super.onDestroyView();
    }
}
