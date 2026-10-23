package com.example.studentmanager.dal;

import java.util.Arrays;
import java.util.List;

/** CHANGE HERE when the official test gives different tables/columns. */
public final class Schema {
    private Schema() {}
    public static final int DB_VERSION = 1; // BUMP when schema changes on an existing install.
    public static final String STUDENT = "Student";
    public static final String TEACHER = "Teacher";
    public static final String SCORE = "Score";

    public static final int TEXT = 0, INTEGER = 1, DECIMAL = 2;
    public static final class Field {
        public final String name, label;
        public final int type;
        public final boolean required;
        public Field(String name, String label, int type, boolean required) {
            this.name = name; this.label = label; this.type = type; this.required = required;
        }
    }
    public static final class Table {
        public final String name, title;
        public final List<Field> fields;
        public Table(String name, String title, Field... fields) {
            this.name = name; this.title = title; this.fields = Arrays.asList(fields);
        }
    }
    public static final Table STUDENTS = new Table(STUDENT, "Students",
        new Field("code", "Student code", TEXT, true),
        new Field("fullname", "Full name", TEXT, true),
        new Field("email", "Email", TEXT, false),
        new Field("accountId", "Account ID (optional)", INTEGER, false));
    // The sample Score.teacherCode requires Teacher.code; this fixes that mismatch.
    public static final Table TEACHERS = new Table(TEACHER, "Teachers",
        new Field("code", "Teacher code", TEXT, true),
        new Field("fullname", "Full name", TEXT, true),
        new Field("email", "Email", TEXT, false),
        new Field("accountId", "Account ID (optional)", INTEGER, false));
    public static final Table SCORES = new Table(SCORE, "Scores",
        new Field("studentCode", "Student code", TEXT, true),
        new Field("teacherCode", "Teacher code", TEXT, true),
        new Field("subject", "Subject", TEXT, true),
        new Field("score", "Score (0 to 10)", DECIMAL, true));

    public static Table find(String name) {
        for (Table t : Arrays.asList(STUDENTS, TEACHERS, SCORES))
            if (t.name.equals(name)) return t;
        return null; // whitelist unknown table names
    }

    public static final String[] CREATE_SQL = {
        "CREATE TABLE Account (id INTEGER PRIMARY KEY, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL)",
        "CREATE TABLE Student (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, fullname TEXT NOT NULL, email TEXT, accountId INTEGER, FOREIGN KEY(accountId) REFERENCES Account(id))",
        "CREATE TABLE Teacher (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, fullname TEXT NOT NULL, email TEXT, accountId INTEGER, FOREIGN KEY(accountId) REFERENCES Account(id))",
        "CREATE TABLE Score (id INTEGER PRIMARY KEY AUTOINCREMENT, studentCode TEXT NOT NULL, teacherCode TEXT NOT NULL, subject TEXT NOT NULL, score REAL NOT NULL CHECK(score >= 0 AND score <= 10), FOREIGN KEY(studentCode) REFERENCES Student(code), FOREIGN KEY(teacherCode) REFERENCES Teacher(code))"
    };
};
