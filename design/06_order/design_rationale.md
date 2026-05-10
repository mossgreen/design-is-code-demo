# 06_order — Design Rationale

This document explains *why the design looks the way it does*. The business doc says **what** the feature must do. The UML and decision tables say **how** the system collaborates and computes. This file says **why those collaborators exist** and **why each one is shaped the way it is** — so future readers do not have to reverse-engineer the reasoning.

## The principle that drives every decision below

> **Software engineering is about abstractions, not implementations.** An interface exists to encapsulate a *responsibility*, not to wrap today's code. When tomorrow brings a different way of computing the same thing, we replace the implementation; the interface — and every caller of it — stays still.

Every participant in this design is named after the *responsibility it owns*, not the *code currently behind it*.

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
- Order assembly (accumulating priced lines, exposing the running subtotal sum, finalising with a shipping fee)
- Shipping fee determination

Each is a unit of business knowledge. Each will change for one reason. Each deserves a name and a contract.

### CRC-style responsibility allocation

| Responsibility | Owner (role / interface) |
|---|---|
| Find a customer by id | `CustomerRepository` |
| Find a product by id | `ProductRepository` |
| Reject the order if a line is short on stock | `StockValidator` |
| Compute a line's price | `LineSubtotalCalculator` |
| Mint an order builder for a customer | `OrderBuilderFactory` |
| Accumulate priced lines and emit the constructed `Order` | `OrderBuilder` |
| Answer questions about its own state (subtotal, total) and accept a shipping fee | `Order` (entity, returned from `OrderBuilder.build()`) |
| Compute the shipping fee | `ShippingFeeCalculator` |
| Persist an `Order` | `OrderRepository` |
| Coordinate the placement workflow | `OrderService` |

The orchestrator delegates; it does not compute, branch, or decide.

### Change-axis audit (the SRP test)

For each abstraction, the *one* reason it would change:

| Abstraction | Reason to change |
|---|---|
| `CustomerRepository` | Customer storage technology |
| `ProductRepository` | Product storage technology |
| `OrderRepository` | Order storage technology |
| `StockValidator` | What "available" means (instant stock → reservations → multi-warehouse) |
| `LineSubtotalCalculator` | Per-line pricing rules (rounding, currency, per-line promos) |
| `OrderBuilderFactory` | How a fresh builder is initialised for a customer |
| `OrderBuilder` | How an order is assembled — what `addLine` accepts, what `build` produces |
| `Order` | What an order knows about itself — its lines, its subtotal, its shipping fee, its total |
| `ShippingFeeCalculator` | Shipping rules (thresholds, regions, promos) |
| `OrderService` | The placement workflow itself (a step added or removed) |

Each participant has one independent change axis, no overlap.

## Why an `OrderBuilder`, not a parallel-list factory

An earlier draft of this design used the convention 05_sale relies on: the loop returns `lineSubtotal` (singular) per iteration, and the post-loop scope refers to `lineSubtotals` (plural) as if a list had been collected. That convention is *not stated* in the DisC SKILL — it is a tacit reading of "loop returns become a post-loop list" — and it has a real cost: the assembly of the plural is invisible to the diagram and unconstrained by the orchestrator's tests. The orchestrator could pass a wrong, empty, or reordered list and the test would not catch it.

The builder pattern makes the assembly explicit and verifiable:

- **State accumulates inside a real participant.** `OrderBuilder.addLine(lineItem, lineSubtotal)` is a `verify_test` per loop iteration — the test pins down that the orchestrator added each line with the correct values.
- **The post-loop value is a real return arrow.** `OrderBuilder.subtotalsSum() → orderTotal` and `OrderBuilder.build(shippingFee) → order` are both bound returns. There is no plural-from-loop convention; there is no ghost variable.
- **Domain coherence.** "An order is a thing being assembled" is closer to how the domain actually works than "an order is a tuple of parallel lists handed to a factory." The builder owns its in-progress state — exactly what builders are for.

The corpus already has this pattern: see [03_loop.puml](../03_loop.puml), where `InvoiceBuilder.addLine(order)` is the per-iteration call and `InvoiceBuilder.build()` returns the assembled invoice. This design composes the same shape with decision-table leaves and a throw-arrow validator.

