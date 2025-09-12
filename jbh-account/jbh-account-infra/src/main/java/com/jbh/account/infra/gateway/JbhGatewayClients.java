package com.jbh.account.infra.gateway;


import com.jbh.gateway.client.users.JbhUserApiGatewayClient;

public interface JbhGatewayClients {

  JbhUserApiGatewayClient getUserClient();

}
