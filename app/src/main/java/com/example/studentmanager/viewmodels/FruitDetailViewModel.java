package com.example.studentmanager.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.studentmanager.models.FruitEntity;
import com.example.studentmanager.models.FruitResult;
import com.example.studentmanager.repositories.FruitRepository;
import com.example.studentmanager.utils.ExpiryPolicy;

public final class FruitDetailViewModel extends ViewModel {
    public enum Status { LOADING, NOT_FOUND, CONTENT, ERROR }

    public static final class State {
        public final Status status;
        public final FruitEntity fruit;
        public final ExpiryPolicy.Status expiryStatus;
        State(Status status, FruitEntity fruit, ExpiryPolicy.Status expiryStatus) {
            this.status = status;
            this.fruit = fruit;
            this.expiryStatus = expiryStatus;
        }
    }

    private final LiveData<FruitResult<FruitEntity>> fruit;
    private final FruitRepository repository;
    private final MutableLiveData<String> today = new MutableLiveData<>(ExpiryPolicy.todayIsoDate());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private FruitEntity currentFruit;
    private boolean receivedFruit;

    public FruitDetailViewModel(FruitRepository repository, int fruitId) {
        this.repository = repository;
        fruit = repository.observeById(fruitId);
        state.setValue(new State(Status.LOADING, null, null));
        state.addSource(fruit, result -> {
            if (result == null || result.isLoading()) {
                state.setValue(new State(Status.LOADING, null, null));
            } else if (result.getError() != null) {
                receivedFruit = false;
                state.setValue(new State(Status.ERROR, null, null));
            } else {
                receivedFruit = true;
                currentFruit = result.getValue();
                publishState();
            }
        });
        state.addSource(today, ignored -> publishState());
    }

    public LiveData<State> getState() { return state; }
    public void refreshToday() { today.setValue(ExpiryPolicy.todayIsoDate()); }
    public void retry() { repository.refresh(); }

    private void publishState() {
        if (!receivedFruit) return;
        if (currentFruit == null) {
            state.setValue(new State(Status.NOT_FOUND, null, null));
            return;
        }
        String currentDay = today.getValue();
        if (currentDay == null) currentDay = ExpiryPolicy.todayIsoDate();
        state.setValue(new State(Status.CONTENT, currentFruit,
                ExpiryPolicy.evaluate(currentFruit.expiryDate, currentDay)));
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final FruitRepository repository;
        private final int fruitId;
        public Factory(FruitRepository repository, int fruitId) {
            this.repository = repository;
            this.fruitId = fruitId;
        }
        @SuppressWarnings("unchecked")
        @Override public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(FruitDetailViewModel.class)) {
                return (T) new FruitDetailViewModel(repository, fruitId);
            }
            throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
        }
    }
}
