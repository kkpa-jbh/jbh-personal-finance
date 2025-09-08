# Assembly

infrastructure modules declare what they need, assembly provides runtime.

jbh-personal-finance/           (Root Parent)
├── pom.xml
├── jbh-account/               (Business Module)
│ └── jbh-account-infra/
└── jbh-assembly/              (Assembly Child ✅)
└── pom.xml

2. Plugins

“Now comes the most crucial part – the configuration of quarkus-maven-plugin.

To make the bootstrap module the one that will start the Quarkus engine,
we need to configure quarkus-maven-plugin in that module properly.

- we make this module responsible for starting the Quarkus engine.


3. Dependency Management

- Assembly inherits parent's dependencyManagement
- No version conflicts between modules
- Centralized Quarkus BOM management

4. CI/CD Simplicity

    - Single build pipeline
    - Assembly automatically gets rebuilt when modules change
    - Easy integration testing

## Responsibilities

Assembly Module (jbh-assembly)
├── Provides Quarkus Runtime Environment
├── Aggregates ALL infrastructure modules as Maven dependencies
├── Contains unified application.properties (port 7777, CDI config)
├── Enables cross-module bean discovery via Jandex indices
├── Hosts the main QuarkusApplication class
└── Builds the final executable JAR

What the Assembly Module DOES NOT Do:

❌ Manually configure CDI beans
❌ Define @Produces methods for injection
❌ Handle specific business logic
❌ Know about internal module dependencies
