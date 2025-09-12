package com.jbh.account.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "jbh.gateway")
public interface JbhGatewayClientConfigProvider {

  @WithName("base-url")
  String baseUrl();

}
