package grug.db.models.chinook.beans;

import grug.db.GrugORM;
import grug.db.GrugORM.EnterpriseGrugBean;
import grug.db.GrugORM.ResultList;

import java.util.Date;

public class EmployeeBean extends EnterpriseGrugBean {

    Long employeeId;
    String lastName;
    String firstName;
    String title;
    Long reportsTo;
    Date birthDate;
    Date hireDate;
    String address;
    String city;
    String state;
    String country;
    String postalCode;
    String phone;
    String fax;
    String email;

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getReportsTo() {
        return reportsTo;
    }

    public void setReportsTo(Long reportsTo) {
        this.reportsTo = reportsTo;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
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

    public static GrugORM.GrugFinder<EmployeeBean> find() {
        return orm().find(EmployeeBean.class);
    }

    public ResultList<EmployeeBean> getReports() {
        return loadN(EmployeeBean.class, "ReportsTo");
    }

    public EmployeeBean getBoss() {
        return load1(EmployeeBean.class, "ReportsTo");
    }

}
