module jbh.account.domain {
  requires static lombok;
  exports com.jbh.accounts_mgmt.accounts to jbh.account.application;
  exports com.jbh.accounts_mgmt.exceptions to jbh.account.application;
  exports com.jbh.accounts_mgmt.movements to jbh.account.application;
}