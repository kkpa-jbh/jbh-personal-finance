package com.jbh.account.application.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.jbh.account.application.async.vo.AsyncTask;
import com.jbh.account.application.async.vo.AsyncTaskType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(1) // Run first or Integer.MAX_VALUE
@DisplayName("AsyncTaskExecutorImpl Tests")
public class AsyncTaskExecutorImplTest {

  private AsyncTaskExecutorImpl asyncTaskExecutor;
  private AsyncTask testAsyncTask;

  @BeforeEach
  void setUp() {
    asyncTaskExecutor = new AsyncTaskExecutorImpl();
    testAsyncTask =
        new AsyncTask(AsyncTaskType.TEST_TASK, new HashMap<>(Map.of("testKey", "testValue")));
  }

  @Test
  @DisplayName("Should successfully execute async task and return result")
  void shouldExecuteAsyncTaskSuccessfully() throws Exception {
    // Given
    final String expectedResult = "Task completed successfully";

    // When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(
            testAsyncTask,
            () -> {
              // Simulate some work
              Thread.sleep(100);
              return expectedResult;
            });

    // Then
    assertNotNull(future);
    final String actualResult = future.get(5, TimeUnit.SECONDS);
    assertEquals(expectedResult, actualResult);
    assertTrue(future.isDone());
    assertTrue(!future.isCancelled());
  }

  @Test
  @DisplayName("Should execute task with different return types")
  void shouldHandleDifferentReturnTypes() throws Exception {
    // Test Integer return type
    final CompletableFuture<Integer> intFuture =
        asyncTaskExecutor.submitTask(testAsyncTask, () -> 42);
    assertEquals(42, intFuture.get(2, TimeUnit.SECONDS));

    // Test Boolean return type
    final CompletableFuture<Boolean> booleanFuture =
        asyncTaskExecutor.submitTask(testAsyncTask, () -> true);
    assertTrue(booleanFuture.get(2, TimeUnit.SECONDS));

    // Test null return type
    final CompletableFuture<String> nullFuture =
        asyncTaskExecutor.submitTask(testAsyncTask, () -> null);
    assertEquals(null, nullFuture.get(2, TimeUnit.SECONDS));
  }

  @DisplayName("Should handle runtime exceptions and complete future exceptionally")
  void shouldHandleRuntimeExceptions() throws Exception {
    // Given
    final RuntimeException expectedException = new RuntimeException("Test runtime exception");

    // When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(
            testAsyncTask,
            () -> {
              throw expectedException;
            });

    // Then - Use exceptionally to verify the exception
    final CompletableFuture<String> result =
        future.exceptionally(
            throwable -> {
              assertEquals(expectedException, throwable);
              return "exception_handled";
            });

    // Wait for completion
    final String finalResult = result.get(5, TimeUnit.SECONDS);
    assertEquals("exception_handled", finalResult);
    assertTrue(future.isCompletedExceptionally());
  }

  @Test
  @DisplayName("Should handle checked exceptions and complete future exceptionally")
  void shouldHandleCheckedExceptions() throws InterruptedException {
    // Given - Create a task with mutable metadata map
    final Map<String, Object> mutableMetadata = new java.util.HashMap<>();
    mutableMetadata.put("testKeyCheckedExceptions", "testValue");
    final AsyncTask taskWithMutableMetadata =
        new AsyncTask(AsyncTaskType.TEST_TASK, mutableMetadata);
    final String exceptionMessage = "Test checked exception";
    final Exception expectedException = new Exception(exceptionMessage);

    // When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(
            taskWithMutableMetadata,
            () -> {
              throw expectedException;
            });

    // Then
    final ExecutionException executionException =
        assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));

    assertTrue(future.isCompletedExceptionally());
    assertEquals(expectedException, executionException.getCause());

    // Verify metadata was updated for checked exception
    assertTrue(taskWithMutableMetadata.metadata().containsKey("exceptionMsg"));
    assertEquals(exceptionMessage, taskWithMutableMetadata.metadata().get("exceptionMsg"));
  }

  @Test
  @DisplayName("Should handle InterruptedException properly")
  void shouldHandleInterruptedException() {
    // Given/When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(
            testAsyncTask,
            () -> {
              Thread.currentThread().interrupt();
              throw new InterruptedException("Thread was interrupted");
            });

    // Then
    final ExecutionException executionException =
        assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));

    assertTrue(future.isCompletedExceptionally());
    assertTrue(executionException.getCause() instanceof InterruptedException);
  }

  @Test
  @DisplayName("Should add exception message to task metadata when exception occurs")
  void shouldAddExceptionMessageToMetadata() throws InterruptedException {
    // Given - Create a task with mutable metadata map
    final Map<String, Object> mutableMetadata = new java.util.HashMap<>();
    mutableMetadata.put("testKeyExceptionMetadata", "testValue");
    final AsyncTask taskWithMutableMetadata =
        new AsyncTask(AsyncTaskType.TEST_TASK, mutableMetadata);
    final String exceptionMessage = "Test exception for metadata";
    final RuntimeException testException = new RuntimeException(exceptionMessage);

    // When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(
            taskWithMutableMetadata,
            () -> {
              throw testException;
            });

    // Then
    try {
      future.get(2, TimeUnit.SECONDS);
      fail("Expected ExecutionException");
    } catch (final ExecutionException e) {
      // Exception is expected
    } catch (final TimeoutException e) {
      throw new RuntimeException(e);
    }

    // Verify metadata was updated
    assertTrue(taskWithMutableMetadata.metadata().containsKey("exceptionMsg"));
    assertEquals(exceptionMessage, taskWithMutableMetadata.metadata().get("exceptionMsg"));
  }

  @Test
  @DisplayName("Should execute multiple tasks concurrently")
  void shouldExecuteMultipleTasksConcurrently() throws Exception {
    // Given
    final int numberOfTasks = 3;
    final java.util.List<CompletableFuture<Integer>> futures = new java.util.ArrayList<>();

    // When - Submit multiple tasks
    for (int i = 0; i < numberOfTasks; i++) {
      final int taskNumber = i;
      final CompletableFuture<Integer> future =
          asyncTaskExecutor.submitTask(
              new AsyncTask(
                  AsyncTaskType.TEST_TASK, new HashMap<>(Map.of("taskNumber", taskNumber))),
              () -> {
                Thread.sleep(50); // Simulate work
                return taskNumber * 2;
              });
      futures.add(future);
    }

    // Then - All tasks should complete successfully
    for (int i = 0; i < numberOfTasks; i++) {
      final Integer result = futures.get(i).get(5, TimeUnit.SECONDS);
      assertEquals(i * 2, result);
      assertTrue(futures.get(i).isDone());
    }
  }

  @Test
  @DisplayName("Should handle task with null metadata")
  void shouldHandleTaskWithNullMetadata() throws Exception {
    // Given
    final AsyncTask taskWithNullMetadata = new AsyncTask(AsyncTaskType.TEST_TASK, null);

    // When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(taskWithNullMetadata, () -> "Success with null metadata");

    // Then
    final String result = future.get(2, TimeUnit.SECONDS);
    assertEquals("Success with null metadata", result);
  }

  @Test
  @DisplayName("Should handle exception with null metadata gracefully")
  void shouldHandleExceptionWithNullMetadata() {
    // Given
    final AsyncTask taskWithNullMetadata = new AsyncTask(AsyncTaskType.TEST_TASK, null);

    // When
    final CompletableFuture<String> future =
        asyncTaskExecutor.submitTask(
            taskWithNullMetadata,
            () -> {
              throw new RuntimeException("Exception with null metadata");
            });

    // Then
    final ExecutionException executionException =
        assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));

    assertTrue(future.isCompletedExceptionally());
    assertTrue(executionException.getCause() instanceof RuntimeException);
  }

  @Test
  @DisplayName("Should execute long-running task successfully")
  void shouldExecuteLongRunningTask() throws Exception {
    // Given
    final long startTime = System.currentTimeMillis();

    // When
    final CompletableFuture<Long> future =
        asyncTaskExecutor.submitTask(
            testAsyncTask,
            () -> {
              Thread.sleep(500); // Simulate longer work
              return System.currentTimeMillis();
            });

    // Then
    final Long endTime = future.get(10, TimeUnit.SECONDS);
    assertTrue(endTime - startTime >= 500);
    assertTrue(future.isDone());
  }
}
