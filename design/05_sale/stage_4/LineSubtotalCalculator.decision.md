---
target: LineSubtotalCalculator.calculate
input:
  quantity: Integer
  unitPrice: BigDecimal
output: BigDecimal
config:
  rounding: HALF_UP
  scale: 2
  nullHandling: throw
---

| quantity | unitPrice | expected |
|----------|-----------|----------|
| 1        | 100.00    | 100.00   |
| 2        | 49.99     | 99.98    |
| 3        | 20.00     | 60.00    |
| 0        | 100.00    | 0.00     |
| 3        | 0.333     | 1.00     |
| -1       | 100.00    | throws: IllegalArgumentException |
