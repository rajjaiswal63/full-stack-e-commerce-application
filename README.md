# Full-Stack E-Commerce Application — Backend

**This is the primary repository and the recommended starting point for understanding the project.** Start here to learn the API, security model, data flow, payment verification and local setup.

- **Backend repository:** https://github.com/rajjaiswal63/full-stack-e-commerce-application
- **Matching frontend repository:** https://github.com/rajjaiswal63/e-commerce-application-frontend

The React application is a separate repository and consumes this Spring Boot REST API. Configure and run this backend before starting the frontend.

## Project overview

This project provides the server side of a full-stack online store. It owns authentication, product and category data, cart and address persistence, price and stock validation, payment verification, order creation, order history and admin/seller operations.

The frontend is the user interface; this backend remains the authority for stored data, user permissions, checkout totals and payment verification.

## Implemented features

- Customer registration, sign-in and sign-out using a JWT in an HttpOnly cookie.
- Role-based access for customers, sellers and administrators.
- Public product and category browsing, keyword search, category filtering, pagination and sorting.
- Product and category administration.
- Product image upload and image URL configuration.
- Customer cart synchronization, quantity updates, server-side pricing and stock validation.
- Customer-owned address management.
- Razorpay order creation and server-side verification of the payment signature and captured payment.
- Cash on delivery as the non-gateway payment option.
- Order creation, stock reduction, cart clearing and customer order history.
- Admin analytics and order management.
- Seller product management and seller order/status management, with ownership checks.

A new order starts with the status Accepted. Authorized administrators and sellers can move it through the supported order statuses according to the backend rules.

## Repository relationship

The backend and frontend are maintained separately:

