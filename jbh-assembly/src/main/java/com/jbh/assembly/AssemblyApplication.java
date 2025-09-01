package com.jbh.assembly;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class AssemblyApplication implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(AssemblyApplication.class);

    public static void main(String... args) {
        LOG.info("Starting JBH Personal Finance Platform...");
        System.out.println("Hello from AssemblyApplication main method!");
        Quarkus.run(AssemblyApplication.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("JBH Personal Finance Platform started successfully!");
        LOG.info("All modules loaded and APIs available");
        LOG.info("Available endpoints should include: /jbh-account/hello/{name}");

        Quarkus.waitForExit();
        return 0;
    }
}