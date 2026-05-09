# 06_order — Design Rationale

This document explains *why the design looks the way it does*. The business doc says **what** the feature must do. The UML and decision tables say **how** the system collaborates and computes. This file says **why those collaborators exist** and **why each one is shaped the way it is** — so future readers do not have to reverse-engineer the reasoning.

## The principle that drives every decision below

> **Software engineering is about abstractions, not implementations.** An interface exists to encapsulate a *responsibility*, not to wrap today's code. When tomorrow brings a different way of computing the same thing, we replace the implementation; the interface — and every caller of it — stays still.

Every participant in this design is named after the *responsibility it owns*, not the *code currently behind it*. Where this conflicts with the urge to "just inline that one line of arithmetic," the urge loses.

## OOAD walkthrough

### From narrative to nouns and verbs

Reading the business doc as a fresh reader:

- **Nouns surfaced:** customer, order, line item, product, stock, line subtotal, order total, shipping fee, region, threshold, warehouse, confirmation.
- **Verbs surfaced:** place, look up, check stock, price (a line, an order), apply shipping rule, build, save.

### Filtering — what belongs in *this* feature's bounded context?

Discarded as out-of-context or as attributes of something else:

- **Warehouse** — implementation detail of stock-checking; not surfaced.
- **Confirmation** — a presentation concern, not a domain object.
- **Threshold** — a *rule inside* the shipping abstraction, not an entity.
- **Stock** — an attribute of a product (`availableQty`), not a thing of its own.

Domain entities and value types: **Customer, Product, LineItem, Order, OrderRequest.**

Domain *concepts* — the responsibilities that, if not given homes, end up bloating the orchestrator:

- Stock availability decision
- Line pricing
- Order totalling
- Shipping fee determination

These four are the OOAD-critical ones. Each is a unit of business knowledge. Each will change for one reason. Each deserves a name and a contract.

### CRC-style responsibility allocation

| Responsibility | Owner (role / interface) |
|---|---|
| Find a customer by id | `CustomerRepository` |
| Find a product by id | `ProductRepository` |
| Decide whether stock is sufficient | `StockValidator` |
| Compute a line's price | `LineSubtotalCalculator` |
| Compute the order's total | `OrderTotalCalculator` |
| Compute the shipping fee | `ShippingFeeCalculator` |
| Construct an `Order` from its parts | `OrderFactory` |
| Persist an `Order` | `OrderRepository` |
| Coordinate the placement workflow | `OrderService` |

Each row's owner exists because *someone* must hold that knowledge. The orchestrator delegates; it does not compute.

### Change-axis audit (the SRP test)

For each abstraction, the *one* reason it would change:

| Abstraction | Reason to change |
|---|---|
| `CustomerRepository` | Customer storage technology |
| `ProductRepository` | Product storage technology |
| `OrderRepository` | Order storage technology |
| `StockValidator` | What "available" means (instant stock → reservations → multi-warehouse) |
| `LineSubtotalCalculator` | Per-line pricing rules (rounding, currency, per-line promos) |
| `OrderTotalCalculator` | How an order's total is composed (sum → sum + tax → sum + tax − discount) |
| `ShippingFeeCalculator` | Shipping rules (thresholds, regions, promos) |
| `OrderFactory` | The `Order` object's structural shape |
| `OrderService` | The placement workflow itself (a step added or removed) |

Nine participants, nine independent change axes, no overlap. That is the SRP check, applied at the *design* level rather than the line-of-code level.

## Why `OrderTotalCalculator` exists, even though today its body is one line

This is the abstraction that most often gets prematurely inlined, so it deserves a direct defence.

The interface name is `OrderTotalCalculator`. Its responsibility is **owning how an order's total is composed**. Today's composition rule is `Σ lineSubtotals`. Tomorrow's plausible compositions:

- `Σ lineSubtotals + tax`
- `Σ lineSubtotals − orderLevelDiscount`
- `Σ lineSubtotals − loyaltyCredit + tax`
- `Σ lineSubtotals − giftCardCredit, capped at zero`

If we inline today's one-line sum into the orchestrator, every one of those changes forces:

1. The orchestrator to grow new logic.
2. Every test of the orchestrator to be re-derived.
3. The "place an order" workflow to re-prove itself end-to-end for what is really a *pricing* change.

By contrast, with `OrderTotalCalculator` as a real abstraction, every one of those changes is **isolated to its implementation**. The orchestrator does not move. Its tests do not move. The change axis stays where it belongs.

