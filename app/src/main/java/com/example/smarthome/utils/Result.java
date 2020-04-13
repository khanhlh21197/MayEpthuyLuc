package com.example.smarthome.utils;

public interface Result<T>  {
    void onFailure(String message);
    void onSuccess(T t, String message);
}
