## What this demo does

The demo builds a sales feature that:

1. Takes a sale request with one or more line items (product + quantity).
2. Looks up each product's price.
3. Calculates the subtotal for each line.
4. Returns a sale response with the line totals and the sale total.

It comes in two stages so you can see both fresh code generation and an update to existing code:

- **Stage 3** — the basic sale: no discount.
- **Stage 4** — adds a bulk discount: buy 5+ of one item to get 10% off that line, 10+ to get 20% off.

## How to run it

DisC is a Claude Code skill. Feed it the design files for a stage and it generates the code.

**Stage 3:**

```
/disc
@design/05_sale/stage_3/CreateSale.puml
@design/05_sale/stage_3/LineSubtotalCalculator.decision.md
```

This creates the Stage 3 code from scratch. Commit it before running Stage 4.

**Stage 4:**

```
/disc
@design/05_sale/stage_4/CreateSaleWithBulkDiscount.puml
@design/05_sale/stage_4/LineSubtotalCalculator.decision.md
@design/05_sale/stage_4/BulkDiscountCalculator.decision.md
```

This adds the bulk-discount feature on top of Stage 3.

## How updates work

- Stage 3's existing code stays exactly as it was — same methods, same logic, byte-for-byte identical.
- New behaviour is added as a **new method** alongside the old one, not by changing the old one.
- New collaborators (like the bulk-discount calculator) get added as new files.

So after Stage 4, you have both `createSale` (the original) and `createSaleWithBulkDiscount` (the new one) living side by side. Existing tests still pass; new tests cover the new method.

This is the safest way to evolve a codebase: old behaviour is preserved by construction, and the design files are the source of truth for what's new.

## Tweaking the discount tiers

The bulk-discount rules (1-4 items: 0%, 5-9: 10%, 10+: 20%) live in `BulkDiscountCalculator.decision.md` as a table. Changing the tiers means editing the table rows and re-running DisC — no code changes by hand.
