---
target: StockValidator.hasStock
package: com.disc.order
input:
  availableQty: Integer
  requestedQty: Integer
output: Boolean
config:
  nullHandling: throw
  exceptionType: java.lang.IllegalArgumentException
---

| availableQty | requestedQty | expected |
|--------------|--------------|----------|
| 10           | 1            | true     |
| 10           | 10           | true     |
| 10           | 11           | false    |
| 0            | 1            | false    |
| 5            | 0            | true     |
| 5            | -1           | throws: IllegalArgumentException |
