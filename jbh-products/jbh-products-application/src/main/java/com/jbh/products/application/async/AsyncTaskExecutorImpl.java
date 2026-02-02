package com.jbh.products.application.async;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.async.vo.AsyncTask;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncTaskExecutorImpl implements AsyncTaskExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncTaskExecutorImpl.class);

  @Override
  public <T> CompletableFuture<T> submitTask(final AsyncTask asyncTask, final Callable<T> task) {

    if (asyncTask.metadata() != null
        && !(asyncTask.metadata() instanceof HashMap<String, Object>)) {
      throw new IllegalArgumentException("Metadata must be a HashMap");
    }

    final CompletableFuture<T> future = new CompletableFuture<>();
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    LOG.info("Starting async task: " + asyncTask);
    final Instant start = Instant.now();
    executorService.submit(
        () -> {
          try {
            final T result = task.call();
            future.complete(result);
            final long durationMs = Duration.between(start, Instant.now()).toMillis();
            LOG.info("Async task " + asyncTask + " completed in " + durationMs + " ms");
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
            exception.printStackTrace();
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
          } finally {
            executorService.shutdown();
            executorService.close();
          }
        });

    return future;
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
