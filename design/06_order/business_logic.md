# 06_order — Business Logic

## User story

> As a customer, I want to place an order for one or more products in a single checkout, so that I receive everything I need in one delivery and pay one shipping fee.

## What the business wants

When a customer places an order, the shop needs to do four things, in order:

1. **Know who the customer is.** The shop looks up the customer's account so it knows where to ship to and how to contact them.

2. **Make sure every item is in stock.** For each product the customer wants, the shop checks the warehouse. If even one item is short, the whole order is rejected — the shop does not partially fulfil orders, because customers find a half-arriving order more frustrating than a clean rejection they can act on.

3. **Work out what the customer pays.** The shop prices each line of the order (unit price × quantity), adds the lines up, and applies the shipping rule:
   - Orders over a certain size ship free, as a thank-you for spending more.
   - Smaller orders pay a flat shipping fee that depends on where the customer lives — local deliveries are cheaper than international ones.

4. **Record the order.** Once the order is priced and accepted, the shop saves it so it can be picked, packed, and shipped later.

## What the customer sees

- **Happy path:** the customer hits "Place order" and gets back a confirmation showing each line, the shipping fee (or "free shipping"), and the final total.
- **Out of stock:** the customer is told which item ran short and that no order was placed. Nothing is charged. They can adjust quantities and try again.
- **Bad input:** if something about the request doesn't make sense (e.g. a negative quantity, an unrecognised shipping region), the customer gets a clear error rather than a silently-wrong order.

## Why this feature exists

Placing an order is the single most important transaction in the shop. Every other feature — browsing, search, recommendations, payment, fulfilment — exists to feed into or off this one. Getting it right means: the customer is charged the right amount, the warehouse is told to ship the right things, and nothing is sold that the shop doesn't actually have.
