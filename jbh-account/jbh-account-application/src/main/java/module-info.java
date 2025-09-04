module jbh.account.application {
  requires jbh.account.domain;
  requires org.slf4j;

  exports com.jbh.account_app.acid;
  exports com.jbh.account_app.common.logging;
}