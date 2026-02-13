# Patterns Directory

This directory contains proven architectural patterns and implementation examples for building features in the JBH Personal Finance application.

## Files

### use-case-complete.md
Complete reference implementation for use case architecture. Includes the full vertical slice from REST endpoint through use case to database, with all layers properly connected. This is the authoritative pattern for implementing any new feature.

### mapping-flow.md
Step-by-step mapping flow pattern showing how data transforms through architectural layers: Entity → DTO → Response. Demonstrates proper separation of concerns and object placement across hexagonal architecture boundaries.

## When to Use

- **Starting a new feature**: Use use-case-complete.md as your implementation template
- **Creating REST endpoints**: Reference mapping-flow.md to understand the complete data transformation pipeline
- **Reviewing architecture**: Validate that implementations follow these established patterns
- **Onboarding**: Study these patterns to understand how the application is structured

These patterns ensure consistency across all features and maintain clean hexagonal architecture principles.
