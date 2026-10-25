# E-commerce Backend (Spring Boot)

This backend is the REST API for the sibling frontend project. It preserves the reference backend's customer, catalog, cart, address, checkout, Razorpay, admin and seller features while retaining the existing target project's validation and ownership/security checks.

Frontend setup and its own environment variables are documented in ../sb-ecommerce-front/README.md.

## Reference alignment

The reference backend is under Downloads\New folder\sb-ecom and uses the same e-commerce domain layers: controllers, services, repositories, entities, DTOs and JWT security. This project keeps its existing org.ecommerce.project Java package and adapts the REST API to the reference frontend.

Unrelated backend features and compatibility paths remain. The Stripe-only payment integration was replaced with Razorpay. Public product/category reads stay public; category writes, admin routes and seller routes remain protected. Address and cart APIs restrict data to the signed-in owner, with global listings limited to ADMIN.

## Features connected to the frontend

- Customer signup/signin/signout and JWT cookie authentication
- Public category/product lists, pagination, sorting, filters and keyword search
- Product create/update/delete and image upload for admin/seller
- Cart synchronization, stock validation, item quantities and totals
- User-owned address create/read/update/delete
- Razorpay order creation, payment signature/capture verification and checkout order creation
- Admin analytics, product/category/order management and seller listing/creation
- Seller-owned product and order management

Razorpay Standard Checkout is the only online payment gateway. The frontend sends the Razorpay callback to the backend, which verifies it before creating an order.

## Routes used by the frontend

All API routes use http://localhost:8080 as the local origin. Swagger UI is available at /swagger-ui/index.html.

| Frontend operation | Method and API path | Authorization |
|---|---|---|
| Signup | POST /api/auth/signup | Public; customer by default; ADMIN can create a seller |
| Signin | POST /api/auth/signin | Public |
| Current user name/details | GET /api/auth/username and GET /api/auth/user | username is public; details require login |
| Signout | POST /api/auth/signout | Login; clears the JWT cookie |
| Browse categories | GET /api/public/categories | Public |
| Browse/search products | GET /api/public/products | Public; supports pagination, keyword and category |
| Browse category products | GET /api/public/categories/{categoryId}/products | Public |
| Search products by keyword | GET /api/public/products/keyword/{keyword} | Public |
| Manage categories | POST/PUT/DELETE /api/admin/categories[/categoryId] | ADMIN |
| Create products | POST /api/admin/categories/{categoryId}/product or /api/seller/categories/{categoryId}/product | ADMIN or SELLER |
| Manage/list products | GET/PUT/DELETE /api/admin/products... or /api/seller/products... | ADMIN or SELLER; owner checks apply to seller |
| Upload product image | PUT /api/admin/products/{productId}/image or /api/seller/products/{productId}/image | ADMIN or SELLER; owner checks apply to seller |
| Save cart | POST /api/cart/create | Login; saves current user's cart |
| Read current cart | GET /api/carts/users/cart | Login; own cart only |
| Add one product | POST /api/carts/products/{productId}/quantity/{quantity} | Login |
| Change/remove cart quantity | PUT /api/cart/products/{productId}/quantity/{add or delete} | Login |
| Manage addresses | POST/GET /api/addresses and GET/PUT/DELETE /api/addresses/{addressId} | Login; own address, ADMIN override |
| Create Razorpay order | POST /api/order/razorpay-order | Login; Razorpay keys configured |
| Verify Razorpay payment and create checkout order | POST /api/order/users/payments/online | Login; signature and captured status verified |
| Admin analytics | GET /api/admin/app/analytics | ADMIN |
| Admin/seller orders | GET /api/admin/orders or /api/seller/orders | ADMIN or SELLER |
| Update order status | PUT /api/admin/orders/{orderId}/status or /api/seller/orders/{orderId}/status | ADMIN or SELLER; service enforces seller rules |
| List sellers | GET /api/auth/sellers?pageNumber=0 | ADMIN |

Product/order/category lists accept the pagination/sorting query parameters expected by the frontend: pageNumber, pageSize, sortBy and sortOrder. Product listing also accepts keyword and category.

## Main classes and request flow

- controller/AuthController → service/AuthServiceImpl handles signup/signin and current-user calls.
- Login uses AuthenticationManager and UserDetailsServiceImpl; JwtUtils creates the HttpOnly cookie.
- security/jwt/AuthTokenFilter reads and verifies the cookie or Bearer token and sets the Spring Security context.
- controller/ProductController and CategoryController → ProductServiceImpl and CategoryServiceImpl → repositories.
- controller/CartController → CartServiceImpl. Cart data sent by the browser is checked against current product stock and server prices.
- controller/AddressController → AddressServiceImpl. Service checks address ownership before read/update/delete.
- controller/OrderController → OrderServiceImpl creates an order and payment record from the authenticated user's cart and address.
- RazorpayServiceImpl creates an order using the backend keys, binds it to the account and cart, verifies the checkout signature, then confirms the fetched payment is captured before fulfilment.
- AnalyticsController → AnalyticsServiceImpl aggregates counts/revenue/order metrics for the admin dashboard.
- repository/ contains Spring Data JPA database operations; model/ contains entities; payload/ contains request/response DTOs; exceptions/ formats API errors; config/ contains shared configuration.

