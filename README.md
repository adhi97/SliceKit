# SliceKit

A lightweight Java framework for **Vertical Slice Architecture** (VSA) that enables feature-complete, isolated slices with built-in dependency injection and HTTP routing.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## What is Vertical Slice Architecture?

Unlike traditional **layered architecture** where code is organized horizontally by technical concerns (controllers, services, repositories), **Vertical Slice Architecture** organizes code vertically by business features.

### Layered vs Vertical Slice Architecture

| **Layered Architecture** | **Vertical Slice Architecture** |
|--------------------------|----------------------------------|
| `controllers/` → `services/` → `repositories/` | `create-order/` → `update-order/` → `cancel-order/` |
| Technical separation | Feature separation |
| Shared across features | Isolated per feature |
| High coupling between layers | Low coupling between slices |

### Example: Order Management

**Layered Approach:**
```
src/
├── controllers/OrderController.java
├── services/OrderService.java  
├── repositories/OrderRepository.java
└── models/Order.java
```

**VSA Approach:**
```
src/
├── create-order/
│   ├── CreateOrderSlice.java
│   ├── OrderDataAccess.java
│   └── OrderEmailService.java
├── update-order/
│   ├── UpdateOrderSlice.java
│   └── OrderValidator.java
└── cancel-order/
    ├── CancelOrderSlice.java
    └── RefundService.java
```

## Why SliceKit?

### Perfect for AI-Assisted Development

**Generative AI agents** like Claude work exceptionally well with VSA because:

- **🎯 Focused Context**: Each slice contains everything needed for one feature
- **🔍 Reduced Cognitive Load**: AI can understand complete workflows in isolation  
- **⚡ Faster Iteration**: Changes are localized and less likely to break other features
- **🧪 Easier Testing**: Each slice can be tested independently
- **📝 Self-Documenting**: Business logic is co-located with implementation

### Built-in Guardrails

SliceKit enforces VSA principles through:

- **Slice Isolation**: Components are scoped to their slice by default
- **Explicit Sharing**: Use `@SharedComponent` only when intentional
- **Route Ownership**: Each slice owns exactly one HTTP endpoint
- **Dependency Boundaries**: Child injectors prevent cross-slice contamination

## Quick Start

### 1. Add Dependencies

```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>

<dependencies>
    <dependency>
        <groupId>io.undertow</groupId>
        <artifactId>undertow-core</artifactId>
        <version>2.3.10.Final</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.16.0</version>
    </dependency>
    <dependency>
        <groupId>com.google.inject</groupId>
        <artifactId>guice</artifactId>
        <version>7.0.0</version>
    </dependency>
    <dependency>
        <groupId>javax.inject</groupId>
        <artifactId>javax.inject</artifactId>
        <version>1</version>
    </dependency>
</dependencies>
```

### 2. Create Your First Slice

```java
@Slice(
    name = "create-user", 
    route = "/api/users", 
    method = HttpMethod.POST,
    description = "Creates a new user account"
)
public class CreateUserSlice {
    
    private final UserDataAccess dataAccess;
    private final EmailService emailService;
    
    @Inject
    public CreateUserSlice(UserDataAccess dataAccess, EmailService emailService) {
        this.dataAccess = dataAccess;
        this.emailService = emailService;
    }
    
    @SliceHandler
    public CreateUserResult handle(CreateUserRequest request) {
        // Complete user creation logic here
        String userId = UUID.randomUUID().toString();
        
        User user = dataAccess.saveUser(new User(userId, request.getEmail()));
        emailService.sendWelcomeEmail(user.getEmail());
        
        return new CreateUserResult(userId, "User created successfully");
    }
}
```

### 3. Add Slice Components

```java
@SliceComponent("create-user")
public class UserDataAccess {
    public User saveUser(User user) {
        // Save user logic
        return user;
    }
}

@SliceComponent("create-user") 
public class EmailService {
    public void sendWelcomeEmail(String email) {
        // Send email logic
    }
}
```

### 4. Start Your Application

```java
public class MyApp {
    public static void main(String[] args) {
        SliceKit.builder()
            .scanPackages("com.mycompany.slices")
            .port(8080)
            .start();
    }
}
```

## Core Concepts

### Slices

