package com.jbh.account.application.async;

import com.jbh.account.application.async.vo.AsyncTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public interface AsyncTaskExecutor {

  <T> CompletableFuture<T> submitTask(AsyncTask asyncTask, Callable<T> task);
}
