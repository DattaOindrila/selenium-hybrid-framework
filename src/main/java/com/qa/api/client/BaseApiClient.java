package com.qa.api.client;

import com.qa.utils.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * Shared Rest Assured setup for every API client.
 *
 * The request specification (base URI, content type, logging) is built once and
 * reused, so no individual test repeats connection details - the API equivalent of
 * the Page Object Model's "locators live in one place".
 *
 * ---------------------------------------------------------------------------
 * IMPORTANT BEHAVIOUR OF THIS PARTICULAR API
 * ---------------------------------------------------------------------------
 * automationexercise.com answers HTTP 200 to every request, including errors. The
 * real status is a "responseCode" field inside the JSON body:
 *
 *     POST /api/verifyLogin with bad credentials
 *       -> HTTP 200   {"responseCode": 404, "message": "User not found!"}
 *
 * So every test asserts on BOTH: the transport-level status (always 200, proving
 * the endpoint was reached) and the body-level responseCode (the actual outcome).
 * Asserting only on the HTTP status would make a test that can never fail.
 */
public abstract class BaseApiClient {

    protected static final Logger log = LogManager.getLogger(BaseApiClient.class);

    private static RequestSpecification requestSpec;

    protected static synchronized RequestSpecification spec() {
        if (requestSpec == null) {
            RequestSpecBuilder builder = new RequestSpecBuilder()
                    .setBaseUri(ConfigReader.get("api.base.uri"))
                    .setContentType(ContentType.URLENC)
                    .setAccept(ContentType.JSON);

            if (ConfigReader.getBoolean("api.log.requests")) {
                // Log into a buffer, not straight to stdout, so a parallel run does
                // not interleave half of one request with half of another.
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                PrintStream stream = new PrintStream(buffer, true);
                builder.addFilter(new RequestLoggingFilter(stream));
                builder.addFilter(new ResponseLoggingFilter(stream));
            }
            requestSpec = builder.build();
        }
        return requestSpec;
    }

    /**
     * Reads the body-level responseCode described above.
     *
     * @throws AssertionError if the field is missing, which means the endpoint
     *         returned something that is not the documented envelope at all.
     */
    public static int responseCodeOf(Response response) {
        Integer code = response.jsonPath().getInt("responseCode");
        if (code == null) {
            throw new AssertionError(
                    "Response had no 'responseCode' field. Body was:\n" + response.asPrettyString());
        }
        return code;
    }

    public static String messageOf(Response response) {
        return response.jsonPath().getString("message");
    }
}
