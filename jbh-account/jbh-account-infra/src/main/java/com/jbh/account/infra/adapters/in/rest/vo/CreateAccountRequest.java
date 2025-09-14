package com.jbh.account.infra.adapters.in.rest.vo;

import com.jbh.account.domain.vo.AccountType;

public record CreateAccountRequest(String name, AccountType type) {}
