package com.jbh.account.application.async;

import com.jbh.account.application.async.vo.AsyncTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncTaskExecutorImpl implements AsyncTaskExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncTaskExecutorImpl.class);

  private void registerToDLQ(final AsyncTask asyncTask, final Exception exception) {
    LOG.warn("Registering async task {} to DLQ {}", asyncTask.type(), exception.getMessage());
  }

  @Override
  public <T> CompletableFuture<T> submitTask(final AsyncTask asyncTask, final Callable<T> task) {

    final CompletableFuture<T> future = new CompletableFuture<>();
    final ExecutorService executorService = Executors.newSingleThreadExecutor();

    executorService.submit(
        () -> {
          try {
            LOG.info(
                "Starting async task: {} on thread: {}",
                asyncTask.type(),
                Thread.currentThread().getName());

            final T result = task.call();
            future.complete(result);

            LOG.info("Async task {} completed successfully", asyncTask.type());
          } catch (final Exception exception) {
            LOG.error(
                "Error executing async task {}: {}",
                asyncTask.type(),
                exception.getMessage(),
                exception);
            if (asyncTask.metadata() != null) {
              asyncTask.metadata().put("exceptionMsg", exception.getMessage());
            }
            registerToDLQ(asyncTask, exception);
            future.completeExceptionally(exception);
          } finally {
            executorService.shutdown();
          }
        });

    executorService.close();

    return future;
  }
}
