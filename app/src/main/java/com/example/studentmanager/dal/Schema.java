package com.example.studentmanager.dal;

import java.util.Arrays;
import java.util.List;

/**
 * Central schema metadata and SQL statements for Fruit Shop Management.
 */
public final class Schema {
    private Schema() {}

    public static final String DB_NAME = "fruit_shop.db";
    public static final int DB_VERSION = 1;

    // Table names
    public static final String ACCOUNT = "Account";
    public static final String CUSTOMER = "Customer";
    public static final String FRUIT = "Fruit";
    public static final String ORDER = "Order";

    // Legacy aliases for template compatibility
    public static final String STUDENT = CUSTOMER;
    public static final String TEACHER = FRUIT;
    public static final String SCORE = ORDER;

    // Roles
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_CUSTOMER = "Customer";

    public static final int TEXT = 0, INTEGER = 1, DECIMAL = 2;

    public static final class Field {
        public final String name, label;
        public final int type;
        public final boolean required;
        public Field(String name, String label, int type, boolean required) {
            this.name = name;
            this.label = label;
            this.type = type;
            this.required = required;
        }
    }

    public static final class Table {
        public final String name, title;
        public final List<Field> fields;
        public Table(String name, String title, Field... fields) {
            this.name = name;
            this.title = title;
            this.fields = Arrays.asList(fields);
        }
    }

    public static final Table ACCOUNTS = new Table(ACCOUNT, "Accounts",
        new Field("username", "Username", TEXT, true),
        new Field("password", "Password", TEXT, true),
        new Field("role", "Role (Admin/Customer)", TEXT, true));

    public static final Table CUSTOMERS = new Table(CUSTOMER, "Customers",
        new Field("fullname", "Full Name", TEXT, true),
        new Field("phone", "Phone Number", TEXT, true),
        new Field("email", "Email Address", TEXT, false),
        new Field("accountId", "Account ID", INTEGER, true));

    public static final Table FRUITS = new Table(FRUIT, "Fruits",
        new Field("name", "Fruit Name", TEXT, true),
        new Field("category", "Category", TEXT, true),
        new Field("price", "Price (VND)", DECIMAL, true),
        new Field("quantity", "Stock Quantity", INTEGER, true),
        new Field("expiryDate", "Expiry Date (YYYY-MM-DD)", TEXT, true),
        new Field("description", "Description", TEXT, false));

    public static final Table ORDERS = new Table(ORDER, "Orders",
        new Field("customerId", "Customer ID", INTEGER, true),
        new Field("orderDate", "Order Date", TEXT, true),
        new Field("totalAmount", "Total Amount", DECIMAL, true),
        new Field("status", "Status (Pending/Confirmed/Completed)", TEXT, true));

    public static Table find(String name) {
        if (name == null) return null;
        for (Table t : Arrays.asList(ACCOUNTS, CUSTOMERS, FRUITS, ORDERS)) {
            if (t.name.equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    public static final String[] CREATE_SQL = {
        "CREATE TABLE IF NOT EXISTS Account (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT NOT NULL UNIQUE, " +
            "password TEXT NOT NULL, " +
            "role TEXT NOT NULL" +
        ");",

        "CREATE TABLE IF NOT EXISTS Customer (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "fullname TEXT NOT NULL, " +
            "phone TEXT NOT NULL, " +
            "email TEXT, " +
            "accountId INTEGER NOT NULL, " +
            "FOREIGN KEY(accountId) REFERENCES Account(id) ON DELETE CASCADE" +
        ");",

        "CREATE TABLE IF NOT EXISTS Fruit (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "category TEXT NOT NULL, " +
            "price REAL NOT NULL CHECK(price > 0), " +
            "quantity INTEGER NOT NULL CHECK(quantity >= 0), " +
            "expiryDate TEXT NOT NULL, " +
            "description TEXT" +
        ");",

        "CREATE TABLE IF NOT EXISTS \"Order\" (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "customerId INTEGER NOT NULL, " +
            "orderDate TEXT NOT NULL, " +
            "totalAmount REAL NOT NULL, " +
            "status TEXT NOT NULL, " +
            "FOREIGN KEY(customerId) REFERENCES Customer(id) ON DELETE CASCADE" +
        ");"
    };
}
