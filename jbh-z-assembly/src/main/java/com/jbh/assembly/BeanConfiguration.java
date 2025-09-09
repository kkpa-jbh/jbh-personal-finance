package com.jbh.assembly;

import com.jbh.account.application.accounts.ports.input.NoOperationInputPort;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection(targets = {
    NoOperationInputPort.class
})
public class BeanConfiguration {


}