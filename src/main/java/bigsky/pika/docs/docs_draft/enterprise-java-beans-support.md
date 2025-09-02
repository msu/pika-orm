# PikaORM Enterprise Java Bean Support

PikaORM provides a custom enterprise bean implementation through `EnterprisePikaBean` that combines ORM functionality with validation and lifecycle management. Unlike traditional Java Enterprise Beans (EJBs), this is a lightweight ORM-focused bean system to replace the stupidity of `EnterpriseJavaBeans`.

## Key Features

• **Automatic persistence tracking** 

- Beans know if they're persisted or new 
- **Built-in validation system** with field-level error handling 
- **Change tracking** - Only updates modified fields 
- **Relationship loading** - Support for one-to-many and many-to-many relationships
- **Lifecycle hooks** - Callbacks for database operations

## Basic Usage

### Creating a Bean Class

```java
public class User extends EnterprisePikaBean {
    private String name;
    private String email;
    private Integer age;
    
    // getters and setters...
    
    @Override
    protected void validation() {
        if (name == null || name.trim().isEmpty()) {
            addError("name", "Name is required");
        }
        if (email == null || !email.contains("@")) {
            addError("email", "Valid email is required");
        }
    }
}
```

### CRUD Operations

```java
// Create new record
User user = new User();
user.setName("John Doe");
user.setEmail("john@example.com");

if (user.validate()) {
    Long id = user.insert(); // Returns generated ID
}

// Update existing record
user.setName("Jane Doe");
user.save(); // Automatically calls update() since persisted

// Delete record
user.delete();
```

### Validation and Error Handling

```java
User user = new User();
user.setEmail("invalid-email");

if (!user.validate()) {
    // Check for general errors
    List<String> generalErrors = user.getGeneralErrors();
    
    // Check field-specific errors
    if (user.hasError("email")) {
        String emailErrors = user.getErrorString("email");
        System.out.println("Email errors: " + emailErrors);
    }
}
```

### Loading Relationships

```java
// Load one-to-many relationship
PikaManyQuery<Order> orders = user.loadMany(Order.class);

// Load many-to-many through join table
PikaManyThroughQuery<UserRole, Role> roles = 
    user.loadManyThrough(UserRole.class, Role.class);

// Load single related entity
Company company = user.load(Company.class);
```

> Keep looking below for more information on 1-N relationships in Pika

### Querying

```java
// Find records
PikaClassFinder<User> finder = User.find(User.class);
PikaList<User> users = finder.where("age = :val", Map.of("val", 10)).toList();

// Single record
User user = User.find(User.class).where("email = :email", Map.of("email", "joe@gmail.com").fetchFirst();
```

## Important Concepts

### Persistence State

- **New beans**: `isPersisted()` returns `false`, can only call `insert()`
- **Persisted beans**: `isPersisted()` returns `true`, can call `update()` or `delete()`
- **Smart save**: `save()` automatically chooses `insert()` or `update()`

### Change Tracking

- Original values are stored after loading from database
- `beforeUpdate()` automatically removes unchanged fields from updates
- Access original values with `getOriginalValue(fieldName)`

### Field Mapping

- Supports automatic field mapping from database columns
- `setFieldsFrom()` allows bulk field setting from maps or suppliers
- Handles camelCase to snake_case conversion automatically

## Java Enterprise Beans Context

Traditional Java Enterprise Beans (EJBs) are server-side components in Java EE that provide:

- **Session Beans**: Stateless/Stateful business logic components
- **Message-Driven Beans**: Asynchronous message processing
- **Container-managed services**: Transactions, security, concurrency

PikaORM's `EnterprisePikaBean` differs by focusing specifically on:

- **Data persistence** rather than business services
- **Lightweight design** without container dependencies
- **Direct database mapping** with built-in validation
- **Simple POJO approach** that can work in any Java environment
