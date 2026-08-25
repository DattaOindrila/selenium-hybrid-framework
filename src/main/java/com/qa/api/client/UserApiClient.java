package com.qa.api.client;

import com.qa.api.model.UserAccount;
import com.qa.constants.AppConstants;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/** Account endpoints: create, verify, read, update, delete. */
public final class UserApiClient extends BaseApiClient {

    public Response createAccount(UserAccount user) {
        log.info("POST {} for {}", AppConstants.API_CREATE_ACCOUNT, user.getEmail());
        return given().spec(spec())
                .formParams(user.toFormParams())
                .when().post(AppConstants.API_CREATE_ACCOUNT)
                .then().extract().response();
    }

    public Response verifyLogin(String email, String password) {
        log.info("POST {} for {}", AppConstants.API_VERIFY_LOGIN, email);
        return given().spec(spec())
                .formParam("email", email)
                .formParam("password", password)
                .when().post(AppConstants.API_VERIFY_LOGIN)
                .then().extract().response();
    }

    /** Omits the email parameter - the API answers responseCode 400. */
    public Response verifyLoginWithoutEmail(String password) {
        log.info("POST {} with no email parameter", AppConstants.API_VERIFY_LOGIN);
        return given().spec(spec())
                .formParam("password", password)
                .when().post(AppConstants.API_VERIFY_LOGIN)
                .then().extract().response();
    }

    /** DELETE is not supported on this endpoint - the API answers responseCode 405. */
    public Response deleteVerifyLogin() {
        log.info("DELETE {} (expected to be rejected)", AppConstants.API_VERIFY_LOGIN);
        return given().spec(spec()).when().delete(AppConstants.API_VERIFY_LOGIN).then().extract().response();
    }

    public Response getUserDetailByEmail(String email) {
        log.info("GET {}?email={}", AppConstants.API_USER_DETAIL_BY_EMAIL, email);
        return given().spec(spec())
                .queryParam("email", email)
                .when().get(AppConstants.API_USER_DETAIL_BY_EMAIL)
                .then().extract().response();
    }

    public Response updateAccount(UserAccount user) {
        log.info("PUT {} for {}", AppConstants.API_UPDATE_ACCOUNT, user.getEmail());
        return given().spec(spec())
                .formParams(user.toFormParams())
                .when().put(AppConstants.API_UPDATE_ACCOUNT)
                .then().extract().response();
    }

    public Response deleteAccount(String email, String password) {
        log.info("DELETE {} for {}", AppConstants.API_DELETE_ACCOUNT, email);
        return given().spec(spec())
                .formParam("email", email)
                .formParam("password", password)
                .when().delete(AppConstants.API_DELETE_ACCOUNT)
                .then().extract().response();
    }

    /** Maps the "user" object of GET /api/getUserDetailByEmail onto the shared model. */
    public UserAccount userFrom(Response response) {
        return response.jsonPath().getObject("user", UserAccount.class);
    }

    /**
     * Teardown helper. Never fails the test that calls it: if cleanup cannot happen
     * the test result should still reflect what the test was actually checking.
     */
    public void deleteAccountQuietly(String email, String password) {
        try {
            Response response = deleteAccount(email, password);
            log.info("Cleanup delete for {} -> responseCode {}", email, responseCodeOf(response));
        } catch (Exception e) {
            log.warn("Cleanup delete for {} failed: {}", email, e.toString());
        }
    }
}
