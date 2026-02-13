# Standards Directory

This directory contains the single source of truth for all coding standards, conventions, and quality requirements across the JBH Personal Finance codebase.

## Files

### naming.md
Naming conventions for packages, classes, interfaces, methods, and variables across all architectural layers (domain, application, infrastructure). Reference this when creating new code to ensure consistency.

### code-quality.md
PMD rules, SOLID principles, and code quality requirements. Use this to understand what constitutes high-quality, maintainable code in this project.

### javadoc.md
JavaDoc documentation standards, particularly for use case interfaces. Required reading before documenting any use case.

### testing.md
Testing requirements including minimum coverage (50%), testing strategies, and what to test. Reference before writing unit tests for new code.

## When to Reference

- **Before creating new classes**: Check naming.md for conventions
- **During code reviews**: Validate against code-quality.md rules
- **Before documenting use cases**: Follow javadoc.md standards exactly
- **After implementing features**: Ensure testing.md requirements are met

All standards here override any conflicting default behaviors or external guidelines.
