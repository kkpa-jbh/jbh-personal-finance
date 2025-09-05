package com.jbh.account.application.accounts.vo;

import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import java.util.List;

public record AddMultipleBasicMovementResponse(AccountDomain account,
                                               List<AccountMonthlyBalanceDomain> monthlyBalances) {

}