The fact that today's implementation is small is a coincidence of the moment; the fact that order-total composition is its own change axis is a permanent property of the domain. We design to the permanent property, not the coincidence.

This was the discipline I applied to `ShippingFeeCalculator` (where the threshold and region rules clearly belong inside) and the same discipline applies here. Consistency with itself is part of a design's quality.

## Why no `PricingStrategy`, `ShippingPolicy`, or `OrderValidator`

The temptation in mid-complexity systems is to invent strategy interfaces "in case" multiple implementations appear. That is speculative generality and it is a worse failure than the one above.

- **`PricingStrategy`** — would have one implementation. Decision tables already express the strategy declaratively. No.
- **`ShippingPolicy`** — same. The strategy *is* the rows in `ShippingFeeCalculator.decision.md`.
- **`OrderValidator`** — stock validation is the only validation. A separate validator on top of `StockValidator` would be a wrapper around a single method.

The discipline: **introduce an abstraction when a responsibility exists in the domain, not when a pattern from a textbook fits.**

## Why the shipping threshold lives inside `ShippingFeeCalculator`

A reader could reasonably ask: shouldn't the orchestrator have an `if orderTotal >= threshold` branch and call shipping only when needed?

No. The threshold is a **shipping rule**, not an **order-workflow rule**. The orchestrator does not know — and should not know — that there is such a thing as a threshold. It asks: *"what is the shipping fee for this order?"* and trusts the shipping abstraction to answer.

This collapses what would have been a top-level `branch_block` into a clean linear post-loop flow, and it makes the threshold a piece of *data* (a row in the decision table) rather than a piece of *control flow*. Changing the threshold from $100 to $150 becomes an edit to one row. The orchestrator's tests are untouched.

This is OCP applied where it actually pays — at a real change axis, not as theatre.

## Why the throw lives inside the loop

The business rule is *fail-fast on first short line, do not partially fulfil*. The diagram expresses that rule directly: the moment a line fails stock, an exception fires. No state is accumulated, no rollback is needed, no "rejected lines" list is built up. The simplest semantics that match the business rule.

The cost is structural: this nests `loop + alt + throw`, a combination not exemplified in the existing skill examples. The fallback if DisC trips on it is to lift validation into a pre-loop pass — but that pre-loop pass would re-fetch products, or carry state, and either way it would be a worse expression of the rule. We accept the structural complexity in exchange for semantic honesty.

## Acknowledged compromise: parallel `lineItems` / `lineSubtotals` lists

The factory takes `(customer, orderRequest, lineSubtotals, shippingFee)`. That implies the factory zips `orderRequest.lineItems` against `lineSubtotals` by index — fragile, OOAD-imperfect.

The OOAD-correct fix is a `PricedLineItem` value type plus its construction step. We are choosing **not** to make that fix here, for two reasons:

1. Consistency with 05_sale, which uses the same convention. Mixing conventions inside a small demo would be more confusing than the wart.
2. Right-time discipline: the demo's purpose is to introduce the throw and the multi-leaf composition pattern. Bundling a `PricedLineItem` introduction into the same step would mix two unrelated lessons.

This is a real wart. We are flagging it here so it is acknowledged rather than hidden. Cleaning it up belongs in a follow-up refactor that touches both 05 and 06 together.

## A note on `OrderTotalCalculator`'s decision-table absence

`OrderTotalCalculator.calculate(lineSubtotals)` takes a `List<BigDecimal>`. Decision tables in this skill have no syntax for a list-typed input column, so this leaf is left in **skeleton mode** — DisC will scaffold a TODO test class for the human to fill.

This is a question of *how the implementation is proven*, not *whether the abstraction is justified*. The interface is defended on responsibility grounds above; the skeleton is a small testing-mechanics tax. We do **not** dissolve the abstraction to dodge a tooling limitation. That would be the implementation tail wagging the design dog.

If, later, the skill grows a list-input syntax, this leaf gets a filled decision table and the skeleton goes away. The interface itself — and every caller of it — stays exactly as it is. That is what a well-placed abstraction buys you.

## Summary

Nine participants. One orchestrator, three side-effect repositories, three pure-function leaves with decision tables, one pure-function leaf in skeleton mode, one factory. Each named for a permanent responsibility in the domain. No top-level branching. One nested guard-clause exception. One loop.

The design will survive: a change to stock semantics, a new pricing rule, a new shipping region, a tax requirement, a discount feature, a switch of database — *each one of these is a single-implementation change with no ripple into the orchestrator or its tests*.

That is what the abstractions buy. That is why they are here.
