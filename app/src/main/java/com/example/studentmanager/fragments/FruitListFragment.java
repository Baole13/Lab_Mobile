package com.example.studentmanager.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.studentmanager.activities.FruitManagementActivity;
import com.example.studentmanager.adapters.FruitAdapter;
import com.example.studentmanager.databinding.FragmentFruitListBinding;
import com.example.studentmanager.utils.ExpiryRefreshScheduler;
import com.example.studentmanager.viewmodels.FruitListViewModel;

public final class FruitListFragment extends Fragment {
    private FragmentFruitListBinding binding;
    private FruitListViewModel viewModel;
    private FruitAdapter adapter;
    private ExpiryRefreshScheduler expiryScheduler;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle state) {
        binding = FragmentFruitListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        FruitManagementActivity activity = (FruitManagementActivity) requireActivity();
        viewModel = new ViewModelProvider(this,
                new FruitListViewModel.Factory(activity.getFruitRepository()))
                .get(FruitListViewModel.class);
        com.example.studentmanager.viewmodels.FruitEditorViewModel demoViewModel = new ViewModelProvider(this,
                new com.example.studentmanager.viewmodels.FruitEditorViewModel.Factory(activity.getFruitRepository(), 0))
                .get(com.example.studentmanager.viewmodels.FruitEditorViewModel.class);
        binding.btnDemoData.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Nạp dữ liệu demo?")
                .setMessage("Thêm 10 trái cây theo ngày hôm nay, tài khoản demo_customer/123456 và 3 đơn hàng mẫu. Không xóa hoặc đặt lại dữ liệu cũ; nạp lại trong cùng ngày không tạo trùng.")
                .setNegativeButton("Hủy", null).setPositiveButton("Nạp dữ liệu", (dialog, which) -> demoViewModel.loadDemoData()).show());
        demoViewModel.getOperation().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.btnDemoData.setEnabled(!result.isLoading());
            binding.btnDemoData.setText(result.isLoading() ? "Đang nạp dữ liệu…" : "Nạp dữ liệu demo");
            if (!result.isLoading()) {
                String message = result.getError() != null ? "Không thể nạp demo: " + result.getError().getMessage()
                        : "Đã thêm " + result.getValue() + " bản ghi. Tìm DEMO để xem dữ liệu mẫu.";
                demoViewModel.consumeResult();
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show();
            }
        });
        binding.btnHome.setOnClickListener(v -> activity.finish());
        binding.btnAdd.setOnClickListener(v -> activity.openFruitForm(0));
        binding.search.setText(viewModel.getSearch());
        binding.search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.setSearch(s.toString()); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        setupSpinner(binding.filter, new String[]{"Tất cả", "Đã hết hạn", "Hết hạn hôm nay", "Sắp hết hạn (1–3 ngày)", "Hết hàng", "Tồn thấp (1–5)"}, viewModel.getFilter(), viewModel::setFilter);
        setupSpinner(binding.sort, new String[]{"Tên A–Z", "Giá tăng dần", "Hạn gần nhất"}, viewModel.getSort(), viewModel::setSort);
        adapter = new FruitAdapter(id -> activity.openFruitDetail(id));
        binding.recyclerFruits.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFruits.setAdapter(adapter);
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
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

    private interface SelectionListener { void select(int position); }

    private void setupSpinner(android.widget.Spinner spinner, String[] labels, int selected, SelectionListener onSelected) {
        android.widget.ArrayAdapter<String> choices = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(choices); spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { onSelected.select(position); }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void render(FruitListViewModel.State state) {
        if (binding == null || state == null) return;
        boolean loading = state.status == FruitListViewModel.Status.LOADING;
        boolean empty = state.status == FruitListViewModel.Status.EMPTY;
        boolean error = state.status == FruitListViewModel.Status.ERROR;
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.recyclerFruits.setVisibility(state.status == FruitListViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        long quantity = 0; double value = 0;
        for (FruitListViewModel.Item item : state.items) {
            quantity += item.fruit.quantity;
            value += item.fruit.price * item.fruit.quantity;
        }
        binding.statistics.setText(error || loading ? "" : "Kết quả: " + state.items.size() + " loại · " + quantity
                + " sản phẩm · Giá trị tồn: " + java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN")).format(value) + " đ");
        adapter.submitList(state.items);
    }

    @Override public void onDestroyView() {
        if (expiryScheduler != null) expiryScheduler.stop();
        binding.recyclerFruits.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}
