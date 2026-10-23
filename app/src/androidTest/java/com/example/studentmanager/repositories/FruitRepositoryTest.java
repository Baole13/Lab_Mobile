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
        assertNotEquals(context.getDatabasePath(com.example.studentmanager.dal.Schema.DB_NAME),
                InstrumentationRegistry.getInstrumentation().getTargetContext().getDatabasePath(com.example.studentmanager.dal.Schema.DB_NAME));
        context.deleteDatabase(com.example.studentmanager.dal.Schema.DB_NAME);
        helper = new DBHelper(context); helper.getWritableDatabase();
        database = Room.databaseBuilder(context, AppDatabase.class, com.example.studentmanager.dal.Schema.DB_NAME).build();
        helper.getWritableDatabase().delete("Fruit", null, null);
        repository = new FruitRepository(context, database);
        auth = new AuthManager(context); assertTrue(auth.login("admin", "123456"));
    }
    @After public void cleanup() {
        if (database != null) database.close();
        if (helper != null) helper.close();
        if (auth != null) auth.logout();
        context.deleteDatabase(com.example.studentmanager.dal.Schema.DB_NAME);
    }
    private FruitEntity fruit() {
        FruitEntity fruit = new FruitEntity();
        fruit.name = "Táo"; fruit.category = "Nội địa"; fruit.price = 45000;
        fruit.quantity = 10; fruit.expiryDate = "2099-12-31";
        return fruit;
    }
    @Test public void crudPreservesConcurrentStock() throws Exception {
        FruitEntity fruit = fruit();
        FruitResult<Long> added = await(repository.save(fruit));
        assertNull(added.getError()); fruit.id = added.getValue().intValue();
        assertEquals(10, database.fruitDao().getById(fruit.id).quantity);
        assertTrue(helper.reduceFruitQuantity(fruit.id, 3));
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

    @Test public void sampleDataCoversAllBoundariesAndDoesNotResetEdits() throws Exception {
        String today = "2026-10-23";
        int originalOrders = helper.getAllOrders().size();
        assertEquals(15, com.example.studentmanager.dal.DemoData.insert(helper.getWritableDatabase(), today));
        java.util.List<FruitEntity> fruits = database.fruitDao().getAll();
        assertEquals(10, fruits.size());
        int expired = 0, expiresToday = 0, soon = 0, outOfStock = 0, lowStock = 0;
        long quantity = 0; double value = 0;
        for (FruitEntity fruit : fruits) {
            com.example.studentmanager.utils.FruitValidation.validate(fruit.name, fruit.category, fruit.price, fruit.quantity, fruit.expiryDate);
            com.example.studentmanager.utils.ExpiryPolicy.Status status = com.example.studentmanager.utils.ExpiryPolicy.evaluate(fruit.expiryDate, today);
            if (status == com.example.studentmanager.utils.ExpiryPolicy.Status.EXPIRED) expired++;
            if (status == com.example.studentmanager.utils.ExpiryPolicy.Status.EXPIRES_TODAY) expiresToday++;
            if (status == com.example.studentmanager.utils.ExpiryPolicy.Status.EXPIRING_SOON) soon++;
            if (fruit.quantity == 0) outOfStock++;
            if (fruit.quantity > 0 && fruit.quantity <= 5) lowStock++;
            quantity += fruit.quantity; value += fruit.price * fruit.quantity;
        }
        assertEquals(1, expired); assertEquals(1, expiresToday); assertEquals(3, soon);
        assertEquals(1, outOfStock); assertEquals(4, lowStock);
        assertEquals(84, quantity); assertEquals(7210000.5, value, 0.001);
        assertTrue(auth.login("demo_customer", "123456"));
        int customerId = helper.getCustomerByAccountId(auth.getAccountId()).getId();
        assertEquals(3, helper.getOrdersByCustomer(customerId).size());
        assertTrue(auth.login("admin", "123456"));
        ContentValues changed = new ContentValues(); changed.put("quantity", 2);
        helper.update("Fruit", fruits.get(0).id, changed);
        assertEquals(0, com.example.studentmanager.dal.DemoData.insert(helper.getWritableDatabase(), today));
        assertEquals(2, database.fruitDao().getById(fruits.get(0).id).quantity);
        assertEquals(originalOrders + 3, helper.getAllOrders().size());
        assertEquals(13, com.example.studentmanager.dal.DemoData.insert(helper.getWritableDatabase(), "2026-10-24"));
        assertEquals(20, database.fruitDao().getAll().size());
    }

    @Test public void demoDataRequiresAdminAndPreservesExistingRows() throws Exception {
        FruitEntity existing = fruit();
        int id = await(repository.save(existing)).getValue().intValue();
        assertTrue(auth.login("customer", "123456"));
        assertTrue(await(repository.loadDemoData()).getError() instanceof SecurityException);
        assertEquals(1, database.fruitDao().getAll().size());
        assertTrue(auth.login("admin", "123456"));
        assertNull(await(repository.loadDemoData()).getError());
        assertEquals(11, database.fruitDao().getAll().size());
        assertEquals("Táo", database.fruitDao().getById(id).name);
    }

    @Test public void orderFailureRollsBackEveryStockChange() throws Exception {
        FruitEntity first = fruit(); first.quantity = 3;
        int firstId = await(repository.save(first)).getValue().intValue();
        FruitEntity second = fruit(); second.quantity = 1;
        int secondId = await(repository.save(second)).getValue().intValue();
        assertTrue(auth.login("customer", "123456"));
        int customerId = helper.getCustomerByAccountId(auth.getAccountId()).getId();
        int oldOrderCount = helper.getAllOrders().size();
        java.util.Map<Integer, Integer> quantities = new java.util.LinkedHashMap<>();
        quantities.put(firstId, 2); quantities.put(secondId, 2);
        try { helper.placeOrder(customerId, quantities, "2026-10-23 12:00", 180000); fail("Expected insufficient stock"); }
        catch (IllegalStateException expected) { }
        assertEquals(3, helper.getFruitById(firstId).quantity);
        assertEquals(1, helper.getFruitById(secondId).quantity);
        assertEquals(oldOrderCount, helper.getAllOrders().size());
        quantities.put(secondId, 1);
        try { helper.placeOrder(customerId, quantities, "2026-10-23 12:00", 1); fail("Expected changed price"); }
        catch (IllegalStateException expected) { }
        assertEquals(3, helper.getFruitById(firstId).quantity);
        long orderId = helper.placeOrder(customerId, quantities, "2026-10-23 12:00", 135000);
        assertTrue(orderId > 0);
        assertEquals(1, helper.getFruitById(firstId).quantity);
        assertEquals(0, helper.getFruitById(secondId).quantity);
        assertEquals(oldOrderCount + 1, helper.getAllOrders().size());
        assertFalse(helper.reduceFruitQuantity(secondId, 1));
        assertEquals(0, helper.getFruitById(secondId).quantity);
    }

    @Test public void invalidDatesAreNotAvailableAndMissingMigrationKeepsData() {
        com.example.studentmanager.models.Fruit fruit = new com.example.studentmanager.models.Fruit();
        fruit.quantity = 2; fruit.expiryDate = "2099-02-30";
        assertFalse(fruit.isAvailable());
        fruit.expiryDate = com.example.studentmanager.utils.ExpiryPolicy.todayIsoDate();
        assertTrue(fruit.isAvailable());
        try { helper.onUpgrade(helper.getWritableDatabase(), 1, 2); fail("Expected missing migration"); }
        catch (IllegalStateException expected) { }
        assertNotNull(helper.getAccountByUsername("admin"));
        assertFalse(helper.getAllOrders().isEmpty());
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
