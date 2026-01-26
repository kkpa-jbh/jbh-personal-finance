# General Instructions

- Always use Context7 MCP when I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.
- Always generate unit tests for new code generated on any module different from jbh-z-assembly and jbh-account-infra. The minimum coverage is 50%.

## PMD Rules

- Declare all variabels final as possible
- Avoid using literals in if statements.
- A method/constructor should not explicitly throw java.lang.Exception.
- Avoid catching generic exceptions such as NullPointerException, RuntimeException, Exception in try-catch block.