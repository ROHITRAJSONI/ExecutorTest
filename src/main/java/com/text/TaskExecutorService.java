package com.text;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

public class TaskExecutorService implements Main.TaskExecutor {
    static int capacity;
    static int currentCapacity;
    static LinkedBlockingQueue<Main.Task> linkedTaskBlockingQueue;
    private final Execution e;

    public TaskExecutorService(int capacity) {
        TaskExecutorService.capacity = capacity;
        currentCapacity = 0;
        linkedTaskBlockingQueue = new LinkedBlockingQueue<>();
        e = new Execution();
    }

    @Override
    public <T> Future<T> submitTask(Main.Task<T> task) {
        linkedTaskBlockingQueue.add(task);
        return e.executeMyMethod();
    }
}

class Execution implements Callable {
    Future executeMyMethod() {
        if (TaskExecutorService.currentCapacity < TaskExecutorService.capacity) {
            TaskExecutorService.currentCapacity++;
            FutureTask futureTask = new FutureTask<>(new Execution());
            Thread t = new Thread(futureTask);
            t.start();
            return futureTask;
        }
        return null;
    }

    @Override
    public Object call() throws Exception {
        while (true) {
            if (!TaskExecutorService.linkedTaskBlockingQueue.isEmpty()) {
                return TaskExecutorService.linkedTaskBlockingQueue.poll().taskAction().call();
            }
        }
    }
}
