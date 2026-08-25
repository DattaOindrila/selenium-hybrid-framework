package com.qa.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A user account, used both as an API request payload and as the source of the
 * values typed into the registration form.
 *
 * One model for both layers is what makes the UI/API cross-validation tests
 * meaningful: the same object is sent to POST /api/createAccount and then compared
 * against what the UI renders, so there is no chance of the two drifting apart in
 * the test code itself.
 *
 * The @JsonProperty names match the field names the API returns from
 * GET /api/getUserDetailByEmail (birth_day, first_name, ...), which differ from the
 * form parameter names the same API accepts on create (birth_date, firstname, ...).
 * That asymmetry is the server's, not ours - toFormParams() handles it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccount {

    @JsonProperty("id")            private Integer id;
    @JsonProperty("name")          private String name;
    @JsonProperty("email")         private String email;
                                   private String password;
    @JsonProperty("title")         private String title;
    @JsonProperty("birth_day")     private String birthDay;
    @JsonProperty("birth_month")   private String birthMonth;
    @JsonProperty("birth_year")    private String birthYear;
    @JsonProperty("first_name")    private String firstName;
    @JsonProperty("last_name")     private String lastName;
    @JsonProperty("company")       private String company;
    @JsonProperty("address1")      private String address1;
    @JsonProperty("address2")      private String address2;
    @JsonProperty("country")       private String country;
    @JsonProperty("state")         private String state;
    @JsonProperty("city")          private String city;
    @JsonProperty("zipcode")       private String zipcode;
    @JsonProperty("mobile_number") private String mobileNumber;

    public UserAccount() {
        // required by Jackson
    }

    /**
     * The create/update endpoints are form-encoded, and their parameter names are
     * not the same as the JSON field names the read endpoint returns. Building the
     * map here keeps that mapping in one place.
     */
    public Map<String, String> toFormParams() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", name);
        form.put("email", email);
        form.put("password", password);
        form.put("title", title);
        form.put("birth_date", birthDay);
        form.put("birth_month", birthMonth);
        form.put("birth_year", birthYear);
        form.put("firstname", firstName);
        form.put("lastname", lastName);
        form.put("company", company);
        form.put("address1", address1);
        form.put("address2", address2);
        form.put("country", country);
        form.put("zipcode", zipcode);
        form.put("state", state);
        form.put("city", city);
        form.put("mobile_number", mobileNumber);
        return form;
    }

    // ---------------------------------------------------------------- accessors

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getTitle() { return title; }
    public String getBirthDay() { return birthDay; }
    public String getBirthMonth() { return birthMonth; }
    public String getBirthYear() { return birthYear; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCompany() { return company; }
    public String getAddress1() { return address1; }
    public String getAddress2() { return address2; }
    public String getCountry() { return country; }
    public String getState() { return state; }
    public String getCity() { return city; }
    public String getZipcode() { return zipcode; }
    public String getMobileNumber() { return mobileNumber; }

    public void setId(Integer id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setTitle(String title) { this.title = title; }
    public void setBirthDay(String birthDay) { this.birthDay = birthDay; }
    public void setBirthMonth(String birthMonth) { this.birthMonth = birthMonth; }
    public void setBirthYear(String birthYear) { this.birthYear = birthYear; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setCompany(String company) { this.company = company; }
    public void setAddress1(String address1) { this.address1 = address1; }
    public void setAddress2(String address2) { this.address2 = address2; }
    public void setCountry(String country) { this.country = country; }
    public void setState(String state) { this.state = state; }
    public void setCity(String city) { this.city = city; }
    public void setZipcode(String zipcode) { this.zipcode = zipcode; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    /** Full name as the site renders it in the "Logged in as ..." header. */
    public String getDisplayName() {
        return name;
    }

    @Override
    public String toString() {
        // password deliberately omitted so it can never reach a log or a report
        return "UserAccount{email='" + email + "', name='" + name + "', city='" + city + "'}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written builder: 18 constructor arguments would be unreadable at the call site. */
    public static final class Builder {
        private final UserAccount u = new UserAccount();

        public Builder name(String v)         { u.name = v; return this; }
        public Builder email(String v)        { u.email = v; return this; }
        public Builder password(String v)     { u.password = v; return this; }
        public Builder title(String v)        { u.title = v; return this; }
        public Builder birthDay(String v)     { u.birthDay = v; return this; }
        public Builder birthMonth(String v)   { u.birthMonth = v; return this; }
        public Builder birthYear(String v)    { u.birthYear = v; return this; }
        public Builder firstName(String v)    { u.firstName = v; return this; }
        public Builder lastName(String v)     { u.lastName = v; return this; }
        public Builder company(String v)      { u.company = v; return this; }
        public Builder address1(String v)     { u.address1 = v; return this; }
        public Builder address2(String v)     { u.address2 = v; return this; }
        public Builder country(String v)      { u.country = v; return this; }
        public Builder state(String v)        { u.state = v; return this; }
        public Builder city(String v)         { u.city = v; return this; }
        public Builder zipcode(String v)      { u.zipcode = v; return this; }
        public Builder mobileNumber(String v) { u.mobileNumber = v; return this; }

        public UserAccount build() {
            return u;
        }
    }
}
