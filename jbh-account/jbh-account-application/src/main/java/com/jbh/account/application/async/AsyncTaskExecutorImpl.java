package com.jbh.account.application.async;

import com.jbh.account.application.async.vo.AsyncTask;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
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

    executorService.submit(
        () -> {
          try {
            LOG.info(
                "Starting async task: {} with metadata {} on thread: {}",
                asyncTask.type(),
                asyncTask.metadata(),
                Thread.currentThread().getName());

            final T result = task.call();
            future.complete(result);
            LOG.info(
                "Async task {} with metadata {} completed successfully",
                asyncTask.metadata(),
                asyncTask.type());
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
