package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.accounts.AccountDomain;
import com.jbh.account.domain.accounts.AccountMonthlyBalanceDomain;
import java.util.List;

public record AddMultipleBasicMovementDTO(AccountDomain account,
                                          List<AccountMonthlyBalanceDomain> monthlyBalances) {

}
