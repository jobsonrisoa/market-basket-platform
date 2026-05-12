# MVP Backend Epics

This backlog turns the remaining MVP backend work described across the documentation into implementation-ready epics. It intentionally excludes frontend apps, Kubernetes, Terraform, full production tracing, broad image scanning, and other platform-hardening work unless it directly enables the backend MVP workflow.

## Epic 1: Authorization Ownership Enforcement

Status: implemented.

Goal: ensure seller, catalog, and inventory actions are authorized by both JWT role/permission claims and resource ownership, not by broad role checks alone.

Current state: seller, catalog, and inventory validate auth-service JWTs and gate protected APIs by role. Seller-service enforces store ownership against its membership table and replaces spoofable actor IDs with the JWT subject. Catalog and inventory write paths require platform/service roles or an active `seller_memberships` JWT claim for the target seller. Explicit seller, product, and member IDs remain only where they identify target resources.

Primary services touched: `seller-service`, `catalog-service`, `inventory-service`, `auth-service` for token claim compatibility only.

In-scope stories:

- Enforce seller membership checks in seller store and membership management APIs so seller owners can manage their own stores and staff, while admins retain platform-review privileges.
- Enforce catalog write ownership so seller owners/staff can only create, update, publish, or unpublish products for seller stores where they have active membership.
- Enforce inventory ownership so seller owners/staff can only manage stock and reservations for products belonging to their seller stores.
- Replace client-supplied actor IDs for protected operations with the authenticated JWT subject wherever possible; keep explicit IDs only where they represent target resources.
- Add integration tests that cover allowed owner/staff/admin access, cross-seller denial, removed membership denial, and unauthenticated/incorrect-role denial.

Out of scope:

- Fine-grained per-product staff permissions.
- Gateway-side JWT authorization.
- External policy engines.

Acceptance criteria:

- Protected seller, catalog, and inventory write operations cannot be performed against another seller's resources by changing request IDs.
- `ADMIN` and `SUPER_ADMIN` retain platform-level review and support capabilities.
- Public catalog read endpoints remain public.
- API docs are updated wherever request payloads or actor identity behavior changes.

Event/API/test expectations:

- No new event type is required for this epic.
- Existing API paths should be preserved unless removing explicit actor IDs requires a backward-incompatible request change.
- Tests must prove JWT subject, roles, permissions, seller membership, and membership status are all considered.

Implemented notes:

- Seller store creation, review, store reads, and membership management use the authenticated JWT subject as the actor.
- Seller membership management requires an active seller `OWNER` membership or platform admin role.
- Catalog product create, update, publish, and unpublish require platform admin role or an active seller membership claim for the product seller.
- Inventory stock and reservation reads/writes require platform admin/service role or an active seller membership claim for the stock seller.

## Epic 2: Customer Profile Foundation

Goal: create the first real customer domain slice so registered shoppers have service-owned profile data outside auth.

Current state: `customer-service` is a bounded-context scaffold with a placeholder Flyway migration. Auth emits `auth.user.registered.v1`, and the PRD identifies customer profile management as future scope.

Primary services touched: `customer-service`, `auth-service` event contract examples if needed.

In-scope stories:

- Add customer profile tables owned by `customer-service` for user ID, display name, phone, default locale, profile status, timestamps, and future address/preference expansion.
- Add customer profile APIs for authenticated users to read and update their own profile.
- Add platform/admin read access for support use cases using existing `SUPPORT_AGENT`, `ADMIN`, or `SUPER_ADMIN` roles.
- Consume or prepare a runtime consumer for `auth.user.registered.v1` that creates an initial customer profile idempotently.
- Add contract tests using the recorded auth user-registered example and integration tests for profile ownership.

Out of scope:

- Payment methods.
- Full address book.
- Allergies, dislikes, delivery preferences, and subscription preferences beyond basic profile placeholders.

Acceptance criteria:

- A registered customer can have exactly one customer profile keyed by auth user ID.
- Customers can read and update only their own profile.
- Support/admin roles can read profiles needed for operations.
- Duplicate registration events do not create duplicate customer profiles.

Event/API/test expectations:

- Consumer handling for `auth.user.registered.v1` must be idempotent.
- API docs must describe customer profile endpoints once implemented.
- Flyway migration validation must pass for `customer-service`.

## Epic 3: Seller-Approved Catalog Publishing

Goal: prevent unapproved sellers from publishing products and make seller approval state available to catalog workflows.

Current state: seller stores can be approved or rejected. Catalog products can be published, but seller approval enforcement and seller lookup are documented as future refinements. `seller.approved.v1` has a producer contract, and catalog has a first consumer contract test.

Primary services touched: `seller-service`, `catalog-service`.

In-scope stories:

- Make catalog publishing check seller approval before moving a product to `PUBLISHED`.
- Add catalog-side seller approval state needed for publishing, either through a service lookup or an idempotent consumer/read model fed by seller approval events.
- Add runtime publishing for `seller.approved.v1` if the chosen implementation depends on events instead of synchronous lookup.
- Add rejection and pending-review test cases so products cannot be published for sellers that are not approved.
- Document how catalog handles unavailable seller approval data.

Out of scope:

- Seller compliance document management.
- Multi-step seller review workflow beyond approve/reject.
- Public search/read-model optimization.

Acceptance criteria:

- Products for `PENDING_REVIEW` or `REJECTED` sellers cannot be published.
- Products for approved sellers can still move through draft, published, and unpublished lifecycle states.
- Catalog behavior is deterministic when seller approval data is missing or stale.
- Event contract tests remain green.

Event/API/test expectations:

- Preserve `seller.approved.v1` compatibility.
- If runtime event publishing is added, test outbox/publish behavior or equivalent delivery path.
- Add catalog integration tests around publish authorization and approval state.

