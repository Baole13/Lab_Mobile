package com.example.studentmanager.repositories;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import androidx.room.InvalidationTracker;
import com.example.studentmanager.models.FruitResult;
import com.example.studentmanager.dal.FruitDao;
import com.example.studentmanager.dal.AppDatabase;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.FruitEntity;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FruitRepository {
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile boolean databaseBootstrapped;
    private final FruitDao fruitDao;
    private final AppDatabase database;
    private final Context appContext;
    private final Set<ObservedFruitData<?>> activeQueries = new CopyOnWriteArraySet<>();

    public FruitRepository(Context context, AppDatabase database) {
        this.appContext = context.getApplicationContext();
        this.database = database;
        this.fruitDao = database.fruitDao();
    }

    public LiveData<FruitResult<List<FruitEntity>>> observeAll() {
        return observe(fruitDao::getAll);
    }

    public LiveData<FruitResult<FruitEntity>> observeById(int id) {
        return observe(() -> fruitDao.getById(id));
    }

    public LiveData<FruitResult<Long>> save(FruitEntity fruit) {
        return write(() -> {
            com.example.studentmanager.utils.FruitValidation.validate(fruit.name, fruit.category,
                    fruit.price, fruit.quantity, fruit.expiryDate);
            if (fruit.id == null) return fruitDao.insert(fruit);
            int changed = fruitDao.updateInformation(fruit.id, fruit.name, fruit.category,
                    fruit.price, fruit.expiryDate, fruit.description);
            if (changed == 0) throw new IllegalStateException("Trái cây đã bị xóa. Hãy tải lại.");
            return (long) fruit.id;
        });
    }

    public LiveData<FruitResult<Long>> delete(int id) {
        return write(() -> {
            if (fruitDao.deleteById(id) == 0) throw new IllegalStateException("Trái cây đã bị xóa.");
            return (long) id;
        });
    }

    private LiveData<FruitResult<Long>> write(Loader<Long> operation) {
        MutableLiveData<FruitResult<Long>> result = new MutableLiveData<>(FruitResult.loading());
        DATABASE_EXECUTOR.execute(() -> {
            try {
                com.example.studentmanager.utils.AuthManager auth = new com.example.studentmanager.utils.AuthManager(appContext);
                if (!auth.isLoggedIn() || !com.example.studentmanager.utils.AuthManager.ADMIN.equals(auth.getRole()))
                    throw new SecurityException("Chỉ Admin đã đăng nhập mới được thay đổi trái cây.");
                initializeSharedDatabase();
                long id = operation.load();
                result.postValue(FruitResult.success(id));
                refresh();
            } catch (Exception error) { result.postValue(FruitResult.failure(error)); }
        });
        return result;
    }

    public void refresh() {
        database.getInvalidationTracker().refreshVersionsAsync();
        for (ObservedFruitData<?> query : activeQueries) query.reload();
    }

    private <T> LiveData<FruitResult<T>> observe(Loader<T> loader) {
        return new ObservedFruitData<>(loader);
    }

    private final class ObservedFruitData<T> extends MutableLiveData<FruitResult<T>> {
        private final Loader<T> loader;
        private final InvalidationTracker.Observer observer = new InvalidationTracker.Observer("Fruit") {
            @Override public void onInvalidated(Set<String> tables) { reload(); }
        };
        private final ContentObserver helperObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange) { reload(); }
        };
        private volatile boolean active;
        private volatile boolean roomObserverRegistered;

        ObservedFruitData(Loader<T> loader) { this.loader = loader; }

        @Override protected void onActive() {
            active = true;
            activeQueries.add(this);
            DBHelper.registerFruitObserver(helperObserver);
            reload();
        }

        @Override protected void onInactive() {
            active = false;
            synchronized (this) {
                if (roomObserverRegistered) {
                    database.getInvalidationTracker().removeObserver(observer);
                    roomObserverRegistered = false;
                }
            }
            DBHelper.unregisterFruitObserver(helperObserver);
            activeQueries.remove(this);
        }

        void reload() {
            postValue(FruitResult.loading());
            DATABASE_EXECUTOR.execute(() -> {
                try {
                    initializeSharedDatabase();
                    if (!active) return;
                    synchronized (this) {
                        if (!active) return;
                        if (!roomObserverRegistered) {
                            database.getInvalidationTracker().addObserver(observer);
                            roomObserverRegistered = true;
                        }
                    }
                    postValue(FruitResult.success(loader.load()));
                }
                catch (Exception error) { postValue(FruitResult.failure(error)); }
            });
        }
    }

    private void initializeSharedDatabase() {
        if (databaseBootstrapped) return;
        synchronized (FruitRepository.class) {
            if (databaseBootstrapped) return;
            // DBHelper creates the other shared tables before Room opens this same database file.
            DBHelper helper = new DBHelper(appContext);
            try { helper.getWritableDatabase(); }
            finally { helper.close(); }
            databaseBootstrapped = true;
        }
    }

    private interface Loader<T> { T load(); }
}
