# jbh-personal-finance
Modular monolith to handle personal finance

## Project Overview

I want to build a modular monolith with some boundaries (A , B, C ) and each module (A , B , C) will contain submodules where each submodule will use hexagonal architecture.
At the end all modules belong to the same root parent project running with quarkus and only the submodule (C.infrastructure, A.infrastructure, B.infrastructure) will the ones that exposes APIs ...
and I'd like to be able to centralize the server port for all modules. 

Each module should be completely independent in how it exposes its APIs, and the assembly should just aggregate them without imposing any specific technology choices.

A modular monolith with clean hexagonal boundaries.

One single Quarkus runtime.

Central control over server, DB, logging, health, metrics.

Easy future migration: if module-b needs to become its own service, just move its infrastructure into a new Quarkus app module.

## How it works

- Root POM → BOM & plugin versions, no Quarkus runtime.
- Modules A/B/C → structured into hexagonal submodules.
- App module
  - Contains application.properties with quarkus.http.port=7777 → applies to the entire app
  - Is the only module with the Quarkus Maven plugin → builds the runnable JAR.
    - Depends on all infrastructure modules:
    ```
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-a-infrastructure</artifactId>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-b-infrastructure</artifactId>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-c-infrastructure</artifactId>
    </dependency>
</dependencies>

```

```
jbh-personal-finance/
├── pom.xml                                 (Root Parent)
├── module-a/
│   ├── pom.xml                            (Module A Parent)
│   ├── a-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/a/domain/
│   ├── a-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/a/application/
│   └── a-infrastructure/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/a/infrastructure/
├── module-b/
│   ├── pom.xml                            (Module B Parent)
│   ├── b-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/b/domain/
│   ├── b-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/b/application/
│   └── b-infrastructure/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/b/infrastructure/
├── module-c/
│   ├── pom.xml                            (Module C Parent)
│   ├── c-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/c/domain/
│   ├── c-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/c/application/
│   └── c-infrastructure/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/c/infrastructure/
└── assembly/
├── pom.xml                            (Assembly Module - Quarkus Runner)
└── src/main/
├── java/com/jbh/finance/assembly/
└── resources/
└── application.properties
```