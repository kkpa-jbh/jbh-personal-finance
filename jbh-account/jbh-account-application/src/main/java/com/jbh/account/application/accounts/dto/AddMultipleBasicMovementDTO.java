package com.jbh.account.application.accounts.dto;

import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import java.util.List;

public record AddMultipleBasicMovementDTO(AccountDomain account,
                                          List<AccountMonthlyBalanceDomain> monthlyBalances) {

}
