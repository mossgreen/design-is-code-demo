---
target: DiscountCalculator.calculate
package: com.disc.decision
input:
  amount: BigDecimal
  tier: Tier
output: BigDecimal
---

| amount  | tier       | expected |
|---------|------------|----------|
| 100.00  | STANDARD   | 100.00   |
| 100.00  | GOLD       | 90.00    |
| 100.00  | PLATINUM   | 80.00    |
| 0.00    | GOLD       | 0.00     |
| -10.00  | STANDARD   | throws: IllegalArgumentException |