| Repository | Responsibility |
|---|---|
| [Backend — primary project](https://github.com/rajjaiswal63/full-stack-e-commerce-application) | REST API, database, authentication, authorization, business rules, payment verification and order persistence |
| [Frontend — React app](https://github.com/rajjaiswal63/e-commerce-application-frontend) | Storefront, checkout UI, customer order history and admin/seller screens |

The frontend API client sends requests to this backend under the /api prefix and includes credentials for cookie-based authentication. The backend CORS origin must match the frontend's local origin.

## Technology

- Java 21 and Spring Boot 4
- Spring Web MVC, Spring Security, Spring Data JPA and Bean Validation
- PostgreSQL for persistent storage; H2 is also included for local/test configurations
- Razorpay Java SDK for server-side order and payment API calls
- JJWT for JWT creation and validation
- ModelMapper for entity-to-DTO mapping
- Maven Wrapper for building and running
- Springdoc OpenAPI / Swagger UI

Dependency versions are declared in pom.xml.

## Start here: run the backend

### Requirements

- Java 21
- PostgreSQL
- The Maven Wrapper included in this repository
- Razorpay test or live credentials for online checkout

Create a PostgreSQL database named ecommerce, or set DB_URL to the database you want to use.

### Configure the backend

The following environment variables are mapped in application.properties:

| Variable | Purpose | Local default or note |
|---|---|---|
| SERVER_PORT | HTTP server port | 8080 |
| DB_URL | JDBC database URL | jdbc:postgresql://localhost:5432/ecommerce |
| DB_USERNAME | Database user | postgres |
| DB_PASSWORD | Database password | Set for your local database |
| DB_DRIVER | JDBC driver class | org.postgresql.Driver |
| H2_CONSOLE_ENABLED | Enable H2 console | false |
| JPA_DDL_AUTO | Hibernate schema behavior | update |
| JPA_SHOW_SQL | Print SQL statements | false |
| JWT_EXPIRATION_MS | JWT lifetime in milliseconds | 86400000 |
| JWT_COOKIE_NAME | Authentication cookie name | ecommerce |
| COOKIE_SECURE | Secure-cookie setting | false for local HTTP; use true behind HTTPS |
| IMAGE_STORAGE_PATH | Local product image storage | ./images/products/ |
| IMAGE_BASE_URL | Public base URL for product images | http://localhost:8080/images/products |
| FRONTEND_URL | Allowed frontend CORS origin | http://localhost:5173 |
| RAZORPAY_CURRENCY | Razorpay checkout currency | inr |

The current application.properties also contains direct values for the JWT signing secret, Razorpay key ID/secret and bootstrap administrator settings. Keep those values private. Do not publish them in this README, commit them to a public repository, or share them in screenshots. Configure private local values before running the project.

For Razorpay, the frontend receives only the public Key ID through its Vite environment file. The Razorpay Key Secret must stay on the backend.

### Run

From the backend repository directory in PowerShell:

    .\mvnw.cmd spring-boot:run

The API runs at http://localhost:8080 by default. Swagger UI is available at:

    http://localhost:8080/swagger-ui/index.html

### Start the matching frontend

After the backend and database are running, clone and configure the [frontend repository](https://github.com/rajjaiswal63/e-commerce-application-frontend). Its README contains its own install and run steps.

For local development, keep the frontend at http://localhost:5173 and set FRONTEND_URL to that exact origin in the backend configuration.

## Important API routes

All routes below include the /api prefix.

| Operation | Route | Access |
|---|---|---|
| Sign up / sign in / sign out | POST /auth/signup, POST /auth/signin, POST /auth/signout | Public for signup/signin; sign-in required for signout |
| Browse catalog | GET /public/products, GET /public/categories | Public |
| Search products | GET /public/products/keyword/{keyword} | Public |
| Read category products | GET /public/categories/{categoryId}/products | Public |
| Manage own cart | /cart/create, /carts/users/cart and /cart/products/... | Signed-in customer |
| Manage addresses | /addresses and /addresses/{addressId} | Signed-in customer; ownership checked |
| Create a Razorpay order | POST /order/razorpay-order?addressId={id} | Signed-in customer |
| Confirm Razorpay payment and create the store order | POST /order/razorpay/confirm | Public callback; Razorpay signature, capture, order ownership and checkout data are verified server-side |
| Read customer order history | GET /order/users/orders | Signed-in customer; returns only that customer's orders |
| Admin analytics and orders | GET /admin/app/analytics, GET /admin/orders | ADMIN |
| Seller orders | GET /seller/orders | SELLER or ADMIN; seller ownership rules apply |
| Update order status | PUT /admin/orders/{orderId}/status or PUT /seller/orders/{orderId}/status | ADMIN or SELLER, subject to role and ownership rules |

Product and order list endpoints accept pagination and sorting parameters such as pageNumber, pageSize, sortBy and sortOrder where supported.

## Main packages and request flow

- controller — maps HTTP routes to backend operations.
- service — contains business rules; service interfaces are implemented by classes such as OrderServiceImpl, ProductServiceImpl, CartServiceImpl and AuthServiceImpl.
- repository — Spring Data JPA access to persisted records.
- model — database entities such as User, Product, Cart, Address, Order and Payment.
- payload — request and response DTOs returned by the API.
- security — WebSecurityConfig, JWT filter, JWT utilities and CORS configuration.
- util — authentication helpers and checkout/cart utilities.
- exceptions — API error types and response handling.

Typical request flow:

1. The React screen calls an async action in the frontend store.
2. The Axios client sends the request to this API, including cookies when required.
3. Spring Security validates the JWT and role for protected routes.
4. A controller delegates to a service implementation.
5. The service validates the owner, stock, prices and business rules, then uses repositories to read or write entities.
6. The controller returns a DTO; the frontend updates its state and screen.

## Checkout and order flow

1. A signed-in customer selects an address and starts checkout.
2. The backend reads the customer's server-side cart, calculates the amount and creates a Razorpay order. It binds the payment order to the customer, cart and address.
3. Razorpay Checkout returns its order ID, payment ID and signature to the frontend.
4. The frontend sends those details to POST /api/order/razorpay/confirm. This callback does not rely on a browser login cookie.
5. The backend verifies the signature and fetches the Razorpay order/payment to confirm captured status, amount, currency and checkout ownership.
6. Within the order transaction, the backend saves the order, payment and order items, reduces stock and clears the cart.
7. The frontend shows the order number and status. The customer can open /profile/orders to see products, quantities, total and status.

The Razorpay callback is public only so the payment provider redirect can complete when the browser session is unavailable. Order creation still requires server-side verification of the Razorpay payment.

## Scope and exclusions

- Online payment: Razorpay. Cash on delivery is also supported.
- Stripe and PayPal are not active payment integrations in this application.
- Razorpay webhooks/refunds, external shipping/tracking, email or OTP verification, reviews, coupons and tax calculation are not included.
- Database schema changes use Hibernate ddl-auto configuration; no Flyway/Liquibase migration scripts are included.

## Related documentation

- [Frontend setup, routes and UI flow](https://github.com/rajjaiswal63/e-commerce-application-frontend)
- [Primary backend repository](https://github.com/rajjaiswal63/full-stack-e-commerce-application)
## Product images in Amazon S3

Product image uploads are stored in a private S3 bucket under the configured image prefix. The backend serves them through /images/products/{fileName}, so the browser does not need direct access to S3 and the bucket can keep Block Public Access enabled. Existing images still on the server IMAGE_STORAGE_PATH are read as a fallback; new uploads are sent only to S3.

Set these environment variables on the backend:

| Variable | Purpose | Example |
|---|---|---|
| AWS_REGION | Region containing the bucket | ap-south-2 |
| AWS_S3_BUCKET | S3 bucket name | my-ecommerce-product-images |
| AWS_S3_IMAGE_PREFIX | Object key prefix | products |
| IMAGE_BASE_URL | Backend image URL returned in product data | http://EC2_PUBLIC_HOST:8080/images/products |

Attach an EC2 instance role with s3:PutObject and s3:GetObject permissions for arn:aws:s3:::YOUR_BUCKET_NAME/products/*. The AWS SDK default credentials provider uses the EC2 instance role automatically; do not put AWS access keys in application.properties. For local development, use an AWS profile or standard AWS credential environment variables. The bucket does not need public-read ACLs or a browser CORS policy because image requests go through the backend.