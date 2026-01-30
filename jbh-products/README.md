# MakeFile

Available commands:

- make help - Shows usage instructions and available targets
- make create-schema - Creates the productmgmt schema
- make drop-schema - Drops the schema with confirmation prompt
- make recreate-schema - Drops and recreates the schema
- make check-connection - Tests database connectivity

Usage:
cd jbh-products
make -f Products.mk help
make -f Products.mk create-schema
make -f Products.mk drop-schema

# Plugins

## Surefire

The Surefire plugin is configured in the root pom.xml to run unit tests across all modules.
Each module can have its own test classes, and Surefire will automatically discover and execute them during the build
process.

## JACOCO

The JaCoCo plugin is also configured in the root pom.xml to provide code coverage reports for the entire project.
It aggregates coverage data from all modules, allowing you to see overall test coverage as well as module-specific
coverage.

- To generate one report for the entire project, configure the `report-aggregate` goal in the parent POM
- Each module where you run tests should attach the JaCoCo agent so coverage is collected:
