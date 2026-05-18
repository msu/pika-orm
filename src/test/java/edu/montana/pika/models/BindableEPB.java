package edu.montana.pika.models;

import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.query.PikaClassFinder;

public class BindableEPB extends EnterprisePikaBean {

    public static final String DDL = """
            CREATE TABLE IF NOT EXISTS bindable_epbs (
                id INTEGER PRIMARY KEY,
                first_name TEXT,
                age INTEGER,
                admin BOOLEAN
            );
            """;

    private Long id;
    private String firstName;
    private Integer age;
    private Boolean admin;

    public transient boolean firstNameSetterCalled;

    public BindableEPB() {
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public Integer getAge() {
        return age;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setFirstName(String s) {
        this.firstNameSetterCalled = true;
        this.firstName = s == null ? null : s.trim();
    }

    public static PikaClassFinder<BindableEPB> find() {
        return orm().find(BindableEPB.class);
    }
}