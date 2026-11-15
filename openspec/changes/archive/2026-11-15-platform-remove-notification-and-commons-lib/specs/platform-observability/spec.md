# platform-observability Specification

## MODIFIED Requirements

### Requirement: No personal data in INFO+ logs
No INFO, WARN or ERROR log line SHALL write amounts, balances, statement or OCR text, LLM prompts or answers, IBAN or account numbers, e-mail addresses, client IP addresses, API keys, transaction lists or a whole domain object.
The e-mail notification flow is not part of this service. It moved to the `jbh-notification` service, which owns its own log rules.

#### Scenario: Product and movement flow
- GIVEN a movement is added to a product with a balance of 1000
- WHEN the INFO lines of the flow are read
- THEN none holds the amount, the balance, the product name or the movement description
- AND they hold ids, periods, counts or enum names only

#### Scenario: Incoming request
- GIVEN a request arrives from a client with an IP address
- WHEN the INFO lines of the request are read
- THEN none holds the client IP address
