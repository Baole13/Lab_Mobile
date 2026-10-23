package com.example.studentmanager.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.studentmanager.activities.FruitManagementActivity;
import com.example.studentmanager.databinding.FragmentFruitFormBinding;
import com.example.studentmanager.models.FruitEntity;
import com.example.studentmanager.utils.FruitValidation;
import com.example.studentmanager.viewmodels.FruitEditorViewModel;

public final class FruitFormFragment extends Fragment {
    private FragmentFruitFormBinding binding;
    private FruitEditorViewModel viewModel;
    private boolean populated;
    private int fruitId;

    public static FruitFormFragment newInstance(int id) {
        FruitFormFragment fragment = new FruitFormFragment();
        Bundle args = new Bundle(); args.putInt("fruit_id", id); fragment.setArguments(args);
        return fragment;
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        binding = FragmentFruitFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(View view, Bundle state) {
        fruitId = requireArguments().getInt("fruit_id");
        populated = state != null && state.getBoolean("populated");
        FruitManagementActivity activity = (FruitManagementActivity) requireActivity();
        viewModel = new ViewModelProvider(this, new FruitEditorViewModel.Factory(activity.getFruitRepository(), fruitId)).get(FruitEditorViewModel.class);
        binding.tvTitle.setText(fruitId == 0 ? "Thêm trái cây" : "Sửa thông tin trái cây");
        // Tồn kho chỉ nhập khi thêm; sửa thông tin không được ghi đè số lượng của đơn hàng.
        binding.quantity.setEnabled(fruitId == 0);
        binding.btnCancel.setOnClickListener(v -> activity.getSupportFragmentManager().popBackStack());
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        binding.btnSave.setEnabled(false);
        binding.btnSave.setOnClickListener(v -> save());
        viewModel.getFruit().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.progressLoading.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            boolean ready = result.isSuccess() && result.getValue() != null;
            binding.btnSave.setEnabled(ready && !viewModel.isBusy());
            binding.btnRetry.setVisibility(result.getError() != null ? View.VISIBLE : View.GONE);
            if (result.getError() != null) binding.tvError.setText("Không thể tải trái cây. Hãy thử lại.");
            else if (result.isSuccess() && result.getValue() == null) binding.tvError.setText("Trái cây đã bị xóa.");
            if (ready && !populated) {
                FruitEntity fruit = result.getValue();
                binding.name.setText(fruit.name); binding.category.setText(fruit.category);
                binding.price.setText(fruitId == 0 ? "" : String.valueOf(fruit.price));
                binding.quantity.setText(String.valueOf(fruit.quantity));
                binding.expiryDate.setText(fruit.expiryDate); binding.description.setText(fruit.description);
                populated = true;
            }
        });
        viewModel.getOperation().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.btnSave.setEnabled(!result.isLoading());
            binding.btnCancel.setEnabled(!result.isLoading());
            binding.progressLoading.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            if (result.getError() != null) {
                binding.tvError.setText(result.getError().getMessage());
                viewModel.consumeResult();
            } else if (result.isSuccess()) {
                viewModel.consumeResult();
                Toast.makeText(requireContext(), "Đã lưu trái cây", Toast.LENGTH_SHORT).show();
                activity.getSupportFragmentManager().popBackStack();
            }
        });
    }
    private void save() {
        try {
            FruitEntity fruit = new FruitEntity(); fruit.id = fruitId == 0 ? null : fruitId;
            fruit.name = binding.name.getText().toString().trim();
            fruit.category = binding.category.getText().toString().trim();
            fruit.price = Double.parseDouble(binding.price.getText().toString().trim().replace(',', '.'));
            fruit.quantity = Integer.parseInt(binding.quantity.getText().toString().trim());
            fruit.expiryDate = binding.expiryDate.getText().toString().trim();
            fruit.description = binding.description.getText().toString().trim();
            FruitValidation.validate(fruit.name, fruit.category, fruit.price, fruit.quantity, fruit.expiryDate);
            binding.tvError.setText(""); viewModel.save(fruit);
        } catch (NumberFormatException error) {
            binding.tvError.setText("Giá phải là số; số lượng phải là số nguyên từ 0 đến 2147483647. Không nhập dấu phân cách hàng nghìn.");
        } catch (IllegalArgumentException error) { binding.tvError.setText(error.getMessage()); }
    }
    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putBoolean("populated", populated);
    }
    @Override public void onDestroyView() { binding = null; super.onDestroyView(); }
}
