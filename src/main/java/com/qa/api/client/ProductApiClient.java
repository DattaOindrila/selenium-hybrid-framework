package com.qa.api.client;

import com.qa.api.model.Brand;
import com.qa.api.model.Product;
import com.qa.constants.AppConstants;
import io.restassured.response.Response;

import java.util.List;

import static io.restassured.RestAssured.given;

/** Catalogue endpoints: products, brands, search. */
public final class ProductApiClient extends BaseApiClient {

    public Response getAllProducts() {
        log.info("GET {}", AppConstants.API_PRODUCTS_LIST);
        return given().spec(spec()).when().get(AppConstants.API_PRODUCTS_LIST).then().extract().response();
    }

    /** Documented as unsupported - the API answers responseCode 405. */
    public Response postToProductsList() {
        log.info("POST {} (expected to be rejected)", AppConstants.API_PRODUCTS_LIST);
        return given().spec(spec()).when().post(AppConstants.API_PRODUCTS_LIST).then().extract().response();
    }

    public Response getAllBrands() {
        log.info("GET {}", AppConstants.API_BRANDS_LIST);
        return given().spec(spec()).when().get(AppConstants.API_BRANDS_LIST).then().extract().response();
    }

    /** Documented as unsupported - the API answers responseCode 405. */
    public Response putToBrandsList() {
        log.info("PUT {} (expected to be rejected)", AppConstants.API_BRANDS_LIST);
        return given().spec(spec()).when().put(AppConstants.API_BRANDS_LIST).then().extract().response();
    }

    public Response searchProduct(String searchTerm) {
        log.info("POST {} search_product={}", AppConstants.API_SEARCH_PRODUCT, searchTerm);
        return given().spec(spec())
                .formParam("search_product", searchTerm)
                .when().post(AppConstants.API_SEARCH_PRODUCT)
                .then().extract().response();
    }

    /** Deliberately omits the required parameter - the API answers responseCode 400. */
    public Response searchProductWithoutParameter() {
        log.info("POST {} with no search_product parameter", AppConstants.API_SEARCH_PRODUCT);
        return given().spec(spec()).when().post(AppConstants.API_SEARCH_PRODUCT).then().extract().response();
    }

    // ------------------------------------------------------------ deserialisers

    public List<Product> productsFrom(Response response) {
        return response.jsonPath().getList("products", Product.class);
    }

    public List<Brand> brandsFrom(Response response) {
        return response.jsonPath().getList("brands", Brand.class);
    }
}
