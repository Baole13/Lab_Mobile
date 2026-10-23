package com.example.studentmanager.models;

public final class FruitResult<T> {
    private final T value;
    private final Exception error;
    private final boolean loading;

    private FruitResult(T value, Exception error, boolean loading) {
        this.value = value;
        this.error = error;
        this.loading = loading;
    }

    public static <T> FruitResult<T> loading() { return new FruitResult<>(null, null, true); }
    public static <T> FruitResult<T> success(T value) { return new FruitResult<>(value, null, false); }
    public static <T> FruitResult<T> failure(Exception error) { return new FruitResult<>(null, error, false); }
    public T getValue() { return value; }
    public Exception getError() { return error; }
    public boolean isSuccess() { return !loading && error == null; }
    public boolean isLoading() { return loading; }
}
