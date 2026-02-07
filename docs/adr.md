# ARCHITECTURE DECISION RECORDS (ADR)

The record focuses on one significant architectural decision that typically affects the system’s structure, core behavior, or key qualities. Michael Nygard, who first popularized
ADRs in 2011, defines architecturally significant decisions as those that impact "the structure, non-functional characteristics, dependencies, interfaces, or construction
techniques" of the system. In other words, ADRs log the big decisions that shape your product, not any information.

## ADR-001 - PRODUCT TYPES

In the context of `Products Creation` facing the approach to storage the different types of products,
I decided to persist all the products in the same table by using a hybrid approach(metadata)
to achieve the following:

1. No JOINs for cross-product aggregations
2. Partition-friendly (by year/month for time-series)
3. Easy to add new types of products
4. Focus on READ-HEAVY operations

### Context

How do we model the different types of products?

### Decision

1. Core products table (common fields) + metadata JSONB column
2. Posible Materialized View to aggregate the data
