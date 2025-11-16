# Package: com.tutorial.taskmanager.constants

This package contains **application-wide constants** used throughout the Task Manager application.

---

## Table of Contents
1. [Package Overview](#package-overview)
2. [Why Constants?](#why-constants)
3. [Constants Files](#constants-files)
4. [The @UtilityClass Pattern](#the-utilityclass-pattern)
5. [Best Practices](#best-practices)
6. [Usage Examples](#usage-examples)
7. [Common Questions](#common-questions)

---

## Package Overview

The `constants` package centralizes **magic strings and numbers** into named constants, providing a single source of truth for values used across multiple parts of the application.

**Package Structure:**
```
com.tutorial.taskmanager.constants/
└── DatabaseTableConstants.java   # Database table and column names
```

**Why a separate package?**
- **Centralization** - All constants in one place
- **Discoverability** - Easy to find and use
- **Organization** - Separate from business logic
- **Reusability** - Accessible from any layer

---

## Why Constants?

### Problem: Magic Strings

**❌ Without constants (magic strings everywhere):**
```java
@Entity
@Table(name = "TASK")  // Magic string
@AttributeOverride(name = "id", column = @Column(name = "TASK_ID"))  // Magic string
public class Task {
    @JoinColumn(name = "APP_USER_ID")  // Magic string
    private AppUser appUser;
}

@Entity
@Table(name = "PROJECT")
@AttributeOverride(name = "id", column = @Column(name = "PROJECT_ID"))
public class Project {
    @JoinColumn(name = "APP_USER_ID")  // Duplicated magic string
    private AppUser appUser;
}
```

**Issues:**
- **Typos** - `"APP_USER_ID"` vs `"APPUSER_ID"` (runtime error)
- **Inconsistency** - Same concept, different strings
- **Maintenance nightmare** - Change in 10 places if schema changes
- **No IDE support** - Can't refactor strings easily
- **Hard to track usage** - Where is this table name used?

---

### Solution: Named Constants

**✅ With constants:**
```java
@UtilityClass
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";
    public static final String TASK_ID_COLUMN = "TASK_ID";
    public static final String APP_USER_ID_COLUMN = "APP_USER_ID";
}

@Entity
@Table(name = DatabaseTableConstants.TASK_TABLE)
@AttributeOverride(name = "id", column = @Column(name = DatabaseTableConstants.TASK_ID_COLUMN))
public class Task {
    @JoinColumn(name = DatabaseTableConstants.APP_USER_ID_COLUMN)
    private AppUser appUser;
}
```

**Benefits:**
- **Type safety** - IDE autocompletes and catches typos
- **Single source of truth** - Change once, updates everywhere
- **Refactor-friendly** - Rename constant → all usages update
- **Searchable** - Find all usages of a constant
- **Self-documenting** - Clear purpose and meaning

---

## Constants Files

### `DatabaseTableConstants.java`

**Purpose:** Centralize database table and column names used in JPA entity mappings.

**Full Implementation:**
```java
package com.tutorial.taskmanager.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DatabaseTableConstants {
    // Generic column names
    public static final String GENERIC_ID_NAME = "id";

    // AppUser table
    public static final String APP_USER_TABLE = "APP_USER";
    public static final String APP_USER_ID_COLUMN = "APP_USER_ID";

    // Task table
    public static final String TASK_TABLE = "TASK";
    public static final String TASK_ID_COLUMN = "TASK_ID";

    // Project table
    public static final String PROJECT_TABLE = "PROJECT";
    public static final String PROJECT_ID_COLUMN = "PROJECT_ID";
}
```

**Organization:**
Constants are grouped by entity/table for clarity:
1. Generic column names (shared across entities)
2. AppUser-related constants
3. Task-related constants
4. Project-related constants

---

### Constant Naming Convention

**Pattern:** `{ENTITY}_{PURPOSE}`

**Examples:**

| Constant | Purpose | Value | Used In |
|----------|---------|-------|---------|
| `GENERIC_ID_NAME` | Base entity ID field name | `"id"` | `@AttributeOverride(name = ...)` |
| `APP_USER_TABLE` | AppUser table name | `"APP_USER"` | `@Table(name = ...)` |
| `APP_USER_ID_COLUMN` | AppUser primary key column | `"APP_USER_ID"` | `@Column(name = ...)`, `@JoinColumn(name = ...)` |
| `TASK_TABLE` | Task table name | `"TASK"` | `@Table(name = ...)` |
| `TASK_ID_COLUMN` | Task primary key column | `"TASK_ID"` | `@Column(name = ...)` |
| `PROJECT_TABLE` | Project table name | `"PROJECT"` | `@Table(name = ...)` |
| `PROJECT_ID_COLUMN` | Project primary key column | `"PROJECT_ID"` | `@Column(name = ...)` |

**Consistency:**
- `*_TABLE` - Table names
- `*_ID_COLUMN` - Primary key column names
- `*_COLUMN` - Other column names (can be added as needed)

---

## The @UtilityClass Pattern

### What is `@UtilityClass`?

**`@UtilityClass`** is a Lombok annotation that creates a class designed to hold only static members (constants, static methods).

**What it does:**
```java
@UtilityClass
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";
}

// Lombok generates:
public final class DatabaseTableConstants {
    private DatabaseTableConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String TASK_TABLE = "TASK";
}
```

**Generated behavior:**
1. **Makes class `final`** - Cannot be extended
2. **Private constructor** - Cannot be instantiated
3. **Throws exception if constructor called** - Prevents reflection-based instantiation
4. **All methods implicitly static** - No need to write `static` keyword

---

### Why @UtilityClass?

**Without `@UtilityClass`:**
```java
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";
}

// Problem: Can be instantiated (pointless)
DatabaseTableConstants constants = new DatabaseTableConstants();  // Why?

// Problem: Can be extended (pointless)
public class MyConstants extends DatabaseTableConstants {}  // Why?
```

**With `@UtilityClass`:**
```java
@UtilityClass
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";
}

// Compiler error: Cannot instantiate
DatabaseTableConstants constants = new DatabaseTableConstants();  // ❌ Compiler error

// Compiler error: Cannot extend
public class MyConstants extends DatabaseTableConstants {}  // ❌ Compiler error
```

**Benefits:**
- **Prevents misuse** - Class is clearly for constants only
- **Best practice enforcement** - Can't be instantiated or extended
- **Less boilerplate** - No need to manually write private constructor
- **Clear intent** - Signals this is a utility class

---

### Alternative: Manual Utility Class

**If not using Lombok:**
```java
public final class DatabaseTableConstants {
    // Private constructor to prevent instantiation
    private DatabaseTableConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String TASK_TABLE = "TASK";
    public static final String TASK_ID_COLUMN = "TASK_ID";
}
```

**`@UtilityClass` saves this boilerplate!**

---

## Best Practices

### 1. Use Meaningful Names

**✅ DO:**
```java
public static final String APP_USER_TABLE = "APP_USER";
public static final String TASK_ID_COLUMN = "TASK_ID";
```

**❌ DON'T:**
```java
public static final String TBL1 = "APP_USER";  // Unclear
public static final String C1 = "TASK_ID";     // Meaningless
```

---

### 2. Group Related Constants

**✅ DO:**
```java
@UtilityClass
public class DatabaseTableConstants {
    // AppUser table
    public static final String APP_USER_TABLE = "APP_USER";
    public static final String APP_USER_ID_COLUMN = "APP_USER_ID";

    // Task table
    public static final String TASK_TABLE = "TASK";
    public static final String TASK_ID_COLUMN = "TASK_ID";
}
```

**❌ DON'T:**
```java
// Unorganized mess
public static final String TASK_TABLE = "TASK";
public static final String APP_USER_ID_COLUMN = "APP_USER_ID";
public static final String PROJECT_TABLE = "PROJECT";
public static final String TASK_ID_COLUMN = "TASK_ID";
```

**Add comments to separate logical groups!**

---

### 3. Naming Convention

**Standard Java convention for constants:**
```java
public static final String MY_CONSTANT = "value";
// ALL_CAPS_WITH_UNDERSCORES
```

**Why?**
- Instantly recognizable as constants
- Industry standard
- Follows Java naming conventions

---

### 4. One Constants Class per Domain

**Current structure:**
```
constants/
└── DatabaseTableConstants.java  # Database-related constants
```

**Future growth (examples):**
```
constants/
├── DatabaseTableConstants.java   # Database table/column names
├── ValidationConstants.java      # Validation messages, regex patterns
├── SecurityConstants.java        # Roles, permissions
├── ApiConstants.java             # API paths, versions
└── CacheConstants.java           # Cache names, TTLs
```

**Why separate files?**
- Clear purpose
- Easy to find what you need
- Prevents giant "Constants.java" file

---

### 5. Don't Overuse Constants

**✅ Good use case - Reused value:**
```java
public static final String APP_USER_ID_COLUMN = "APP_USER_ID";
// Used in multiple entities (Task, Project)
```

**❌ Poor use case - Single-use value:**
```java
public static final int ONE = 1;  // Pointless
public static final String HELLO = "Hello";  // Not reused
```

**Rule of thumb:** If a value is used in **2+ places** or has **business meaning**, make it a constant.

---

## Usage Examples

### In Entity Mappings

**Table names:**
```java
@Entity
@Table(name = DatabaseTableConstants.TASK_TABLE)
public class Task extends BaseEntity {
    // ...
}
```

**Column names with `@AttributeOverride`:**
```java
@AttributeOverride(
    name = DatabaseTableConstants.GENERIC_ID_NAME,  // "id" field
    column = @Column(name = DatabaseTableConstants.TASK_ID_COLUMN)  // "TASK_ID" column
)
public class Task extends BaseEntity {
    // Inherits "id" field from BaseEntity
    // But database column is named "TASK_ID"
}
```

**Join columns (foreign keys):**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = DatabaseTableConstants.APP_USER_ID_COLUMN)
private AppUser appUser;
// Foreign key column: APP_USER_ID
```

---

### In Repository Queries (Future)

**Native SQL queries:**
```java
@Query(value = "SELECT * FROM " + DatabaseTableConstants.TASK_TABLE +
               " WHERE " + DatabaseTableConstants.APP_USER_ID_COLUMN + " = :userId",
       nativeQuery = true)
List<Task> findTasksByUserId(@Param("userId") Long userId);
```

**Benefits:**
- If table name changes, query updates automatically
- No risk of typos in SQL

---

### IDE Auto-Completion

**Typing in entity:**
```java
@Table(name = DatabaseTableConstants.
// IDE suggests:
// - APP_USER_TABLE
// - TASK_TABLE
// - PROJECT_TABLE
```

**Refactoring:**
```java
// Rename constant:
public static final String TASK_TABLE = "TASK_TABLE";  // Changed value

// All usages automatically updated!
// No manual find-and-replace needed
```

---

## Common Questions

### Q1: Why use constants for database names instead of hardcoding?

**A:** Maintainability and refactoring.

**Scenario: Table rename**

**With constants:**
```java
// Change in ONE place:
public static final String TASK_TABLE = "TASKS";  // Changed from "TASK"

// All usages update automatically (20+ places)
```

**Without constants:**
```java
// Must find and replace in 20+ places:
@Table(name = "TASK")           // Change to "TASKS"
"SELECT * FROM TASK"            // Change to "TASKS"
"INSERT INTO TASK VALUES"       // Change to "TASKS"
// ... 17 more places ...

// High risk of missing one!
```

---

### Q2: Can I use constants from other classes?

**A:** Yes! Constants are public and static.

**Example:**
```java
package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.constants.DatabaseTableConstants;

@Service
public class TaskService {
    public void logTableName() {
        System.out.println("Task table: " + DatabaseTableConstants.TASK_TABLE);
    }
}
```

**Static import (optional):**
```java
import static com.tutorial.taskmanager.constants.DatabaseTableConstants.*;

@Entity
@Table(name = TASK_TABLE)  // No prefix needed
public class Task {
    @JoinColumn(name = APP_USER_ID_COLUMN)
    private AppUser appUser;
}
```

**Be careful with static imports!**
- Can make code less clear (where does `TASK_TABLE` come from?)
- Use sparingly, only for frequently-used constants

---

### Q3: Should I create constants for everything?

**A:** No, only for values that:
1. **Are reused** in multiple places
2. **Have business meaning** (not arbitrary values)
3. **Might change** in the future

**✅ Good candidates:**
```java
public static final String DEFAULT_PAGE_SIZE = "20";  // Reused in pagination
public static final String DATE_FORMAT = "yyyy-MM-dd";  // Business standard
public static final int MAX_LOGIN_ATTEMPTS = 3;  // Security policy
```

**❌ Poor candidates:**
```java
public static final int ZERO = 0;  // Obvious
public static final String EMPTY = "";  // Use String.isEmpty()
public static final boolean TRUE = true;  // Pointless
```

---

### Q4: What's the difference between `@UtilityClass` and `final class`?

**A:**

**`@UtilityClass` (Lombok):**
```java
@UtilityClass
public class DatabaseTableConstants {}

// Generates:
public final class DatabaseTableConstants {
    private DatabaseTableConstants() {
        throw new UnsupportedOperationException(...);
    }
}
```

**Manual `final class`:**
```java
public final class DatabaseTableConstants {
    private DatabaseTableConstants() {}  // You write this
}
```

**`@UtilityClass` advantages:**
- Less boilerplate
- Throws exception if instantiated (better enforcement)
- Clear intent

**Manual advantages:**
- No Lombok dependency
- More explicit

**Both achieve the same goal!**

---

### Q5: Can I have non-constant fields in a constants class?

**A:** Technically yes, but **you shouldn't!**

**❌ BAD:**
```java
@UtilityClass
public class DatabaseTableConstants {
    public static final String TASK_TABLE = "TASK";  // ✓
    public static String currentTable = "TASK";      // ❌ Mutable!
}
```

**Why bad?**
- Not a constant (can be changed)
- Defeats the purpose of constants class
- Confusing to users

**Rule:** Constants classes should only have `static final` fields.

---

### Q6: When should I create a new constants class?

**A:** When you have a logical grouping of related constants.

**Examples:**

**Database constants:**
```java
@UtilityClass
public class DatabaseTableConstants {
    // All database-related constants
}
```

**Validation constants:**
```java
@UtilityClass
public class ValidationConstants {
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,20}$";
    public static final String EMAIL_PATTERN = "...";
    public static final int MIN_PASSWORD_LENGTH = 8;
}
```

**API constants:**
```java
@UtilityClass
public class ApiConstants {
    public static final String API_V1_PATH = "/api/v1";
    public static final String TASKS_PATH = "/tasks";
    public static final String USERS_PATH = "/users";
}
```

**Rule:** One constants class per **logical domain/concern**.

---

### Q7: What if I need different table names per environment?

**A:** Use Spring's `@Value` and configuration properties instead.

**For environment-specific values, use `application.yml`:**
```yaml
# application-dev.yml
database:
  task-table: TASK_DEV

# application-prod.yml
database:
  task-table: TASK_PROD
```

**In code:**
```java
@Configuration
public class DatabaseConfig {
    @Value("${database.task-table}")
    private String taskTableName;
}
```

**Constants are for values that are truly constant across environments!**

---

## When NOT to Use Constants

### 1. Configuration that varies by environment
**Use:** `application.yml` and `@Value` or `@ConfigurationProperties`

### 2. Enum-like values
**Use:** Java enums (e.g., `TaskStatus.TODO`)

### 3. Single-use values
**Use:** Inline literals (no point in constant)

### 4. Generated values
**Use:** Factory methods or builders

---

## Future Enhancements

As the application grows, you might add:

**`ValidationConstants.java`:**
```java
@UtilityClass
public class ValidationConstants {
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final String INVALID_USERNAME_MESSAGE = "Username must be 3-20 characters";
}
```

**`SecurityConstants.java`:**
```java
@UtilityClass
public class SecurityConstants {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final int SESSION_TIMEOUT_MINUTES = 30;
}
```

**`CacheConstants.java`:**
```java
@UtilityClass
public class CacheConstants {
    public static final String TASKS_CACHE = "tasks";
    public static final String USERS_CACHE = "users";
    public static final long CACHE_TTL_SECONDS = 3600;
}
```

---

## Next Steps

Now that you understand constants:

1. **Use constants in validation** - Create `ValidationConstants` for validation rules
2. **Centralize API paths** - Create `ApiConstants` when building REST controllers
3. **Add security constants** - Create `SecurityConstants` when implementing Spring Security

---

## Related Documentation

- [../../docs/07-spring-data-jpa.md](../../docs/07-spring-data-jpa.md) - JPA entities guide
- [../model/PACKAGE-INFO.md](../model/PACKAGE-INFO.md) - Entity documentation
- [../enums/PACKAGE-INFO.md](../enums/PACKAGE-INFO.md) - Enum documentation

---

## Additional Resources

- [Effective Java (Item 30)](https://www.oreilly.com/library/view/effective-java/9780134686097/) - Use enums instead of int constants
- [Java Naming Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html)
- [Lombok @UtilityClass](https://projectlombok.org/features/experimental/UtilityClass)

---

**Last Updated:** 2025-11-15
**Package Status:** ✅ Complete and production-ready