A **slice** is a complete vertical feature containing:
- HTTP endpoint definition (`@Slice`)
- Request handler (`@SliceHandler`) 
- Business logic and dependencies
- Data access and external integrations

### Components

- **`@SliceComponent("slice-name")`**: Scoped to a specific slice
- **`@SharedComponent`**: Available to multiple slices (use sparingly)
- **`@Inject`**: Constructor dependency injection

### Dependency Isolation

```java
// ✅ Good: Slice-specific component
@SliceComponent("create-order")
public class OrderDataAccess { }

// ⚠️ Use carefully: Shared across slices  
@SharedComponent(sharedWith = {"create-order", "update-order"})
public class OrderValidator { }

// ❌ Avoid: Global shared component
@SharedComponent  
public class GlobalService { }
```

## Examples

SliceKit includes working examples in `src/main/java/io/slicekit/examples/`:

### Simple Slice (No Dependencies)
- **[HelloWorldSlice](src/main/java/io/slicekit/examples/HelloWorldSlice.java)** - Basic GET endpoint
- **[HealthCheckSlice](src/main/java/io/slicekit/examples/HealthCheckSlice.java)** - System health monitoring

### Complex Slice (With Dependencies)  
- **[CreateOrderSlice](src/main/java/io/slicekit/examples/CreateOrderSlice.java)** - POST endpoint with dependency injection
- **[OrderDataAccess](src/main/java/io/slicekit/examples/OrderDataAccess.java)** - Slice-scoped data access
- **[OrderEmailService](src/main/java/io/slicekit/examples/OrderEmailService.java)** - Slice-scoped email service

### Interactive Slice
- **[EchoSlice](src/main/java/io/slicekit/examples/EchoSlice.java)** - POST endpoint with JSON request/response

## Running Examples

```bash
# Compile the project
mvn clean compile

# Start the example application
mvn exec:java -Dexec.mainClass="io.slicekit.examples.SliceKitExampleApp"

# Test the endpoints
curl http://localhost:8080/hello
curl http://localhost:8080/health
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"test","items":["item1"],"totalAmount":29.99}'
```

## Architecture Benefits

### For Development Teams
- **Parallel Development**: Teams can work on different slices simultaneously
- **Easier Onboarding**: New developers can understand one slice at a time
- **Reduced Merge Conflicts**: Changes are isolated to specific slices
- **Clear Ownership**: Each slice has a clear business purpose

### For AI-Assisted Development
- **Complete Context**: AI can see entire workflow in one place
- **Predictable Patterns**: Consistent slice structure across features
- **Isolated Changes**: Modifications don't cascade across the system
- **Self-Contained Testing**: Each slice can be tested independently

### For System Maintenance
- **Independent Deployment**: Slices can potentially be deployed separately
- **Easier Debugging**: Issues are contained within slice boundaries
- **Simplified Testing**: Unit and integration tests are slice-focused
- **Clear Dependencies**: Explicit component scoping and sharing

## Framework Guardrails

SliceKit enforces VSA principles through:

1. **Automatic Slice Discovery**: Scans for `@Slice` annotated classes
2. **Route Conflict Detection**: Prevents duplicate route registrations
3. **Dependency Isolation**: Components are slice-scoped by default
4. **Explicit Sharing**: `@SharedComponent` requires intentional declaration
5. **Constructor Injection**: Enforces dependency declaration at construction time

## Best Practices

### ✅ Do
- Keep slices focused on single business capabilities
- Use descriptive slice and component names
- Minimize use of `@SharedComponent`
- Write integration tests per slice
- Co-locate related functionality within slices

### ❌ Don't
- Create god slices that handle multiple concerns
- Share mutable state between slices
- Use static dependencies or singletons
- Bypass the injection system with direct instantiation
- Create deep inheritance hierarchies across slices

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Requirements

- Java 17+
- Maven 3.6+

## Dependencies

- **Undertow** - Embedded HTTP server
- **Jackson** - JSON serialization/deserialization  
- **Google Guice** - Dependency injection
- **Reflections** - Classpath scanning
- **SLF4J + Logback** - Logging

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- 📖 **Documentation**: Check the examples in `src/main/java/io/slicekit/examples/`
- 🐛 **Issues**: Report bugs and feature requests via GitHub Issues
- 💬 **Discussions**: Use GitHub Discussions for questions and ideas

---

**SliceKit** - *Building features, not layers* 🎯