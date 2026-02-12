package com.jbh.finance.infra.gateway;

import com.jbh.gateway.client.users.JbhUserGatewayClient;

public interface GatewayClientFactory {
  JbhUserGatewayClient getUserClient();
}
