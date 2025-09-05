module jbh.account.application {
  requires jbh.account.domain;
  requires org.slf4j;

  exports com.jbh.account.application.acid;
  exports com.jbh.account.application.common.logging;
}