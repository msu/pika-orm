package bigsky.pika.integration.model.beans;

import bigsky.pika.bean.EnterprisePikaBean;
import bigsky.pika.query.PikaClassFinder;

public class CustomerBean extends EnterprisePikaBean {

    int customerId;
    String firstName;
    String lastName;
    String company;
    String address;
    String city;
    String state;
    String country;
    String postalCode;
    String phone;
    String fax;
    String email;
    int supportRepId;

    // Getters and setters
    public int getCustomerId() {
        return customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getSupportRepId() {
        return supportRepId;
    }

    public void setSupportRepId(int supportRepId) {
        this.supportRepId = supportRepId;
    }

    public static PikaClassFinder<CustomerBean> find() {
        return find(CustomerBean.class);
    }

    @Override
    protected void validation() {
        if (firstName == null || firstName.trim().isEmpty()) {
            addError("firstName", "First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            addError("lastName", "Last name cannot be null or empty");
        }
        if (email != null && !email.contains("@")) {
            addError("email", "Email must contain '@' symbol");
        }
    }
}