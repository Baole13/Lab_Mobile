package com.example.studentmanager.repositories;

import android.content.Context;
import android.content.ContentValues;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.studentmanager.dal.AppDatabase;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.models.FruitEntity;
import com.example.studentmanager.models.FruitResult;
import com.example.studentmanager.utils.AuthManager;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FruitRepositoryTest {
    private Context context;
    private AppDatabase database;
    private DBHelper helper;
    private FruitRepository repository;
    private AuthManager auth;

    @Before public void prepare() {
        // Tách tên database và preferences; không đụng dữ liệu/phiên đăng nhập của app.
        context = new android.content.ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public java.io.File getDatabasePath(String name) {
                return getBaseContext().getDatabasePath("fruit_management_test_" + name);
            }
            @Override public boolean deleteDatabase(String name) {
                return getBaseContext().deleteDatabase("fruit_management_test_" + name);
            }
            @Override public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                return getBaseContext().getSharedPreferences("fruit_management_test_" + name, mode);
            }
        };
        assertNotEquals(context.getDatabasePath("test2.db"),
                InstrumentationRegistry.getInstrumentation().getTargetContext().getDatabasePath("test2.db"));
        context.deleteDatabase("test2.db");
        helper = new DBHelper(context); helper.getWritableDatabase();
        database = Room.databaseBuilder(context, AppDatabase.class, "test2.db").build();
        repository = new FruitRepository(context, database);
        auth = new AuthManager(context); assertTrue(auth.login("admin", "123456"));
    }
    @After public void cleanup() {
        if (database != null) database.close();
        if (helper != null) helper.close();
        if (auth != null) auth.logout();
        context.deleteDatabase("test2.db");
    }
    private FruitEntity fruit() {
        FruitEntity fruit = new FruitEntity();
        fruit.name = "Táo"; fruit.category = "Nội địa"; fruit.price = 45000;
        fruit.quantity = 10; fruit.expiryDate = "2026-10-26";
        return fruit;
    }
    @Test public void crudPreservesConcurrentStock() throws Exception {
        FruitEntity fruit = fruit();
        FruitResult<Long> added = await(repository.save(fruit));
        assertNull(added.getError()); fruit.id = added.getValue().intValue();
        assertEquals(10, database.fruitDao().getById(fruit.id).quantity);
        ContentValues stock = new ContentValues(); stock.put("quantity", 7);
        helper.update("Fruit", fruit.id, stock);
        fruit.name = "Táo mới"; // Form cũ vẫn giữ quantity=10.
        assertNull(await(repository.save(fruit)).getError());
        FruitEntity updated = database.fruitDao().getById(fruit.id);
        assertEquals(7, updated.quantity); assertEquals("Táo mới", updated.name);
        assertNull(await(repository.delete(fruit.id)).getError());
        assertNull(database.fruitDao().getById(fruit.id));
        assertNotNull(await(repository.save(fruit)).getError());
    }
    @Test public void rejectsUnauthorizedAndInvalidWrites() throws Exception {
        FruitEntity fruit = fruit(); fruit.price = Double.NaN;
        assertNotNull(await(repository.save(fruit)).getError());
        assertTrue(database.fruitDao().getAll().isEmpty());
        fruit.price = 1;
        long id = await(repository.save(fruit)).getValue();
        fruit.id = (int) id;
        assertTrue(auth.login("customer", "123456"));
        assertTrue(await(repository.save(fruit)).getError() instanceof SecurityException);
        assertTrue(await(repository.delete((int) id)).getError() instanceof SecurityException);
        assertNotNull(database.fruitDao().getById((int) id));
        auth.logout();
        assertTrue(await(repository.save(fruit())).getError() instanceof SecurityException);
    }
    @Test public void preventsDoubleSaveInViewModel() throws Exception {
        AtomicReference<com.example.studentmanager.viewmodels.FruitEditorViewModel> editor = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            editor.set(new com.example.studentmanager.viewmodels.FruitEditorViewModel(repository, 0));
            editor.get().save(fruit()); editor.get().save(fruit());
        });
        assertNull(await(editor.get().getOperation()).getError());
        assertEquals(1, database.fruitDao().getAll().size());
    }

    private <T> FruitResult<T> await(LiveData<FruitResult<T>> data) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<FruitResult<T>> value = new AtomicReference<>();
        Observer<FruitResult<T>> observer = result -> {
            if (result != null && !result.isLoading()) { value.set(result); latch.countDown(); }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.observeForever(observer));
        try { assertTrue("Timed out waiting for database", latch.await(10, TimeUnit.SECONDS)); return value.get(); }
        finally { InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.removeObserver(observer)); }
    }
}
