# 06_order — Design Rationale

This document explains *why the design looks the way it does*. The business doc says **what** the feature must do. The UML and decision tables say **how** the system collaborates and computes. This file says **why those collaborators exist** and **why each one is shaped the way it is** — so future readers do not have to reverse-engineer the reasoning.

## The principle that drives every decision below

> **Software engineering is about abstractions, not implementations.** An interface exists to encapsulate a *responsibility*, not to wrap today's code. When tomorrow brings a different way of computing the same thing, we replace the implementation; the interface — and every caller of it — stays still.

Every participant in this design is named after the *responsibility it owns*, not the *code currently behind it*. Where this conflicts with the urge to "just inline that one line of arithmetic," the urge loses.

## OOAD walkthrough

### From narrative to nouns and verbs

Reading the business doc as a fresh reader:

- **Nouns surfaced:** customer, order, line item, product, stock, line subtotal, order total, shipping fee, region, threshold, warehouse, confirmation.
- **Verbs surfaced:** place, look up, validate stock, price (a line, an order), apply shipping rule, build, save.

### Filtering — what belongs in *this* feature's bounded context?

Discarded as out-of-context or as attributes of something else:

- **Warehouse** — implementation detail of stock-validation; not surfaced.
- **Confirmation** — a presentation concern, not a domain object.
- **Threshold** — a *rule inside* the shipping abstraction, not an entity.
- **Stock** — an attribute of a product (`availableQty`), not a thing of its own.

Domain entities and value types: **Customer, Product, LineItem, PricedLineItem, Order, OrderRequest.**

Domain *concepts* — the responsibilities that, if not given homes, end up bloating the orchestrator:

- Stock validation
- Line pricing
- Priced-line construction
- Order totalling
- Shipping fee determination

Each is a unit of business knowledge. Each will change for one reason. Each deserves a name and a contract.

### CRC-style responsibility allocation

| Responsibility | Owner (role / interface) |
|---|---|
| Find a customer by id | `CustomerRepository` |
| Find a product by id | `ProductRepository` |
| Reject the order if a line is short on stock | `StockValidator` |
| Compute a line's price | `LineSubtotalCalculator` |
| Pair a line item with its priced subtotal | `PricedLineItemFactory` |
| Compute the order's total | `OrderTotalCalculator` |
| Compute the shipping fee | `ShippingFeeCalculator` |
| Construct an `Order` from its parts | `OrderFactory` |
| Persist an `Order` | `OrderRepository` |
| Coordinate the placement workflow | `OrderService` |

Each row's owner exists because *someone* must hold that knowledge. The orchestrator delegates; it does not compute, branch, or decide.

### Change-axis audit (the SRP test)

For each abstraction, the *one* reason it would change:

| Abstraction | Reason to change |
|---|---|
| `CustomerRepository` | Customer storage technology |
| `ProductRepository` | Product storage technology |
| `OrderRepository` | Order storage technology |
| `StockValidator` | What "available" means (instant stock → reservations → multi-warehouse) |
| `LineSubtotalCalculator` | Per-line pricing rules (rounding, currency, per-line promos) |
| `PricedLineItemFactory` | The `PricedLineItem` value type's shape |
| `OrderTotalCalculator` | How an order's total is composed (sum → sum + tax → sum + tax − discount) |
| `ShippingFeeCalculator` | Shipping rules (thresholds, regions, promos) |
| `OrderFactory` | The `Order` object's structural shape |
| `OrderService` | The placement workflow itself (a step added or removed) |

Ten participants, ten independent change axes, no overlap. That is the SRP check, applied at the *design* level rather than the line-of-code level.

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

## Why `PricedLineItem` exists as its own value type

A naïve design would pass two parallel lists to the order factory: `lineItems` and `lineSubtotals`, correlated by index. That is fragile (mismatched lengths, off-by-one zips) and OOAD-poor (the relationship between a line and its price is invisible to the type system).

`PricedLineItem` is the **named pair**: a `LineItem` together with its computed `BigDecimal` subtotal. The two cannot exist apart in this domain — a priced line *is* the line plus its price. Surfacing that as a value type:

- Makes the order's shape honest: an order is `(customer, list of priced lines, shipping fee)` — three coherent things, no parallel anything.
- Eliminates the "trust the factory to zip correctly" failure mode by construction.
- Gives a stable place to add per-line metadata later (per-line discount, tax, line note) without re-shaping the factory call.

Construction is its own responsibility (`PricedLineItemFactory`), so the orchestrator doesn't perform domain construction inline. This is the same discipline applied to `OrderFactory`: factories do construction, orchestrators do orchestration.

## Why `StockValidator` throws instead of returning a Boolean

An earlier version of this design returned a `Boolean` from the validator and let the orchestrator decide whether to throw. That version had two problems:

1. The validator knew *the rule for "is there enough stock"* but the orchestrator knew *what to do when not*. Two parts of one responsibility, in two places.
2. The orchestrator carried an `if (not hasStock) throw` — control flow that is really a *validation rule*, leaking out of the validator.

Renaming `hasStock` (a question) to `validate` (an imperative) and giving it `void` return + thrown exception fixes both. The validator owns the rule end-to-end. The orchestrator simply *calls* `validate(...)` — no `if`, no branch, no decision. The diagram has no `alt` block; the loop body is a clean linear sequence.

This is OOAD doing real work: naming the responsibility (*"validate"*) determines the interface shape, and the interface shape eliminates a structural complication in the caller.

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

## A note on `OrderTotalCalculator`'s decision-table absence

`OrderTotalCalculator.calculate(lineSubtotals)` takes a `List<BigDecimal>`. Decision tables in this skill have no syntax for a list-typed input column, so this leaf is left in **skeleton mode** — DisC will scaffold a TODO test class for the human to fill.

This is a question of *how the implementation is proven*, not *whether the abstraction is justified*. The interface is defended on responsibility grounds above; the skeleton is a small testing-mechanics tax. We do **not** dissolve the abstraction to dodge a tooling limitation. That would be the implementation tail wagging the design dog.

If, later, the skill grows a list-input syntax, this leaf gets a filled decision table and the skeleton goes away. The interface itself — and every caller of it — stays exactly as it is. That is what a well-placed abstraction buys you.

## Summary

Ten participants. One orchestrator, three side-effect repositories, three pure-function leaves with decision tables, one pure-function leaf in skeleton mode, two factories. Each named for a permanent responsibility in the domain. No top-level branching. No `alt` block in the orchestrator. One throw, owned by the validator that decides on it. One loop with a clean linear body.

The design will survive: a change to stock semantics, a new pricing rule, a new shipping region, a tax requirement, a discount feature, the addition of per-line metadata, a switch of database — *each one of these is a single-implementation change with no ripple into the orchestrator or its tests*.

That is what the abstractions buy. That is why they are here.
