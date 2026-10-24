package com.jbh.finance.application.async;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.async.vo.AsyncTask;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncTaskExecutorImpl implements AsyncTaskExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncTaskExecutorImpl.class);
  private static final long KEEP_ALIVE_SECONDS = 30L;
  private static final String WORKER_THREAD_NAME = "async-task-worker";

  private final ThreadLocal<Boolean> workerThreadFlag = new ThreadLocal<>();

  // One thread keeps the tasks of this instance in submit order. Data safety across instances comes
  // from the database lock, not from this executor.
  private final ThreadPoolExecutor executorService = createExecutor();

  private ThreadPoolExecutor createExecutor() {
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            1,
            1,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
              final Thread thread =
                  new Thread(
                      () -> {
                        workerThreadFlag.set(Boolean.TRUE);
                        runnable.run();
                      },
                      WORKER_THREAD_NAME);
              thread.setDaemon(true);
              return thread;
            });
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  @Override
  public <T> CompletableFuture<T> submitTask(final AsyncTask asyncTask, final Callable<T> task) {

    if (asyncTask.metadata() != null
        && !(asyncTask.metadata() instanceof HashMap<String, Object>)) {
      throw new IllegalArgumentException("Metadata must be a HashMap");
    }

    final CompletableFuture<T> future = new CompletableFuture<>();
    LOG.info("[START] task: " + asyncTask);
    final Instant start = Instant.now();
    executorService.submit(
        () -> {
          try {
            final T result = task.call();
            future.complete(result);
            final long durationMs = Duration.between(start, Instant.now()).toMillis();
            LOG.info("[COMPLETED] task " + asyncTask + " completed in " + durationMs + " ms");
          } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.error("Task was interrupted: {}", asyncTask.type(), exception);
            registerToDLQ(asyncTask, exception);
            future.completeExceptionally(exception);
          } catch (final GenericSpecificationException gse) {
            LOG.error(
                "Generic Specification Exception executing async task {}: {}",
                asyncTask.type(),
                gse.getMessage(),
                gse);
            if (asyncTask.metadata() != null) {
              asyncTask.metadata().put("exceptionMsg", gse.getMessage());
            }
            future.completeExceptionally(gse);
          } catch (final Exception exception) {
            LOG.error(
                "General exception executing async task {}: {}",
                asyncTask.type(),
                exception.getMessage(),
                exception);
            if (asyncTask.metadata() != null) {
              asyncTask.metadata().put("exceptionMsg", exception.getMessage());
            }
            registerToDLQ(asyncTask, exception);
            future.completeExceptionally(exception);
          }
        });

    return future;
  }

  @Override
  public void awaitIdle(final Duration timeout) {
    if (Boolean.TRUE.equals(workerThreadFlag.get())) {
      throw new IllegalStateException("awaitIdle cannot be called from the executor worker thread");
    }
    // One thread and FIFO order: when the sentinel has run, earlier tasks are finished.
    final Future<?> sentinel = executorService.submit(() -> {});
    try {
      sentinel.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
    } catch (final TimeoutException exception) {
      sentinel.cancel(false);
      throw new IllegalStateException(
          "Timed out after " + timeout + " waiting for pending async tasks", exception);
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for pending async tasks", exception);
    } catch (final ExecutionException exception) {
      throw new IllegalStateException("Unexpected failure waiting for pending async tasks", exception);
    }
  }

  // FIXME: Am I getting everything that I need to retry?
  private void registerToDLQ(final AsyncTask asyncTask, final Throwable throwable) {
    LOG.warn(
        "Registering async task {}-{} to DLQ {}",
        asyncTask.type(),
        asyncTask.metadata(),
        throwable.getMessage());
  }
}
