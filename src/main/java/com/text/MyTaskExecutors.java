package com.text;

public class MyTaskExecutors {

    static Main.TaskExecutor myNewFixedThreadPool(int capacity) {

        return new TaskExecutorService(capacity);
    }
}
