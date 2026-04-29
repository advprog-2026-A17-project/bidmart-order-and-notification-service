# Order and Notification Service

This service implements the extra-mile post-auction order module for BidMart. The assignment treats order and notification as optional for a 4-member team, but this repository takes Strategy B by adding a dedicated service for order creation, shipment tracking, buyer receipt confirmation, and auction-won event handling.

Notification endpoints are intentionally not implemented in this first slice because the current project direction explicitly ignores notification for now. The package name keeps `ordernotificationservice` so notification can be added later without creating another service boundary.

## Stack Choice

This service uses Java 21 and Spring Boot because the existing auth, catalogue, gateway, and legacy wallet services already use Spring Boot, Gradle, JPA, H2 for tests, and PostgreSQL for runtime. Keeping the optional module in the same stack reduces operational complexity and makes the implementation easier to review alongside the rest of the project.

## Git Workflow

- `main` is the release branch.
- `staging` is the integration branch.
- Feature work is done on branches such as `feat/order-service-extra-mile`.
- Feature branches should be merged into `staging` first.
- `main` should only receive changes through `staging`.

## Implemented API

- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `PUT /api/v1/orders/{orderId}/status`
- `POST /api/v1/orders/{orderId}/confirm`
- `POST /api/v1/orders/events/auction-won`

## Lifecycle

Orders support these lifecycle states:

- `CREATED`
- `PACKED`
- `SHIPPED`
- `DELIVERED`
- `CONFIRMED`
- `DISPUTED`

New orders expose shipping status as `PENDING` until the seller updates fulfillment status. Buyer confirmation transitions the order to `CONFIRMED`.

## Security Model

The service trusts identity headers only when they are supplied by the gateway:

- Sellers can create orders and update shipping status.
- Buyers can confirm receipt.
- Sellers and buyers can retrieve their own order details.
- Auction-won event creation requires `X-Internal-Service-Token`.

## Local Test

Run:

```bash
./gradlew test
```

The current contract test covers manual order creation, retrieval, seller shipping updates, buyer confirmation, automatic auction-won order creation, idempotent event replay, and seller or buyer authorization.
