package com.localhost_abuse.stealthapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class RequestLogger {
    private static final RequestLogger instance = new RequestLogger();
    private final MutableLiveData<String> logLiveData = new MutableLiveData<>();

    private RequestLogger() {}

    public static RequestLogger getInstance() {
        return instance;
    }

    public LiveData<String> getLogLiveData() {
        return logLiveData;
    }

    public void logRequest(String logEntry) {
        logLiveData.postValue(logEntry);
    }
}
