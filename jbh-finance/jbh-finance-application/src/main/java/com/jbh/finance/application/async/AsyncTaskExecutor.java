package com.jbh.finance.application.async;

import com.jbh.finance.application.async.vo.AsyncTask;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public interface AsyncTaskExecutor {

  <T> CompletableFuture<T> submitTask(AsyncTask asyncTask, Callable<T> task);

  /**
   * Waits until every task submitted before this call, on this instance, has finished. A failed
   * task does not make the wait fail.
   *
   * <p>Never call it inside a transaction. A background task may wait for a database lock that the
   * caller still holds, so both would wait for each other until the timeout.
   *
   * @param timeout maximum time to wait
   * @throws IllegalStateException on timeout, on interrupt, or when called from the executor's own
   *     worker thread
   */
  void awaitIdle(Duration timeout);
}
