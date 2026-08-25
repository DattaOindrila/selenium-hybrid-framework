package com.qa.base;

import com.qa.api.client.ProductApiClient;
import com.qa.api.client.UserApiClient;
import com.qa.utils.ConfigReader;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;

/**
 * Parent of every API test class.
 *
 * Deliberately does NOT extend BaseTest: an API test has no reason to start a
 * browser, and starting one would make the API suite fifty times slower and
 * dependent on a browser install it does not need. The two hierarchies meet only
 * in the cross-validation tests, which extend BaseTest and instantiate the API
 * clients directly.
 */
public abstract class BaseApiTest {

    protected static final Logger log = LogManager.getLogger(BaseApiTest.class);

    protected UserApiClient userApi;
    protected ProductApiClient productApi;

    @BeforeClass(alwaysRun = true)
    public void setUpApi() {
        RestAssured.baseURI = ConfigReader.get("api.base.uri");
        userApi = new UserApiClient();
        productApi = new ProductApiClient();
        log.info("API base URI: {}", RestAssured.baseURI);
    }
}
