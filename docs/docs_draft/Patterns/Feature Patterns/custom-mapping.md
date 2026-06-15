---
title: "Custom Mapping Pattern"
layout: default
---

# Custom Mapping Pattern

PikaORM relies heavily on default conventions (e.g., camelCase Java fields map to snake_case database columns). However, when you are working with legacy databases or need to serialize complex types (like JSON or lists), you must override these conventions.

The **Custom Mapping Pattern** is how you achieve this cleanly.

## The `Mapping.mapping()` Method

If PikaORM finds a `public static Mapping mapping()` method on your domain class, it will use the returned `Mapping` object instead of its default reflection-based mapping.

This keeps all database-specific mapping logic encapsulated directly within the class it belongs to.

### Pattern Example: Mapping a JSON Field

Imagine you want to store a Java `Map` in a single `TEXT` column as a JSON string.

```java
import com.google.gson.Gson;
import edu.montana.pika.mapping.Mapping;
import edu.montana.pika.mapping.FieldMapping;

public class UserPreferences {

    private long userId;
    private Map<String, Object> settings;

    // Standard getters/setters...
    public long getUserId() { return userId; }
    public Map<String, Object> getSettings() { return settings; }
    
    // ---------------------------------------------------------
    // The Custom Mapping Pattern
    // ---------------------------------------------------------
    public static Mapping mapping() {
        Gson gson = new Gson();
        
        return new Mapping() {
            @Override
            public String mapToTable() {
                // Override the table name
                return "user_prefs_table"; 
            }
            
            @Override
            public FieldMapping mapField(Field field) {
                return switch (field.getName()) {
                    case "userId" -> map(field)
                                        .toColumn("id")
                                        .asId(); // Mark as primary key
                    
                    case "settings" -> map(field)
                                        .toColumn("json_settings")
                                        .asType(String.class)
                                        // Java -> DB (Serialize to JSON)
                                        .transformForDB(gson::toJson)
                                        // DB -> Java (Deserialize from JSON)
                                        .transformFromDB(val -> gson.fromJson(String.valueOf(val), Map.class));
                    
                    default -> defaultMapping(field); // Fallback for other fields
                };
            }
        };
    }
}
```

## Key Aspects of the Pattern

1. **Inline anonymous class**: We return `new Mapping() { ... }` which allows us to selectively override only the methods we care about (usually `mapToTable` and `mapField`).
2. **Switch statement**: Using Java's modern switch expression on `field.getName()` is the cleanest way to route field-specific configurations.
3. **`defaultMapping(field)` fallback**: Always include a `default` case that returns `defaultMapping(field)`. This ensures that if you add new, simple fields to the class later, they are mapped automatically without requiring updates to the switch block.
4. **`transformForDB` and `transformFromDB`**: These hooks let you define how data transitions between your Java type and the SQL type defined by `asType()`.

For exhaustive details on what you can configure, see the [Custom Field Mapping and Metadata]({{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/custom-field-mapping.md) technical guide.
