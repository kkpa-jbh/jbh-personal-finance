# Assembly

infrastructure modules declare what they need, assembly provides runtime.


jbh-personal-finance/           (Root Parent)
├── pom.xml
├── jbh-account/               (Business Module)
│   └── jbh-account-infra/
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
