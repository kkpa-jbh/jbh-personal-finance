package com.jbh.finance.test.testfixtures;

import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UseCaseTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(UseCaseTestBase.class);

  @BeforeAll
  public static void resetFixtures() {
    LOG.warn("Resetting In-Memory Data");
    UseCaseFixtureBuilder.resetState();
  }
}