## Why `OrderBuilderFactory.create(customer)` takes a customer

A reader could reasonably ask: *isn't a factory's job pure construction? Shouldn't `create()` take no args, and the customer be set via a separate `withCustomer(customer)` method on the builder?*

The answer is no, and the reason is a distinction that matters in OOAD: **a factory's job is to construct a *valid* instance, not a *generic empty* one.**

- `Customer` requires a name to be valid; a name-less `Customer` is not a domain state.
- `Address` requires a street; a street-less `Address` is not a domain state.
- `OrderBuilder` requires a customer to be valid, because an "order in progress" is conceptually *for somebody*. An order-being-assembled with no owner is not a meaningful domain state — there is no real-world stage at which Bob's order has not yet been assigned to anyone.

By that reading, `customer` is the builder's **identity at birth**, not a field that happens to be set first. The factory's signature `create(customer)` is doing one thing — minting a valid builder — and the customer is what *makes* it valid.

**Contrast with [03_loop.puml](../03_loop.puml).** There, `InvoiceBuilderFactory.create()` takes no args because the invoice in that domain has no identity at construction time — it is just an aggregator of orders. Different domains, different valid initial states, different factory signatures. The corpus is consistent if read as *"factory args = the minimum required for the constructed thing to be valid."*

**Why the alternative (`create()` + `withCustomer(customer)`) is worse.** It allows an `OrderBuilder` instance to exist in a customer-less state, which is invalid in this domain. It privileges a generic methodology principle ("factories take no args") over the domain principle ("make invalid states unrepresentable"). The latter wins: the interface that cannot express invalid states is the interface that cannot be misused.

The line to draw: a factory taking *the identity required for validity* is fine; a factory taking *arbitrary mutable configuration* (e.g., `create(customer, region, threshold, …)`) is not — that conflates construction with configuration, and the configuration parts belong on the constructed object as state mutations.

## Why `build()` takes no arguments

A `build()` method is a *commit*. It says: *"I have told you everything; now produce the result."* Every other method on the builder exists to feed `build()`. If `build()` itself takes a parameter, then `build()` is doing two things — accepting one last input AND finalising — which is the exact role-overloading we are otherwise careful to avoid.

The universal builder-pattern convention reflects this: `StringBuilder.toString()`, Lombok `@Builder.build()`, Java's `Stream.collect(...)` — none take new state at the commit step. Inputs go in via setters and `add` methods; the commit step takes nothing.

Concretely, an earlier draft had `build(shippingFee) → order`. That was lazy interface design: the shipping fee deserved its own arrival path, not a parameter on the commit. The current shape is `build()` with no arguments — the constructed `Order` arrives ready to be queried for its subtotal so that shipping can be computed and applied.

## Why `subtotal()` lives on `Order`, not on the builder

The subtotal is *the sum of an order's line subtotals*. It is, by definition, a property of the order — a number you can derive from the order's lines whenever you have the order in hand.

An earlier draft put `subtotalsSum()` on the builder, so the orchestrator could query the in-progress sum before shipping was decided. That had two problems:

1. **The builder grew a query method**, which conflicts with the textbook builder lifecycle (write-only until commit). Builders are asymmetric — every method exists to feed `build()` — and a query that does not feed `build()` is a leak of responsibility.
2. **The sum is a property of the order, not of the assembly process.** It is not "a number the builder happens to maintain"; it is "a fact about an order's lines." Whichever object owns the lines should own the question.

Putting `subtotal()` on `Order` matches the data: the lines live on the order, so the question lives on the order. The builder is now write-only (`addLine` only), which restores its textbook shape.

## Why `Order.applyShipping(shippingFee)` is a mutator (and what we accept by allowing it)

The shipping fee depends on the order's subtotal, which depends on the order's lines, which are only fully known after the loop. So the fee cannot be passed to `build()` (we want `build()` to take nothing) and it cannot live on the builder (the builder has emitted the order and is done). The remaining honest options are:

1. **`Order` is constructed without shipping and accepts it later via a mutator** (`order.applyShipping(fee)`). The order is mutable for one specific transition: from "lines settled, shipping unset" to "fully priced." Chosen.
2. **A separate `OrderDraft` value type represents the lines-settled-but-shipping-unknown phase**, and a `finalise(fee) → Order` step produces an immutable `Order`. Cleaner — no mutation, no invalid states — but introduces a new domain concept (`OrderDraft`) that the shop does not natively name.
3. **`build(shippingFee)` takes the fee.** Rejected (see preceding section).