## Epic 4: Inventory Reservation Lifecycle

Goal: complete the reservation lifecycle needed by checkout, subscription renewal, cancellation, payment failure, and fulfillment.

Current state: inventory can upsert stock, create active reservations, and release reservations. Reservation expiry, commit, order-driven stock decrement, and adjustment behavior remain future work.

Primary services touched: `inventory-service`, `order-service` for consumer/producer contracts.

In-scope stories:

- Add reservation expiry timestamps and a scheduled or command-driven expiry flow that releases expired active reservations.
- Add reservation commit behavior that converts a reservation into committed stock consumption for confirmed orders.
- Add stock decrement/finalization behavior for fulfillment completion and a forward-only adjustment path for shrinkage or correction.
- Add idempotency for reserve, release, expire, and commit operations keyed by order/reference identifiers.
- Expand inventory event contracts for committed, expired, or adjusted stock events if consumers need them for order consistency.

Out of scope:

- Warehouse/bin-level inventory.
- Lot, harvest date, quality grade, and fulfillment-window modeling beyond what is needed by MVP orders.
- Automated replenishment.

Acceptance criteria:

- Reserved quantity cannot exceed on-hand quantity.
- Expired or released reservations return quantity to availability.
- Committed reservations reduce final available/on-hand stock exactly once.
- Duplicate order events or repeated API calls do not double-reserve, double-release, or double-commit stock.

Event/API/test expectations:

- Preserve `inventory.stock_reserved.v1` and `inventory.reservation_released.v1` compatibility.
- Add tests for reserve, release, expiry, commit, insufficient stock, and duplicate command scenarios.
- Update API docs for new reservation lifecycle endpoints or event-driven commands.

## Epic 5: Subscription And Order MVP Workflow

Goal: introduce the first end-to-end commerce workflow that connects customers, catalog products, inventory reservations, and order states.

Current state: `subscription-service` and `order-service` are scaffolds with placeholder migrations and first consumer contract tests. The PRD describes subscription renewal and order lifecycle as core future scope.

Primary services touched: `subscription-service`, `order-service`, `customer-service`, `catalog-service`, `inventory-service`.

In-scope stories:

- Add subscription plan and customer subscription tables for basket size, cadence, status, seller or marketplace basket reference, timestamps, and next renewal date.
- Add APIs for customers to create, view, pause, resume, skip, and cancel their own subscriptions.
- Add order tables and APIs for draft, confirmed, cancelled, fulfillment-ready, fulfilled, and failed order states.
- Implement renewal flow that creates a draft order from an active subscription and emits `subscription.renewal_due.v1`.
- Implement order confirmation flow that requests or consumes inventory reservation and emits `order.confirmed.v1`.
- Add order fulfillment status changes that emit `order.fulfillment_status_changed.v1`.

Out of scope:

- Real payment processor integration.
- Promotions, discounts, taxes, settlement, and seller payouts.
- Complex basket recommendation or substitution logic.

Acceptance criteria:

- A customer can create an active subscription and see its generated draft order.
- Confirming an order reserves inventory and moves the order to a confirmed state.
- Cancelling an order releases active inventory reservations when appropriate.
- Fulfillment status changes are persisted and published for downstream notification/read-model consumers.

Event/API/test expectations:

- Define JSON Schema contracts and example payloads for `subscription.renewal_due.v1`, `order.confirmed.v1`, and `order.fulfillment_status_changed.v1`.
- Add producer and consumer contract tests for the first runtime event paths.
- Add integration tests for subscription creation, renewal, order confirmation, cancellation, and fulfillment transitions.

## Epic 6: Notification MVP And Event Adoption

Goal: provide an email-first notification foundation and start consuming the events needed for customer and seller communication.

Current state: `notification-service` is a scaffold with a placeholder migration and consumer contract tests. The PRD calls for notification delivery through email first, with SMS and push later.

Primary services touched: `notification-service`, `auth-service`, `seller-service`, `subscription-service`, `order-service`.

In-scope stories:

- Add notification preference, template, notification intent, delivery attempt, and suppression tables owned by `notification-service`.
- Add internal/admin APIs to inspect notification intents and delivery attempts.
- Consume key events: `auth.user.registered.v1`, `seller.approved.v1`, `subscription.renewal_due.v1`, `order.confirmed.v1`, and `order.fulfillment_status_changed.v1`.
- Implement an email provider port with a local no-op or log-backed adapter for development.
- Add idempotent event handling so duplicate Kafka deliveries do not create duplicate notification intents.

Out of scope:

- SMS and push channels.
- Rich template editing UI.
- Full marketing campaigns.

Acceptance criteria:

- Registration, seller approval, renewal, order confirmation, and fulfillment events create auditable notification intents.
- Delivery attempts are recorded with status, provider response metadata, and timestamps.
- Duplicate events are safely ignored or linked to the existing notification intent.
- Local development can exercise the notification flow without real provider credentials.

Event/API/test expectations:

- Consumer contract tests must use recorded example payloads for each consumed event.
- Integration tests must cover idempotent consume, preference suppression, successful local delivery, and failed delivery recording.
- Operations docs must be updated before enabling any real email provider credentials.

## Cross-Epic Defaults

- Keep JSON Schema plus contract tests as the event governance mechanism until multiple production consumers justify Schema Registry.
- Prefer service-owned PostgreSQL tables and versioned Flyway migrations.
- Keep APIs protected by auth-service JWTs and validate issuer/audience consistently.
- Add or update `docs/api.md`, `docs/architecture.md`, and `docs/prd.md` when implementation changes public behavior.
- Avoid frontend, Kubernetes, Terraform, broad tracing, and production incident-routing work in these MVP backend epics.
