# Assembly

infrastructure modules declare what they need, assembly provides runtime.


jbh-personal-finance/           (Root Parent)
├── pom.xml
├── jbh-account/               (Business Module)
│   └── jbh-account-infra/
└── jbh-assembly/              (Assembly Child ✅)
└── pom.xml


3. Dependency Management

- Assembly inherits parent's dependencyManagement
- No version conflicts between modules
- Centralized Quarkus BOM management

    4. CI/CD Simplicity

    - Single build pipeline
    - Assembly automatically gets rebuilt when modules change
    - Easy integration testing
