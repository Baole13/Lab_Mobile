package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.AppDatabase;
import com.example.studentmanager.databinding.ActivityFruitManagementBinding;
import com.example.studentmanager.fragments.FruitDetailFragment;
import com.example.studentmanager.fragments.FruitListFragment;
import com.example.studentmanager.repositories.FruitRepository;
import com.example.studentmanager.utils.AuthManager;

public final class FruitManagementActivity extends AppCompatActivity {
    private ActivityFruitManagementBinding binding;
    private FruitRepository repository;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuthManager auth = new AuthManager(this);
        if (!auth.isLoggedIn() || !AuthManager.ADMIN.equals(auth.getRole())) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityFruitManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnHostRetry.setOnClickListener(view -> initializeList());
        if (state == null) initializeList();
    }

    private void initializeList() {
        binding.hostError.setVisibility(View.GONE);
        try {
            repository = new FruitRepository(this, AppDatabase.getInstance(getApplicationContext()));
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new FruitListFragment())
                    .commit();
        } catch (RuntimeException error) {
            binding.hostError.setVisibility(View.VISIBLE);
            binding.tvHostError.setText("Không thể mở dữ liệu trái cây. Hãy thử tải lại.");
        }
    }

    public FruitRepository getFruitRepository() {
        if (repository == null) repository = new FruitRepository(this, AppDatabase.getInstance(getApplicationContext()));
        return repository;
    }

    public void openFruitForm(int id) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, com.example.studentmanager.fragments.FruitFormFragment.newInstance(id))
                .addToBackStack(null).commit();
    }

    public void openFruitDetail(int id) {
        Fragment detail = FruitDetailFragment.newInstance(id);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, detail)
                .addToBackStack(null)
                .commit();
    }
}
