module jbh.account.domain {
  requires static lombok;
  exports com.jbh.account.domain.utils to jbh.account.application;
  exports com.jbh.account.domain.accounts to jbh.account.application, jbh.account.infra;
  exports com.jbh.account.domain.exceptions to jbh.account.application;
  exports com.jbh.account.domain.movements to jbh.account.application;
}