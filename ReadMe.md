# B2C Mobile Money Payment Microservice

---

## Features
- **Initiate B2C payments** via a REST API.
- **Track payment status**.
- **Asynchronous SMS notifications** for transaction status updates.
- **Abstracted integrations** for Mobile Money APIs (M-Pesa, Airtel Money, etc.) and SMS Gateway.
- **OAuth2 Resource Server security**.
- **In-memory H2 database** for persistence.
- **Logging and error handling**.
- **Unit and Integration tests**.
- **Dockerization**.

---

## Project Structure
The project follows a standard Spring Boot application structure:

```plaintext
src/main/java/com/finsense/payment
├── config
│   ├── SecurityConfig.java         # Security configuration (OAuth2)
├── controller
│   ├── PaymentController.java      # REST API endpoints
├── exception
│   ├── GlobalExceptionHandler.java # Centralized exception handling
│   ├── PaymentException.java
│   ├── InvalidRequestException.java
│   └── ExternalApiException.java   # Exceptions for external service interactions
├── model
│   ├── B2CPaymentRequest.java      # Request body for payment initiation
│   ├── PaymentStatus.java          # Enum for transaction status
│   └── PaymentTransaction.java     # Entity for payment transactions
├── repository
│   ├── PaymentTransactionRepository.java # JPA repository for H2 database
├── service
│   ├── MobileMoneyService.java         # Abstract interface for Mobile Money APIs
│   ├── SmsGateway.java               # Abstract interface for SMS Gateway
│   ├── PaymentService.java           # Core business logic
│   ├── mock                          # Mock implementations for external services
│   │   ├── MockMobileMoneyService.java
│   │   └── MockSmsGateway.java
├── PaymentApplication.java           # Main Spring Boot application class
└── util
    └── PaymentIdGenerator.java     # Utility for generating transaction IDs

src/main/resources
├── application.properties          # Application configuration
└── application-test.properties     # Configuration for tests

src/test/java/com/finsense/payment
├── controller
│   └── PaymentControllerIntegrationTest.java # Integration tests for the controller
├── service
│   └── PaymentServiceTest.java           # Unit tests for the service layer
└── repository
    └── PaymentTransactionRepositoryTest.java # Unit tests for the repository

Dockerfile                          # Docker configuration
pom.xml                             # Maven project file
```

---

## Prerequisites
- **Java Development Kit (JDK) 1.8**  (as configured in `pom.xml`).
- **Maven 3.6+**.
- **Docker** (optional, for containerization).

---

## Getting Started

### Building the Application
Navigate to the project root directory in your terminal and run:

```bash
mvn clean install
```

This will compile the code, run tests, and package the application into a JAR file in the `target` directory.

### Running the Application
You can run the application using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

Alternatively, you can run the generated JAR file:

```bash
java -jar target/finsense-app-0.0.1-SNAPSHOT.jarr
```

The application will start on the port configured in `application.properties` (default is **8080**).

---

## Configuration
The main configuration is in `src/main/resources/application.properties`:

```properties
server.port=8080
spring.application.name=finsense-app
spring.datasource.url=jdbc:h2:mem:paymentdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=YOUR_JWK_SET_URI
logging.level.com.finsense.payment=DEBUG
logging.level.org.springframework=INFO
```

**Important**: Replace `YOUR_JWK_SET_URI` with the actual JWK Set URI from your OAuth2 authorization server.

For testing, `src/main/resources/application-test.properties` is used, which configures H2 with `ddl-auto=create-drop` for a clean database state for each test run.

---

## API Endpoints

The microservice exposes the following REST endpoints under the `/api/v1/payments` base path:

### `POST /api/v1/payments/initiate`
**Description**: Initiates a new B2C payment transaction.

**Request Body**:

```json
{
  "recipientPhoneNumber": "+[country_code][number]",
  "amount": 100.00,
  "currency": "KES",
  "provider": "MPESA" | "AIRTEL_MONEY" | "MOCK", // Example providers, MOCK is for testing
  "description": "Payment description (optional)"
}
```

**Authentication**: Requires a valid OAuth2 token with the `payment:initiate` scope.

**Response**: Returns the created PaymentTransaction object with an initial status (e.g., PENDING, IN_PROGRESS) and a unique transaction ID.

**Status Codes**:
- `201 Created`: Payment initiation request accepted.
- `400 Bad Request`: Invalid request payload (validation errors), unsupported provider, or invalid amount.
- `401 Unauthorized`: Missing or invalid authentication token.
- `403 Forbidden`: Token does not have the required scope.
- `500 Internal Server Error`: An unexpected error occurred on the server.
- `502 Bad Gateway`: Error communicating with the external mobile money provider.

### `GET /api/v1/payments/{transactionId}/status`
**Description**: Retrieves the current status of a payment transaction.

**Path Variable**: `{transactionId}` - The unique ID of the payment transaction returned by the `/initiate` endpoint.

**Authentication**: Requires a valid OAuth2 token with the `payment:status` scope.

**Response**: Returns the PaymentTransaction object with the latest status and details.

**Status Codes**:
- `200 OK`: Transaction found and status returned.
- `401 Unauthorized`: Missing or invalid authentication token.
- `403 Forbidden`: Token does not have the required scope.
- `404 Not Found`: Payment transaction with the given ID was not found.
- `500 Internal Server Error`: An unexpected error occurred on the server.

---

## Security (OAuth2)
The microservice acts as an OAuth2 Resource Server. It expects incoming requests to the secured endpoints (`/api/v1/payments/initiate`, `/api/v1/payments/{transactionId}/status`) to include a valid **JWT Bearer** token in the Authorization header.

The microservice validates the token's signature using the JWK Set obtained from the URI configured in `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`. It also checks if the token contains the required scopes (`payment:initiate`, `payment:status`) using `@PreAuthorize`.

You will need an external **OAuth2 Authorization Server** to issue these tokens.

---

## H2 Console
The **H2 in-memory database console** is enabled for development and debugging. You can access it at:

[http://localhost:8080/h2-console](http://localhost:8080/h2-console)

Use the JDBC URL `jdbc:h2:mem:paymentdb`, username `sa`, and an empty password to connect.

---

## Mock Services
For development and testing purposes, mock implementations of `MobileMoneyService` (`MockMobileMoneyService`) and `SmsGateway` (`MockSmsGateway`) are provided. These mocks simulate successful interactions with external services without making actual API calls.

In a production environment, you would replace these mocks with concrete implementations that integrate with the actual M-Pesa, Airtel Money, and SMS gateway APIs.

---

## Testing
The project includes unit and integration tests:

- **Unit Tests**: Located in `src/test/java/com/finsense/payment/service` and `src/test/java/com/finsense/payment/repository`. These tests use mocking to isolate the component being tested.
- **Integration Tests**: Located in `src/test/java/com/finsense/payment/controller`. These tests use Spring Boot's testing utilities (`@SpringBootTest`, `MockMvc`) to test the interaction between different layers of the application, including the web layer, service layer, and repository.

You can run all tests using Maven:

```bash
mvn test
```

---

## Dockerization
A **Dockerfile** is included to containerize the application.

To build the Docker image:

```bash
docker build -t finsense-payment-service .
```

To run the Docker container:

```bash
docker run -p 8080:8080 finsense-payment-service
```

The application will be accessible on [http://localhost:8080](http://localhost:8080).


