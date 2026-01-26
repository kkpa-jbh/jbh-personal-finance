module jbh.commons {
  // Public API (explicitly exported)
  exports com.jbh.commons.util;
  exports com.jbh.commons.exception;

  // No transitive dependencies
  requires java.base;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
}
