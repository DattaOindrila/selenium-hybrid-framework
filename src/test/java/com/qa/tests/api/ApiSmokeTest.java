package com.qa.tests.api;

import com.qa.base.BaseApiTest;
import com.qa.constants.AppConstants;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.qa.api.client.BaseApiClient.responseCodeOf;

/** Proves the API layer is wired up and the endpoint answers. */
public class ApiSmokeTest extends BaseApiTest {

    @Test(groups = {"smoke", "api"}, description = "GET /api/productsList returns the product catalogue")
    public void productsListReturnsProducts() {
        Response response = productApi.getAllProducts();

        // Both assertions matter: the HTTP status proves the endpoint was reached,
        // the body-level responseCode is the API's actual verdict.
        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK, "Body-level responseCode");
        Assert.assertFalse(productApi.productsFrom(response).isEmpty(), "Product list was empty");
    }
}
