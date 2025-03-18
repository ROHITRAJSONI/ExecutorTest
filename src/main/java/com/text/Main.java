package com.text;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

  /**
   * Enumeration of task types.
   */
  public enum TaskType {
    READ,
    WRITE,
  }

  /**
   * Representation of computation to be performed by the {@link TaskExecutor}.
   *
   * @param taskUUID Unique task identifier.
   * @param taskGroup Task group.
   * @param taskType Task type.
   * @param taskAction Callable representing task computation and returning the result.
   * @param <T> Task computation result value type.
   */
  public record Task<T>(
    UUID taskUUID,
    TaskGroup taskGroup,
    TaskType taskType,
    Callable<T> taskAction) {
    public Task {
      if (taskUUID == null || taskGroup == null || taskType == null || taskAction == null) {
        throw new IllegalArgumentException("All parameters must not be null");
      }
    }
  }

  /**
   * Task group.
   *
   * @param groupUUID Unique group identifier.
   */
  public record TaskGroup(UUID groupUUID) {
    public TaskGroup {
      if (groupUUID == null) {
        throw new IllegalArgumentException("All parameters must not be null");
      }
    }
  }

  public interface TaskExecutor {
    /**
     * Submit new task to be queued and executed.
     *
     * @param task Task to be executed by the executor. Must not be null.
     * @return Future for the task asynchronous computation result.
     */
    <T> Future<T> submitTask(Main.Task<T> task);
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    TaskExecutor taskExecutor = MyTaskExecutors.myNewFixedThreadPool(3);
    UUID taskUuid = UUID.randomUUID();
    UUID groupUuid = UUID.randomUUID();
    Task<String> task = new Task<>(taskUuid, new TaskGroup(groupUuid), TaskType.READ, () -> "Hello World");
    Task<String> task1 = new Task<>(taskUuid, new TaskGroup(groupUuid), TaskType.READ, () -> "Hello World1");
    Task<String> task2 = new Task<>(taskUuid, new TaskGroup(groupUuid), TaskType.READ, () -> "Hello World2");
    Future<String> stringFuture = taskExecutor.submitTask(task);
    Future<String> stringFuture1 = taskExecutor.submitTask(task1);
    Future<String> stringFuture2 = taskExecutor.submitTask(task2);
    System.out.println(stringFuture.get());
    System.out.println(stringFuture1.get());
    System.out.println(stringFuture2.get());
  }
}

