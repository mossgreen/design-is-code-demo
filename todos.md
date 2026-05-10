# DisC — Methodology Gaps & TODOs

Programming control-flow primitives the methodology should consider. Ranked by how often the pattern shows up in real Spring/enterprise code.

## Real expressiveness gaps (cannot be expressed today)

### 1. `while` / condition-based loops — HIGHEST PRIORITY
DisC's `loop_block` rule assumes a *collection* ("Test data uses a single-element collection so iteration executes once"). It cannot express:
- `while (cond) { ... }` — repeat until a condition becomes true (retries, polling, paging, queue draining)
- `do-while` — guaranteed at least one iteration
- Infinite loops with internal break (service main loops, message consumers)

**Frequency:** common. **Suggested test idiom:** stub condition `true` once then `false`, verify exactly one iteration ran and that termination happened.
**Action:** add a `while_block` concept to SKILL.md with its own transformation rule.

### 2. Resource management / transactions — HIGHEST PRIORITY
No concept for the universal acquire-do-release pattern:
```
acquire resource → try { do work } finally { release resource }
```
Spring code lives on this: `@Transactional` methods, JDBC connection lifecycle, file handles, locks. Today a DisC user must annotate the implementation manually after generation, outside the design.

**Frequency:** very common in Spring. **Largest Spring-specific gap.**
**Action:** add a stance — either a `transactional_block` concept, or an explicit "DisC does not express this; annotate the implementation manually" with a worked example.

### 3. Catch / recover (the other side of throw)
DisC has `throw_arrow` but no concept for catching and recovering. There is no expression for:
- Retry on transient failure
- Fall back to a degraded path
- Translate exceptions across layers

The SKILL refuses `break` outright (which is the natural PlantUML construct for this).

**Frequency:** common.
**Action:** decide a stance — possibly deliberate (workflows that recover are arguably orchestrating *another* workflow), but currently undocumented.

### 4. Async / future-typed returns
A `return_arrow` carrying `CompletableFuture<Order>` is fundamentally different from one carrying `Order` — the test must reason about composition, not just value. SKILL doesn't address.

**Frequency:** common in modern code (reactive, parallel I/O).
**Action:** add a stance and (probably) a stub idiom for completed-future returns.

### 5. Workflow-level fan-out (`par`)
SKILL explicitly refuses `par` (Step 1 refusal list). Reasoning makes sense for thread-level concurrency — mockist tests can't verify race-freedom. But the *workflow-level* fan-out pattern ("call N collaborators in parallel, wait for all, aggregate") is real and common (parallel API calls, scatter-gather), and could be implemented as `CompletableFuture.allOf(...)`.

**Action:** re-examine the refusal. Possibly add a `par_block` whose semantics are "all arrows fire; tests verify in any order" — without committing to JVM-thread semantics.

## Expressible-but-undocumented (silent on the workaround)

### 6. N-way selection guidance
`branch_block` rule covers `alt`/`else` but never demonstrates more than two arms. PlantUML supports `alt`/`else if`/`else`. The OOAD-correct answer is usually "push into a decision-table leaf," but the methodology doesn't say so.

**Action:** document the prescriptive answer — "if dispatching on a value, push into a decision-table leaf" — and update `branch_block` rule to say what it is *for* (paths that need different *workflows*, not just different *values*).

### 7. Early return inside a branch
02_branching's `else` branch uses `OrderService --> : result.get()` — a return without a destination. Awkward syntax, half-documented. `branch_block` rule doesn't say what happens when one branch produces a `result_test` and the other does not.

**Action:** make early-return first-class with a documented syntax and test pattern.

### 8. Orchestrator-local mutable state
No concept for `int counter = 0; for (...) { counter += compute(...); }`-shaped code. Today it must be pushed into a builder (which 06_order does for `subtotalsSum`) or a stateful collaborator. Possibly *intentional* (forcing state into named collaborators is good design discipline) but undocumented as a stance.

**Action:** document the stance — likely "use a builder; orchestrator-local state is not modelled by DisC."

### 9. Recursion
Self-arrows with parentheses (a `call_arrow` to self with a method name). SKILL never mentions this case. Test implications: base case + recursive case, two `test_group`s; the recursive call is a self-call that needs `Mockito.spy` to mock.

**Frequency:** rare in workflow code.
**Action:** documented stance — likely "DisC does not currently express recursion; lift it into a leaf."

## Non-issues (handled by existing patterns)

For the record, these *look* like gaps but aren't:

- **Return-of-multiple-values** — wrap in a record, return as one value. Not a gap.
- **Polymorphic dispatch** — interfaces are mocked; concrete type is an injection-time concern. Not a gap.

## Meta-observation

DisC handles **structural primitives a programmer chooses** (sequence, selection, iteration over a known collection). It does not handle **contextual primitives the runtime imposes** (lifetime, transactionality, asynchrony, recovery). This reflects DisC's mockist test foundation: `verify(collaborator).method()` constrains "did this call happen with these arguments" — not "inside a transaction," "before a timeout fired," "after the connection was released."

The right question per gap is not "can DisC cover it?" but **"what's the stance?"** Three honest answers per gap:

1. **Express it** — extend SKILL with new concepts (e.g., `while_block`).
2. **Refuse it explicitly** — "DisC does not model X; use an integration test" (current stance for `par`, presumably).
3. **Document the workaround** — "X is expressed as Y" (builder is the answer to orchestrator-local state).

Most gaps above are silent — neither expressed, refused, nor worked-around. A reader hits the case and is on their own.

## Recommended methodology evolution (in order)

1. Add `while_block` as a first-class concept.
2. Add a stance on transactional / resource boundaries (largest Spring-specific gap).
3. Document the prescriptive answer for n-way selection.
4. Document stances on recursion, recovery, async, and orchestrator-local state.
5. Re-examine the `par` refusal for the workflow-level fan-out case.