We chose option 1 to keep the participant count flat and avoid adding a domain type whose only purpose is to make the `Order`'s lifecycle a chain of immutable values rather than a brief mutable interval.

**What we accept by choosing the mutator:**

- `Order` is mutable in one specific window — between `build()` and `applyShipping(...)`. During that window, calling `subtotal()` is valid; calling `total()` (subtotal + shipping) is not yet meaningful; saving the order is incorrect. The interface allows these mistakes; the implementation must guard against them, or the workflow must be trusted not to make them. The orchestrator's tests pin down the *correct* sequence (`build → subtotal → calculate shipping → applyShipping → save`) so the contract is enforced by the workflow, not by the type.
- The order's lifecycle is partially encoded in convention rather than in the type system. A more rigorous design would represent the two phases as two types (option 2 above). We are choosing convention here for compactness; if `Order`'s lifecycle grows new partial phases (e.g., partial payment, partial fulfilment), the right move is to revisit and split the type.

This is OOAD doing honest accounting: **we name the cost of the chosen shape so a future reader understands what was traded for compactness.**

## Why `StockValidator` throws instead of returning a Boolean

An earlier draft returned `Boolean` and let the orchestrator decide whether to throw. That version had two problems:

1. The validator knew *the rule for "is there enough stock"* but the orchestrator knew *what to do when not*. Two parts of one responsibility, in two places.
2. The orchestrator carried an `if (not hasStock) throw` — control flow that is really a *validation rule*, leaking out of the validator.

Renaming `hasStock` (a question) to `validate` (an imperative) and giving it `void` return + thrown exception fixes both. The validator owns the rule end-to-end. The orchestrator simply *calls* `validate(...)`. The diagram has no `alt` block; the loop body is a clean linear sequence.

This is OOAD doing real work: naming the responsibility (*"validate"*) determines the interface shape, and the interface shape eliminates a structural complication in the caller.

## Why no `PricingStrategy`, `ShippingPolicy`, or `OrderValidator`

The temptation in mid-complexity systems is to invent strategy interfaces "in case" multiple implementations appear. That is speculative generality.

- **`PricingStrategy`** — would have one implementation. Decision tables already express the strategy declaratively.
- **`ShippingPolicy`** — same. The strategy *is* the rows in `ShippingFeeCalculator.decision.md`.
- **`OrderValidator`** — stock validation is the only validation. A separate validator on top of `StockValidator` would be a wrapper around a single method.

The discipline: **introduce an abstraction when a responsibility exists in the domain, not when a pattern from a textbook fits.**

## Why the shipping threshold lives inside `ShippingFeeCalculator`

A reader could reasonably ask: shouldn't the orchestrator have an `if orderTotal >= threshold` branch and call shipping only when needed?

No. The threshold is a **shipping rule**, not an **order-workflow rule**. The orchestrator does not know — and should not know — that there is such a thing as a threshold. It asks: *"what is the shipping fee for this order?"* and trusts the shipping abstraction to answer.

This collapses what would have been a top-level `branch_block` into a clean linear post-loop flow, and it makes the threshold a piece of *data* (a row in the decision table) rather than a piece of *control flow*. Changing the threshold from $100 to $150 becomes an edit to one row. The orchestrator's tests are untouched.

This is OCP applied where it actually pays — at a real change axis, not as theatre.

## Summary

One orchestrator, three side-effect repositories, three pure-function leaves with decision tables, one builder factory, one write-only builder, and one entity (`Order`) that knows its own subtotal and accepts its shipping fee. Each named for a permanent responsibility in the domain. No top-level branching. No `alt` block in the orchestrator. One throw, owned by the validator that decides on it. One loop with a clean linear body. Every value on the wire is bound to a return arrow or to an input.

The design will survive: a change to stock semantics, a new pricing rule, a new shipping region, a tax requirement, a discount feature, the addition of per-line metadata, a switch of database — *each one of these is a single-implementation change with no ripple into the orchestrator or its tests*.

That is what the abstractions buy. That is why they are here.
