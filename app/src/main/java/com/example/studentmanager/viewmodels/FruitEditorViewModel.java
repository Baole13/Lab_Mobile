package com.example.studentmanager.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.studentmanager.models.FruitEntity;
import com.example.studentmanager.models.FruitResult;
import com.example.studentmanager.repositories.FruitRepository;

public final class FruitEditorViewModel extends ViewModel {
    private final FruitRepository repository;
    private final LiveData<FruitResult<FruitEntity>> fruit;
    private final MediatorLiveData<FruitResult<Long>> operation = new MediatorLiveData<>();
    private boolean busy;

    public FruitEditorViewModel(FruitRepository repository, int id) {
        this.repository = repository;
        fruit = id == 0 ? new androidx.lifecycle.MutableLiveData<>(FruitResult.success(new FruitEntity()))
                : repository.observeById(id);
    }
    public LiveData<FruitResult<FruitEntity>> getFruit() { return fruit; }
    public LiveData<FruitResult<Long>> getOperation() { return operation; }
    public boolean isBusy() { return busy; }
    public void retry() { repository.refresh(); }
    public void save(FruitEntity value) { if (!busy) track(repository.save(value)); }
    public void loadDemoData() { if (!busy) track(repository.loadDemoData()); }
    public void delete(int id) { if (!busy) track(repository.delete(id)); }
    public void consumeResult() { operation.setValue(null); }

    private void track(LiveData<FruitResult<Long>> source) {
        busy = true;
        operation.setValue(FruitResult.loading());
        operation.addSource(source, result -> {
            if (result != null && !result.isLoading()) {
                busy = false;
                operation.removeSource(source);
            }
            operation.setValue(result);
        });
    }
    public static final class Factory implements ViewModelProvider.Factory {
        private final FruitRepository repository;
        private final int id;
        public Factory(FruitRepository repository, int id) { this.repository = repository; this.id = id; }
        @Override public <T extends ViewModel> T create(Class<T> type) {
            if (!type.isAssignableFrom(FruitEditorViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel");
            return type.cast(new FruitEditorViewModel(repository, id));
        }
    }
}
