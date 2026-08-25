package com.qa.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One entry of GET /api/productsList and POST /api/searchProduct. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    @JsonProperty("id")       private int id;
    @JsonProperty("name")     private String name;
    @JsonProperty("price")    private String price;   // the API returns "Rs. 500", not a number
    @JsonProperty("brand")    private String brand;
    @JsonProperty("category") private Category category;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getBrand() { return brand; }
    public Category getCategory() { return category; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(String price) { this.price = price; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setCategory(Category category) { this.category = category; }

    /** "Rs. 500" -> 500, for comparisons against the total shown in the cart. */
    public int getPriceValue() {
        return Integer.parseInt(price.replaceAll("[^0-9]", ""));
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price='" + price + "', brand='" + brand + "'}";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Category {
        @JsonProperty("category") private String category;
        @JsonProperty("usertype") private UserType usertype;

        public String getCategory() { return category; }
        public UserType getUsertype() { return usertype; }
        public void setCategory(String category) { this.category = category; }
        public void setUsertype(UserType usertype) { this.usertype = usertype; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserType {
        @JsonProperty("usertype") private String usertype;

        public String getUsertype() { return usertype; }
        public void setUsertype(String usertype) { this.usertype = usertype; }
    }
}
