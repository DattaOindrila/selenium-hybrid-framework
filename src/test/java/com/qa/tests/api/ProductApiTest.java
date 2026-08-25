package com.qa.tests.api;

import com.qa.api.model.Brand;
import com.qa.api.model.Product;
import com.qa.base.BaseApiTest;
import com.qa.constants.AppConstants;
import com.qa.dataproviders.TestDataProviders;
import com.qa.utils.ExtentManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.qa.api.client.BaseApiClient.messageOf;
import static com.qa.api.client.BaseApiClient.responseCodeOf;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Catalogue endpoints - APIs 1 to 6 of the site's published list.
 *
 * READ THIS BEFORE THE ASSERTIONS MAKE SENSE
 * ------------------------------------------
 * This API answers HTTP 200 to everything, including errors. The real status is a
 * "responseCode" field inside the JSON body. So every test asserts twice:
 *   - getStatusCode() == 200      proves the endpoint was reached at all
 *   - responseCodeOf(...) == N    is the API's actual verdict
 * A test that only checked the HTTP status could never fail, which is worse than no
 * test at all.
 */
public class ProductApiTest extends BaseApiTest {

    @Test(groups = {"api", "regression", "smoke"},
          description = "GET /api/productsList returns the whole product catalogue")
    public void getAllProductsReturnsTheCatalogue() {
        Response response = productApi.getAllProducts();

        Assert.assertEquals(response.getStatusCode(), 200,
                "Transport-level HTTP status (this endpoint always returns 200)");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK,
                "Body-level responseCode, which is the API's real verdict");

        List<Product> products = productApi.productsFrom(response);
        Assert.assertFalse(products.isEmpty(), "The product catalogue came back empty");
        ExtentManager.logStep("Catalogue contains " + products.size() + " products");
    }

    @Test(groups = {"api", "regression"},
          description = "The products payload matches its published JSON schema")
    public void productsPayloadMatchesItsSchema() {
        // Schema validation catches a shape change - a renamed field, a price that
        // becomes a number instead of a string - that a handful of value assertions
        // would sail straight past.
        productApi.getAllProducts()
                .then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/products-list-schema.json"));
    }

    @Test(groups = {"api", "regression"},
          description = "POST to /api/productsList is rejected as an unsupported method")
    public void postToProductsListIsRejected() {
        Response response = productApi.postToProductsList();

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_METHOD_NOT_ALLOWED,
                "POST should be rejected with a body-level 405");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_METHOD_NOT_SUPPORTED,
                "Unexpected message for an unsupported method");
    }

    @Test(groups = {"api", "regression"},
          description = "GET /api/brandsList returns the brand list and matches its schema")
    public void getAllBrandsReturnsTheBrandList() {
        Response response = productApi.getAllBrands();

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK, "Body-level responseCode");

        List<Brand> brands = productApi.brandsFrom(response);
        Assert.assertFalse(brands.isEmpty(), "The brand list came back empty");
        Assert.assertTrue(brands.stream().anyMatch(b -> "Polo".equals(b.getBrand())),
                "Expected 'Polo' among the brands but got " + brands);

        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/brands-list-schema.json"));
    }

    @Test(groups = {"api", "regression"},
          description = "PUT to /api/brandsList is rejected as an unsupported method")
    public void putToBrandsListIsRejected() {
        Response response = productApi.putToBrandsList();

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_METHOD_NOT_ALLOWED,
                "PUT should be rejected with a body-level 405");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_METHOD_NOT_SUPPORTED,
                "Unexpected message for an unsupported method");
    }

    /**
     * The strict per-result check that the UI tests cannot make.
     *
     * Search matches a product's CATEGORY as well as its name: searching "top"
     * legitimately returns "Little Girls Mr. Panda Shirt", whose category is
     * "Tops & Shirts". The API response includes the category, so this is the layer
     * where every single result can be justified.
     */
    @Test(groups = {"api", "regression"},
          description = "Every search result matches the term in either its name or its category")
    public void everySearchResultIsJustifiedByNameOrCategory() {
        String term = "top";
        Response response = productApi.searchProduct(term);

        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK, "Body-level responseCode");

        List<Product> results = productApi.productsFrom(response);
        Assert.assertFalse(results.isEmpty(), "Searching for '" + term + "' returned nothing");

        for (Product product : results) {
            boolean nameMatches = product.getName().toLowerCase().contains(term);
            boolean categoryMatches = product.getCategory().getCategory().toLowerCase().contains(term);
            Assert.assertTrue(nameMatches || categoryMatches,
                    "Product '" + product.getName() + "' (category '"
                    + product.getCategory().getCategory() + "') matched neither the name nor the "
                    + "category for the search term '" + term + "'");
        }
        ExtentManager.logStep(results.size() + " results, every one justified by name or category");
    }

    @Test(dataProvider = "positiveSearchData", dataProviderClass = TestDataProviders.class,
          groups = {"api", "regression"},
          description = "Each search term from the spreadsheet returns results through the API")
    public void searchTermsFromTheSpreadsheetReturnResults(Map<String, String> row) {
        String term = row.get("searchTerm");
        Response response = productApi.searchProduct(term);

        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK,
                "Body-level responseCode for '" + term + "'");
        Assert.assertFalse(productApi.productsFrom(response).isEmpty(),
                "Search for '" + term + "' returned nothing, but " + row.get("description"));
    }

    @Test(groups = {"api", "regression"},
          description = "Searching without the required parameter is rejected with a clear message")
    public void searchWithoutTheRequiredParameterIsRejected() {
        Response response = productApi.searchProductWithoutParameter();

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_BAD_REQUEST,
                "A missing parameter should produce a body-level 400");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_MISSING_SEARCH_PARAM,
                "Unexpected message for a missing search_product parameter");
    }

    @Test(groups = {"api", "regression"},
          description = "A term that matches nothing returns an empty or absent product list")
    public void searchForSomethingThatDoesNotExistReturnsNoProducts() {
        Response response = productApi.searchProduct("zzzznotaproduct");

        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK, "Body-level responseCode");

        List<Product> results = productApi.productsFrom(response);
        // The API omits the products key entirely rather than returning an empty
        // array, so a null list is the documented "no results" answer here.
        Assert.assertTrue(results == null || results.isEmpty(),
                "A nonsense search term returned products: " + results);
    }

    @Test(groups = {"api", "regression"},
          description = "Every product in the catalogue has a well-formed price and a brand")
    public void everyProductHasAWellFormedPriceAndBrand() {
        List<Product> products = productApi.productsFrom(productApi.getAllProducts());

        for (Product product : products) {
            Assert.assertTrue(product.getPrice().matches("Rs\\. ?\\d+"),
                    "Product " + product.getId() + " '" + product.getName()
                    + "' had a price outside the documented 'Rs. N' format: '" + product.getPrice() + "'");
            Assert.assertFalse(product.getBrand() == null || product.getBrand().isBlank(),
                    "Product " + product.getId() + " '" + product.getName() + "' had no brand");
            Assert.assertTrue(product.getId() > 0,
                    "Product '" + product.getName() + "' had a non-positive id");
        }
        ExtentManager.logStep("Validated " + products.size() + " products");
    }
}
