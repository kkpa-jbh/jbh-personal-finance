package com.jbh.account.application.accounts.dto;

import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;

public record AddBasicMovementResponse(AccountDomain account,
                                       AccountMonthlyBalanceDomain monthlyBalance,
                                       AccountMovementDomain movement) {

}
