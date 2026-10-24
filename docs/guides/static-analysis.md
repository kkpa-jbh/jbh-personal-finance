# Static analysis: PMD and SpotBugs

Both tools run in `./mvnw clean verify`, after the tests. CI runs the same command.

| Tool | Config | Fails the build |
|------|--------|-----------------|
| PMD | `pmd-ruleset.xml` (root) | Yes, by default (`pmd.failOnViolation=true`) |
| SpotBugs + FindSecBugs | `spotbugs-include.xml`, `spotbugs-exclude.xml` (root and some modules), effort `Max` | Locally no (`spotbugs.failOnError=false`). In CI yes. |

## Commands

```bash
# Same checks as CI
./mvnw clean verify -Dspotbugs.failOnError=true

# One module, no tests
./mvnw -pl jbh-finance/jbh-finance-application -am verify -DskipTests

# Reports only
./mvnw pmd:pmd            # target/site/pmd.html in each module
./mvnw spotbugs:spotbugs  # target/spotbugsXml.xml in each module
./mvnw spotbugs:gui       # desktop viewer
```

`./mvnw test` and `./mvnw package` do not run PMD.

## Notes

- PMD does not check test code.
- Before you finish a change, read "Common traps" in `.claude/standards/code-quality.md`.
- CI keeps the PMD and SpotBugs XML reports for 30 days (Actions artifacts).
