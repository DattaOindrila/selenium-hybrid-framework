package com.qa.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One entry of GET /api/brandsList. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Brand {

    @JsonProperty("id")    private int id;
    @JsonProperty("brand") private String brand;

    public int getId() { return id; }
    public String getBrand() { return brand; }
    public void setId(int id) { this.id = id; }
    public void setBrand(String brand) { this.brand = brand; }

    @Override
    public String toString() {
        return "Brand{id=" + id + ", brand='" + brand + "'}";
    }
}
