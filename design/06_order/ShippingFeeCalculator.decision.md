---
target: ShippingFeeCalculator.calculate
package: com.disc.order
input:
  orderTotal: BigDecimal
  region: String
output: BigDecimal
config:
  rounding: HALF_UP
  scale: 2
  nullHandling: throw
  exceptionType: java.lang.IllegalArgumentException
---

| orderTotal | region          | expected |
|------------|-----------------|----------|
| 100.00     | "DOMESTIC"      | 0.00     |
| 100.00     | "INTERNATIONAL" | 0.00     |
| 250.00     | "DOMESTIC"      | 0.00     |
| 99.99      | "DOMESTIC"      | 5.00     |
| 50.00      | "DOMESTIC"      | 5.00     |
| 0.00       | "DOMESTIC"      | 5.00     |
| 99.99      | "INTERNATIONAL" | 25.00    |
| 50.00      | "INTERNATIONAL" | 25.00    |
| 50.00      | "ZZZ"           | throws: IllegalArgumentException |
