---
target: StockValidator.validate
package: com.disc.order
input:
  availableQty: Integer
  requestedQty: Integer
output: void
config:
  nullHandling: throw
  exceptionType: java.lang.IllegalArgumentException
---

| availableQty | requestedQty | expected |
|--------------|--------------|----------|
| 10           | 1            | (no exception) |
| 10           | 10           | (no exception) |
| 5            | 0            | (no exception) |
| 10           | 11           | throws: InsufficientStockException |
| 0            | 1            | throws: InsufficientStockException |
| 5            | -1           | throws: IllegalArgumentException |
