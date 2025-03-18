package com.opentext;

import java.util.UUID;
import java.util.concurrent.*;

public class Main {

  public enum TaskType {
    READ,
    WRITE,
  }

  public interface TaskExecutor {
    <T> Future<T> submitTask(Task<T> task);
  }

  public record Task<T>(
    UUID taskUUID,
    TaskGroup taskGroup,
    TaskType taskType,
    Callable<T> taskAction
  ) {
    public Task {
      if (taskUUID == null || taskGroup == null || taskType == null || taskAction == null) {
        throw new IllegalArgumentException("All parameters must not be null");
      }
    }
  }

  public record TaskGroup(
    UUID groupUUID
  ) {
    public TaskGroup {
      if (groupUUID == null) {
        throw new IllegalArgumentException("All parameters must not be null");
      }
    }
  }

  public static class TaskExecutorService implements TaskExecutor {
    private final ExecutorService executorService;
    private final ConcurrentHashMap<UUID, BlockingQueue<Runnable>> taskGroups;
    private final int maxConcurrency;

    public TaskExecutorService(int maxConcurrency) {
      this.executorService = Executors.newFixedThreadPool(maxConcurrency);
      this.taskGroups = new ConcurrentHashMap<>();
      this.maxConcurrency = maxConcurrency;
    }

    @Override
    public <T> Future<T> submitTask(Task<T> task) {
      BlockingQueue<Runnable> groupQueue = taskGroups.computeIfAbsent(task.taskGroup().groupUUID(), k -> new LinkedBlockingQueue<>());
      TaskWrapper<T> taskWrapper = new TaskWrapper<>(task, groupQueue);
      groupQueue.add(taskWrapper);
      processNextTask(task.taskGroup().groupUUID());
      return taskWrapper.future;
    }

    private void processNextTask(UUID groupUUID) {
      BlockingQueue<Runnable> groupQueue = taskGroups.get(groupUUID);
      if (groupQueue != null) {
        Runnable nextTask = groupQueue.poll();
        if (nextTask != null) {
          executorService.submit(() -> {
            try {
              nextTask.run();
            } finally {
              processNextTask(groupUUID);
            }
          });
        }
      }
    }

    private static class TaskWrapper<T> implements Runnable {
      private final Task<T> task;
      private final BlockingQueue<Runnable> groupQueue;
      private final CompletableFuture<T> future;

      TaskWrapper(Task<T> task, BlockingQueue<Runnable> groupQueue) {
        this.task = task;
        this.groupQueue = groupQueue;
        this.future = new CompletableFuture<>();
      }

      @Override
      public void run() {
        try {
          T result = task.taskAction().call();
          future.complete(result);
        } catch (Exception e) {
          future.completeExceptionally(e);
        }
      }
    }
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    TaskExecutorService executorService = new TaskExecutorService(4);

    TaskGroup group1 = new TaskGroup(UUID.randomUUID());
    TaskGroup group2 = new TaskGroup(UUID.randomUUID());

    Task<String> task1 = new Task<>(UUID.randomUUID(), group1, TaskType.READ, () -> {
      Thread.sleep(1000);
      return "Task 1 completed";
    });

    Task<String> task2 = new Task<>(UUID.randomUUID(), group1, TaskType.WRITE, () -> {
      Thread.sleep(500);
      return "Task 2 completed";
    });

    Task<String> task3 = new Task<>(UUID.randomUUID(), group2, TaskType.READ, () -> {
      Thread.sleep(200);
      return "Task 3 completed";
    });

    Future<String> future1 = executorService.submitTask(task1);
    Future<String> future2 = executorService.submitTask(task2);
    Future<String> future3 = executorService.submitTask(task3);

    System.out.println(future1.get());
    System.out.println(future2.get());
    System.out.println(future3.get());
  }
}