### Package layout

- controller/ — REST API controllers
- service/ — service interfaces and business logic
- repository/ — database repositories
- model/ — JPA entities and role definitions
- payload/ — request/response DTOs
- security/ — Spring Security, JWT and CORS
- exceptions/ — API errors
- config/ — constants, mapping and static image handling
- src/main/resources/application.properties — runtime configuration
- images/products/ — default and sample images; uploaded files are stored here by default

## Run locally

Requirements: Java 21, PostgreSQL and the included Maven wrapper.

1. Create a PostgreSQL database named ecommerce.
2. Set DB_PASSWORD and JWT_SECRET. Add the Razorpay Key ID and Key Secret for online checkout.
3. Ensure FRONTEND_URL is http://localhost:5173 for the sibling Vite app.
4. From this directory run .\mvnw.cmd spring-boot:run.
5. Open http://localhost:8080/swagger-ui/index.html.

Hibernate uses ddl-auto=update by default. No Flyway/Liquibase migration scripts are included. Back up data before schema changes.
Razorpay payment orders are fulfilled only after the backend verifies the callback signature and confirms the payment is captured. Enable automatic payment capture in the Razorpay Dashboard for the account.

## Backend environment variables

application.properties resolves these variables. Set them in the process/OS environment, or put literal values in application.properties. Keep secrets out of version control.

| Variable | Property | Default / requirement |
|---|---|---|
| SERVER_PORT | server.port | 8080 |
| DB_URL | spring.datasource.url | jdbc:postgresql://localhost:5432/ecommerce |
| DB_USERNAME | spring.datasource.username | postgres |
| DB_PASSWORD | spring.datasource.password | Set the local PostgreSQL password |
| DB_DRIVER | spring.datasource.driver-class-name | org.postgresql.Driver |
| H2_CONSOLE_ENABLED | spring.h2.console.enabled | false |
| JPA_DDL_AUTO | spring.jpa.hibernate.ddl-auto | update |
| JPA_SHOW_SQL | spring.jpa.show-sql | false |
| JWT_SECRET | spring.app.jwtSecret | Required; Base64 text decoding to at least 32 random bytes |
| JWT_EXPIRATION_MS | spring.app.jwtExpirationMs | 86400000 (24 hours) |
| JWT_COOKIE_NAME | spring.ecom.app.jwtCookieName | ecommerce |
| COOKIE_SECURE | security.cookie.secure | false locally; true behind HTTPS |
| IMAGE_STORAGE_PATH | project.image | ./images/products/ |
| IMAGE_BASE_URL | image.base.url | http://localhost:8080/images/products |
| FRONTEND_URL | frontend.url | http://localhost:5173; exact allowed CORS origin, no trailing slash |
| RAZORPAY_KEY_ID | razorpay.key.id | Razorpay public Key ID; also set VITE_RAZORPAY_KEY_ID in the frontend .env |
| RAZORPAY_KEY_SECRET | razorpay.key.secret | Required for online payments; keep secret in backend only |
| RAZORPAY_CURRENCY | razorpay.currency | inr (Indian rupees; set a currency supported by your Razorpay account) |
| BOOTSTRAP_ADMIN_USERNAME | app.bootstrap.admin.username | Empty disables bootstrap admin |
| BOOTSTRAP_ADMIN_EMAIL | app.bootstrap.admin.email | Empty disables bootstrap admin |
| BOOTSTRAP_ADMIN_PASSWORD | app.bootstrap.admin.password | Empty disables bootstrap admin; if set, all three bootstrap values are required and password must be 12+ characters |

JWT_SECRET is Base64-decoded by the application before token signing. COOKIE_SECURE=true also uses SameSite=None for cross-site HTTPS cookie requests. For local Vite development keep both app origins on localhost and COOKIE_SECURE=false.

## Maven dependencies

All backend dependencies are declared in pom.xml. The official Razorpay Java SDK is used for server-side order and payment API calls.

- Spring Boot 4.0.5, Java 21
- Spring Web MVC, Spring Data JPA, Validation and Security
- springdoc-openapi-starter-webmvc-ui 3.0.0
- Razorpay Java SDK 1.4.10
- PostgreSQL JDBC runtime driver; H2 and H2 console for local/test use
- ModelMapper 3.2.4 and optional Lombok
- JJWT 0.12.6 modules: jjwt-api, jjwt-impl and jjwt-jackson
- Spring Boot test and Spring Security test dependencies (test scope)

Spring Boot manages versions for its starter and database dependencies.

## Not implemented in the reference scope

These are outside the current project scope:

- PayPal payment processing
- Razorpay webhooks and refunds
- Customer order-history page/API
- Email/OTP verification
- Shipping integration/tracking, reviews, coupons or tax calculation
- Flyway/Liquibase database migration scripts


Customer order history is available to the signed-in user at GET /api/order/users/orders (supports pageNumber, pageSize, sortBy and sortOrder).
