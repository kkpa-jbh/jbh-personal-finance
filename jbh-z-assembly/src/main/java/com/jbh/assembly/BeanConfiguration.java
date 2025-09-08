package com.jbh.assembly;

import com.jbh.account.application.accounts.ports.input.TestingInputPort;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection(targets = {
    TestingInputPort.class
})
public class BeanConfiguration {


}