package com.jbh;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class AssemblyApplication implements QuarkusApplication {

    public static void main(String... args) {
        System.out.println("Hello from AssemblyApplication main method!");
        Quarkus.run(AssemblyApplication.class, args);
    }


    @Override
    public int run(String... args) throws Exception {
        Quarkus.waitForExit();
        return 0;
    }
}