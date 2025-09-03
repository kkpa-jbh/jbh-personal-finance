module jbh.account.domain {
  requires static lombok;
  exports com.jbh.accounts_mgmt.accounts.domain to jbh.account.application;
  exports com.jbh.accounts_mgmt.transactions to jbh.account.application;
}