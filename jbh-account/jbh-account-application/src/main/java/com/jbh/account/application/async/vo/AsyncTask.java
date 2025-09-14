package com.jbh.account.application.async.vo;

import java.util.Map;

public record AsyncTask(AsyncTaskType type, Map<String, Object> metadata) {}
