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
import java.util.ArrayList;
import java.util.List;

public final class FruitListViewModel extends ViewModel {
    public enum Status { LOADING, EMPTY, CONTENT, ERROR }

    public static final class Item {
        public final FruitEntity fruit;
        public final ExpiryPolicy.Status expiryStatus;
        Item(FruitEntity fruit, ExpiryPolicy.Status expiryStatus) {
            this.fruit = fruit;
            this.expiryStatus = expiryStatus;
        }
    }

    public static final class State {
        public final Status status;
        public final List<Item> items;
        State(Status status, List<Item> items) { this.status = status; this.items = items; }
    }

    private final FruitRepository repository;
    private final LiveData<FruitResult<List<FruitEntity>>> fruits;
    private final MutableLiveData<String> today = new MutableLiveData<>(ExpiryPolicy.todayIsoDate());
    private final MediatorLiveData<State> state = new MediatorLiveData<>();
    private List<FruitEntity> currentFruits = new ArrayList<>();
    private boolean hasLoaded;
    private String search = "";
    private int filter;
    private int sort;
    public void setSearch(String value) { search = value; publishState(); }
    public void setFilter(int value) { filter = value; publishState(); }
    public void setSort(int value) { sort = value; publishState(); }
    public String getSearch() { return search; }
    public int getFilter() { return filter; }
    public int getSort() { return sort; }

    public FruitListViewModel(FruitRepository repository) {
        this.repository = repository;
        fruits = repository.observeAll();
        state.setValue(new State(Status.LOADING, new ArrayList<>()));
        state.addSource(fruits, result -> {
            if (result == null || result.isLoading()) {
                hasLoaded = false;
                state.setValue(new State(Status.LOADING, new ArrayList<>()));
            } else if (result.getError() != null) {
                hasLoaded = false;
                state.setValue(new State(Status.ERROR, new ArrayList<>()));
            } else {
                hasLoaded = true;
                currentFruits = result.getValue() == null ? new ArrayList<>() : result.getValue();
                publishState();
            }
        });
        state.addSource(today, ignored -> publishState());
    }

    public LiveData<State> getState() { return state; }
    public void refreshToday() { today.setValue(ExpiryPolicy.todayIsoDate()); }
    public void retry() {
        hasLoaded = false;
        state.setValue(new State(Status.LOADING, new ArrayList<>()));
        repository.refresh();
    }

    private void publishState() {
        if (!hasLoaded) return;
        String currentDay = today.getValue();
        if (currentDay == null) currentDay = ExpiryPolicy.todayIsoDate();
        List<Item> items = new ArrayList<>(currentFruits.size());
        for (FruitEntity fruit : currentFruits) {
            ExpiryPolicy.Status expiry = ExpiryPolicy.evaluate(fruit.expiryDate, currentDay);
            if (com.example.studentmanager.utils.FruitListPolicy.matches(fruit.name, fruit.category,
                    fruit.quantity, expiry, search, filter)) items.add(new Item(fruit, expiry));
        }
        java.util.Collections.sort(items, (first, second) -> {
            int comparison;
            if (sort == 1) comparison = Double.compare(first.fruit.price, second.fruit.price);
            else if (sort == 2) comparison = first.fruit.expiryDate.compareTo(second.fruit.expiryDate);
            else comparison = first.fruit.name.compareToIgnoreCase(second.fruit.name);
            return comparison == 0 ? Integer.compare(first.fruit.id, second.fruit.id) : comparison;
        });
        state.setValue(new State(items.isEmpty() ? Status.EMPTY : Status.CONTENT, items));
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final FruitRepository repository;
        public Factory(FruitRepository repository) { this.repository = repository; }
        @SuppressWarnings("unchecked")
        @Override public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(FruitListViewModel.class)) {
                return (T) new FruitListViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
        }
    }
}
