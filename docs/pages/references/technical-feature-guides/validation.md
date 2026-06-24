---
layout: default
title: "Validation & Forms"
description: "Validate PikaBeans before they save, and bind web form data onto a bean safely."
active_page: validation
permalink: /pages/validation/
---

# Validation & Forms

Override `validation()` on a bean to check it before every insert or update. Add errors with the helpers; if there are any, the save aborts.

## Validate

```java
public class User extends PikaBean {
    String name;
    String email;
    Integer age;

    @Override
    protected void validation() {
        require("name");          // not null, not blank
        require("email");
        requireUnique("email");   // no other row has this email
        if (age != null && age < 18) {
            addError("age", "must be 18 or older");
        }
    }
}
```

Call `validate()` to run the checks. Read errors with `hasErrors()` and `getErrorString(field)`:

```java
User u = new User();
u.setEmail("nope");

if (!u.validate()) {
    String msg = u.getErrorString("email");  // joined messages for one field
}
```

`save()`, `insert()`, and `update()` run `validation()` first and return `false` if it fails.

## saveOrThrow()

In a web handler you usually want an exception, not a boolean to check:

```java
user.saveOrThrow();   // throws IllegalStateException if validation fails
```

## Bind form data

`setFieldsFrom()` copies values from a request map onto the bean, coercing strings to each field's type. List the fields you allow. Anything not listed is ignored, which prevents mass assignment.

```java
Map<String, String> form = Map.of(
    "name",  "Alice",
    "age",   "25",      // coerced to Integer
    "admin", "true"     // ignored: not in the allowlist
);

User u = new User();
u.setFieldsFrom(form, "name", "age");
u.saveOrThrow();
```

String to non-string conversion uses Pika's [Coercion System]({{ '/pages/mapping/' | relative_url }}).
