package com.techno.waterpressure.utils;

public interface Result<T>  {
    void onFailure(String message);
    void onSuccess(T t, String message);
}
