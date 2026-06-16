---
layout: default
title: "Custom Field Mapping and Metadata"
description: "PikaORM Custom Field Mapping: FieldMapping, transformForDB, transformFromDB, JSON serialization, and collection types."
active_page: custom-field-mapping
permalink: /pages/custom-field-mapping/
---

# Custom Field Mapping and Metadata

This guide covers how to create custom field mappings and metadata transformations in PikaORM, allowing you to handle complex data types, custom serialization, and specialized database storage requirements.

## Overview

PikaORM's mapping system provides several extension points for customizing how your Java objects are mapped to database columns:

- **Custom Field Mappings**: Override default field-to-column behavior
- **Data Transformations**: Convert between Java types and database storage formats
- **Metadata Customization**: Handle complex types like JSON, collections, and custom objects
- **Column Specifications**: Control database column properties

## Core Mapping Components

### *FieldMapping* Class

The `FieldMapping` class is the core component that defines how a field maps to a database column:

```java
public class FieldMapping {
    // Core configuration methods
    FieldMapping toColumn(String columnName)           // Set custom column name
    FieldMapping asType(Class<?> dbClass)              // Set database storage type
    FieldMapping asId()                                // Mark as ID column
    FieldMapping asVersionColumn()                     // Mark as version column
    
    // Data transformation methods
    FieldMapping transformForDB(UnaryOperator<Object> func)    // Java -> Database
    FieldMapping transformFromDB(UnaryOperator<Object> func)   // Database -> Java
    FieldMapping withVersionIncrementer(UnaryOperator<Object> incrementer)
}
```

## Custom Mapping Examples

### JSON Field Mapping

Store Java objects as JSON strings in the database:

```java
public class HasCustomizedMetadata {
    
    @Override
    public void migrations()
    {
        add(this::customMetadataExample);
    }
                
    public PikaMigration customMetadataExample() {
        return makeMigration("customMetadataExample")
                .up("""
                        CREATE TABLE IF NOT EXISTS foos (
                        id INTEGER PRIMARY KEY,
                        json TEXT
                  	  );
                        """)
                .down("""
                        DROP TABLE foos;
                        """);
    }

    public static Mapping mapping() {
        Gson gson = new Gson();
        return new Mapping() {
            @Override
            public String mapToTable() {
                return "foos";
            }
            @Override
            public FieldMapping mapField(Field field) {
                return switch (field.getName()) {
                    case "ignoreMe" -> ignore(field);
                    case "myId" -> map(field).toColumn("id").asId();
                    case "json" -> map(field).asType(String.class).transformForDB(gson::toJson)
                            .transformFromDB(val -> gson.fromJson(String.valueOf(val), Map.class));
                    default -> defaultMapping(field);
                };
            }
        };
    }

    String ignoreMe;
    long myId;
    Map json;

    public void setMap(Map foo) {
        this.json = foo;
    }

    public Map getMap() {
        return json;
    }

    public Object getId() {
        return myId;
    }
}

```

> All customized mapping is done by overriding the methods for metadata mapping with the `Mapping` Class as well as `FieldMapping`. We recommend using the `switch` and `case` statement for better readability with your custom classes. This also shows the setup in addition to a standard `Migration` layout for Pika as well

### Example of full Collection Serialization Custom Mapping

Handle List or Set fields by storing them as delimited strings using the `switch` `case` pattern:

```java
public class BlogPost {
        
	@Override 
    public void migrations(){
        add(this::addMigrationBlogPost);
    }
    
    public PikaMigration addMigrationBlogPost() {
    return makeMigration("BlogPost")
            .up("""
                    CREATE TABLE blog_post (
                    id BIGINT PRIMARY KEY,
                    title VARCHAR(255),
                    content TEXT,
                    tags_csv TEXT,
                    category_ids TEXT,
                    keywords_set TEXT,
                    version_num INTEGER DEFAULT 1
                	);
                    """)
            .down("""
                    DROP TABLE blog_post;
                    """);
	}

    
    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private List<Long> categoryIds;
    private Set<String> keywords;
    private Integer version;
    
    // Standard getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public Set<String> getKeywords() { return keywords; }
    public void setKeywords(Set<String> keywords) { this.keywords = keywords; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    // Custom mapping inner class
    public static class MyMapping extends Mapping {
        @Override
        protected FieldMapping mapField(Field field) {
            switch (field.getName()) {
                case "tags":
                    return map(field)
                        .toColumn("tags_csv")
                        .asType(String.class)
                        .transformForDB(this::serializeStringList)
                        .transformFromDB(this::deserializeStringList);
                        
                case "categoryIds":
                    return map(field)
                        .toColumn("category_ids")
                        .asType(String.class)
                        .transformForDB(this::serializeLongList)
                        .transformFromDB(this::deserializeLongList);
                        
                case "keywords":
                    return map(field)
                        .toColumn("keywords_set")
                        .asType(String.class)
                        .transformForDB(this::serializeStringSet)
                        .transformFromDB(this::deserializeStringSet);
                        
                case "version":
                    return map(field)
                        .toColumn("version_num")
                        .asVersionColumn()
                        .withVersionIncrementer(v -> v == null ? 1 : ((Integer) v) + 1);
                        
                default:
                    return defaultMapping(field);
            }
        }
        
        private String serializeStringList(Object obj) {
            if (obj == null) return null;
            List<String> list = (List<String>) obj;
            return String.join(",", list);
        }
        
        private List<String> deserializeStringList(Object csv) {
            if (csv == null || ((String) csv).trim().isEmpty()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(Arrays.asList(((String) csv).split(",")));
        }
        
        private String serializeLongList(Object obj) {
            if (obj == null) return null;
            List<Long> list = (List<Long>) obj;
            return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        }
        
        private List<Long> deserializeLongList(Object csv) {
            if (csv == null || ((String) csv).trim().isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.stream(((String) csv).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        }
        
        private String serializeStringSet(Object obj) {
            if (obj == null) return null;
            Set<String> set = (Set<String>) obj;
            return String.join(",", set);
        }
        
        private Set<String> deserializeStringSet(Object csv) {
            if (csv == null || ((String) csv).trim().isEmpty()) {
                return new HashSet<>();
            }
            return new HashSet<>(Arrays.asList(((String) csv).split(",")));
        }
    }
}
```

> Great example of a full customized remapping, that is compatible and modular with custom data types, the options are endless!

## Usage with ORM

Register and use your custom mapping:

```java
@Test
public void testCustomMapping() {
    // init the ORM
    PikaORM orm = new PikaORM("jdbc:sqlite:web.db")
            .withLogLevel(TRACE) //check out the logging page for more info
            .makeDefaultORM()

    // The ORM will automatically detect and use the inner Mapping class
    HasCustomizedMetadata entity = new HasCustomizedMetadata();
    entity.setMap(Map.of("foo", 1.0, "bar", 2.0));

    
    orm.insert(entity);
    
    var retrieved = orm.find(HasCustomizedMetadata.class).byId(entity.getId());
 
}
```

> Overall the sky is the limit! We can't wait to see what kind of crazy migrations and custom metadata architecture the community could create, we hope these examples can point you in the right direction.