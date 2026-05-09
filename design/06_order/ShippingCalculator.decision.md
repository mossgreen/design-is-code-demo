---
target: ShippingCalculator.calculate
package: com.disc.order
input:
  orderTotal: BigDecimal
  customer.region: String
output: BigDecimal
config:
  rounding: HALF_UP
  scale: 2
  nullHandling: throw
---

| orderTotal | customer.region | expected |
|------------|-----------------|----------|
| 50.00      | "DOMESTIC"      | 5.00     |
| 99.99      | "DOMESTIC"      | 5.00     |
| 50.00      | "INTERNATIONAL" | 25.00    |
| 99.99      | "INTERNATIONAL" | 25.00    |
| 0.00       | "DOMESTIC"      | 0.00     |
| 50.00      | "ZZZ"           | throws: IllegalArgumentException |
