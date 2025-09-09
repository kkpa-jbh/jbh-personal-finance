🚀 GitHub Actions Features:

- Java 21 + Maven setup with caching
- Static Analysis on every push/PR
- Security Scanning with Trivy
- Test Reporting with JUnit results
- PR Comments with analysis summary
- Fail Fast - builds fail on violations in CI

## Current CI Pipeline Flow

graph TD
A[Push/PR] --> B[Job 1: test-and-analyze]
B --> C[Checkout Code]
C --> D[Setup Java 21]
D --> E[Cache Maven Deps]
E --> F[Run mvn verify with PMD & SpotBugs]
F --> G[Generate Test Reports]
G --> H[Archive PMD Reports]
H --> I[Archive SpotBugs Reports]
I --> J[Comment on PR]
B --> K[Job 2: security-scan]
K --> L[Trivy Vulnerability Scan]
L --> M[Upload to Security Tab]