---
target: BulkDiscountCalculator.calculate
package: com.disc.sale
input:
  quantity: Integer
  lineSubtotal: BigDecimal
output: BigDecimal
config:
  rounding: HALF_UP
  scale: 2
  nullHandling: throw
---

| quantity | lineSubtotal | expected |
|----------|--------------|----------|
| 1        | 100.00       | 0.00     |
| 4        | 400.00       | 0.00     |
| 5        | 500.00       | 50.00    |
| 5        | 49.99        | 5.00     |
| 9        | 900.00       | 90.00    |
| 10       | 1000.00      | 200.00   |
| 20       | 999.80       | 199.96   |
| 0        | 0.00         | 0.00     |
| -1       | 100.00       | throws: IllegalArgumentException |
