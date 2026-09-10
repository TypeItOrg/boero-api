# Instancio

Optional fixture generation for tasks already using Instancio or explicitly adding it. Prefer the repository's existing deterministic factories; field count alone is not a reason to introduce a dependency.

## When to Use

- Repetitive fixture setup for which existing factories are insufficient and generation is part of the requested scope
- Setting up test data for repositories
- Creating DTOs for controller tests
- Avoiding repetitive builder/setter calls

## Dependency

If adding Instancio is explicitly in scope, declare the agreed version as a Gradle test dependency in `build.gradle`. The following examples assume that dependency already exists. Do not run an installation merely to read this reference.

## Basic Usage

### Simple Object

```java
final var order = Instancio.create(Order.class);
// All fields populated with random data
```

### List of Objects

```java
final var orders = Instancio.ofList(Order.class).size(5).create();
// 5 orders with random data
```

## Customizing Values

### Set Specific Fields

```java
final var order = Instancio.of(Order.class)
  .set(field(Order::getStatus), "PENDING")
  .set(field(Order::getTotal), new BigDecimal("99.99"))
  .create();
```

### Supply Generated Values

```java
final var order = Instancio.of(Order.class)
  .supply(field(Order::getEmail), () -> "user" + UUID.randomUUID() + "@test.com")
  .create();
```

### Ignore Fields

```java
final var order = Instancio.of(Order.class)
  .ignore(field(Order::getId)) // Let DB generate
  .create();
```

## Complex Objects

### Nested Objects

```java
final var order = Instancio.of(Order.class)
  .set(field(Order::getCustomer), Instancio.create(Customer.class))
  .set(field(Order::getItems), Instancio.ofList(OrderItem.class).size(3).create())
  .create();
```

### All Fields Random

```java
// When you need fully random but valid data
final var randomOrder = Instancio.create(Order.class);
// Customer, items, addresses - all populated
```

## Spring Boot Integration

### Repository Test Setup

```java
@DataJpaTest
@AutoConfigureTestDatabase
@Testcontainers
class OrderRepositoryTest {
  
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
  
  @Autowired
  private OrderRepository orderRepository;
  
  @Test
  void shouldFindOrdersByStatus() {
    // Given: Create 10 random orders with PENDING status
    final var orders = Instancio.ofList(Order.class)
      .size(10)
      .set(field(Order::getStatus), "PENDING")
      .create();
    
    orderRepository.saveAll(orders);
    
    // When
    final var found = orderRepository.findByStatus("PENDING");
    
    // Then
    assertThat(found).hasSize(10);
  }
}
```

### Controller Test Setup

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
  
  @Autowired
  private MockMvcTester mvc;
  
  @MockitoBean
  private OrderService orderService;
  
  @Test
  void shouldReturnOrder() {
    // Given: Random order with specific ID
    Order order = Instancio.of(Order.class)
      .set(field(Order::getId), 1L)
      .create();
    
    given(orderService.findById(1L)).willReturn(order);
    
    // When/Then
    assertThat(mvc.get().uri("/orders/1"))
      .hasStatus(HttpStatus.OK)
      .bodyJson()
      .convertTo(OrderResponse.class)
      .satisfies(response -> {
        assertThat(response.getId()).isEqualTo(1L);
      });
  }
}
```

## Patterns

### Builder Pattern Alternative

```java
// Instead of:
Order order = Order.builder()
  .id(1L)
  .status("PENDING")
  .customer(Customer.builder().name("John").build())
  .items(List.of(
    OrderItem.builder().product("A").price(10).build(),
    OrderItem.builder().product("B").price(20).build()
  ))
  .build();

// Use:
Order order = Instancio.of(Order.class)
  .set(field(Order::getId), 1L)
  .set(field(Order::getStatus), "PENDING")
  .create();
// Customer and items auto-generated
```

### Seeded Data

```java
// Consistent "random" data for reproducible tests
Order order = Instancio.of(Order.class)
  .withSeed(12345L)
  .create();
// Same data every test run with seed 12345
```

## Common Patterns

### Email Generation

```java
String email = Instancio.gen().net().email();
```

### Date Generation

```java
LocalDateTime createdAt = Instancio.gen().temporal()
  .localDateTime()
  .past()
  .create();
```

### String Patterns

```java
String phone = Instancio.gen().text().pattern("+1-###-###-####");
```

## Comparison

| Approach | Lines of Code | Maintainability |
| -------- | ------------- | --------------- |
| Manual setters | 10-20 | Low |
| Builder pattern | 5-10 | Medium |
| **Instancio** | 2-5 | **High** |

## Best Practices

1. **Use deliberately** - Reuse existing factories first; do not choose a generator by property count
2. **Set only what's relevant** - Let Instancio fill the rest
3. **Use with Testcontainers** - Great for database seeding
4. **Set IDs explicitly** - When testing specific scenarios
5. **Ignore auto-generated fields** - Like createdAt, updatedAt

## Links

- [Instancio Documentation](https://www.instancio.org/)
- [JUnit 5 Extension](https://www.instancio.org/user-guide/#junit-integration)
