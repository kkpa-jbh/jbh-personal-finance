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

# CONSUL

This is a modular monolith - a single deployable application (jbh-z-assembly) that contains multiple modules. In Consul, you should register one service (the whole application),
not separate services per module.

The modules (jbh-account, jbh-notification) are internal packages, not independent microservices. They share:

- Same JVM process
- Same port (7777)
- Same health endpoint

## Where to Place Configuration

┌─────────────────────┬───────────────────────────────────────────────────────────────┐                                                                                                                                      
│ Location │ What Goes There │                                                                                                                                      
├─────────────────────┼───────────────────────────────────────────────────────────────┤                                                                                                                                      
│ jbh-z-assembly │ Consul registration code and main config (single entry point)
│                                                                                                                                      
├─────────────────────┼───────────────────────────────────────────────────────────────┤                                                                                                                                      
│ Module infra layers │ Nothing for Consul - they're not separate services
│                                                                                                                                      
└─────────────────────┴───────────────────────────────────────────────────────────────┘

## How It Works

1. On Startup: @Observes StartupEvent triggers registration with Consul
2. Health Check: Consul polls /q/health every 30s
3. On Shutdown: @Observes ShutdownEvent deregisters the service
4. Auto-deregister: Service removed after 1 minute if health checks
   fail

## Why One Service (Not Per Module)

Since this is a modular monolith, all modules (jbh-account, jbh-notification) run in the same JVM on port 7777. They're not separate deployable services, so only one Consul
registration is needed in jbh-z-assembly. 
                                                                               