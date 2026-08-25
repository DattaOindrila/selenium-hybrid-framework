package com.qa.tests.api;

import com.qa.api.model.UserAccount;
import com.qa.base.BaseApiTest;
import com.qa.constants.AppConstants;
import com.qa.utils.ExtentManager;
import com.qa.utils.TestDataFactory;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.qa.api.client.BaseApiClient.messageOf;
import static com.qa.api.client.BaseApiClient.responseCodeOf;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Account endpoints - APIs 7 to 14 of the site's published list.
 *
 * Every account these tests create is deleted again, either inside the test or in
 * the @AfterClass sweep, so the shared practice site is left as it was found.
 *
 * As in ProductApiTest: HTTP is always 200 here, and the meaningful status is the
 * "responseCode" field inside the body.
 */
public class UserApiTest extends BaseApiTest {

    /** Anything registered by these tests, so nothing is left behind if a test fails mid-way. */
    private final List<UserAccount> createdAccounts = Collections.synchronizedList(new ArrayList<>());

    @AfterClass(alwaysRun = true)
    public void deleteAnythingLeftBehind() {
        for (UserAccount user : createdAccounts) {
            userApi.deleteAccountQuietly(user.getEmail(), user.getPassword());
        }
        createdAccounts.clear();
    }

    @Test(groups = {"api", "regression"},
          description = "POST /api/createAccount registers a new user")
    public void createAccountRegistersANewUser() {
        UserAccount user = register();

        Response verify = userApi.verifyLogin(user.getEmail(), user.getPassword());
        Assert.assertEquals(responseCodeOf(verify), AppConstants.API_OK,
                "The account was reported as created but could not then be verified");
    }

    @Test(groups = {"api", "regression"},
          description = "POST /api/verifyLogin accepts correct credentials")
    public void verifyLoginAcceptsCorrectCredentials() {
        UserAccount user = register();

        Response response = userApi.verifyLogin(user.getEmail(), user.getPassword());

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK, "Body-level responseCode");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_USER_EXISTS,
                "Unexpected message for a valid login");
    }

    @Test(groups = {"api", "regression"},
          description = "POST /api/verifyLogin rejects credentials that belong to nobody")
    public void verifyLoginRejectsUnknownCredentials() {
        Response response = userApi.verifyLogin(TestDataFactory.uniqueEmail(), "NotARealPassword@1");

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_NOT_FOUND,
                "An unknown user should produce a body-level 404");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_USER_NOT_FOUND,
                "Unexpected message for an unknown user");
    }

    @Test(groups = {"api", "regression"},
          description = "POST /api/verifyLogin without the email parameter is a bad request")
    public void verifyLoginWithoutEmailIsABadRequest() {
        Response response = userApi.verifyLoginWithoutEmail("AnyPassword@1");

        Assert.assertEquals(responseCodeOf(response), AppConstants.API_BAD_REQUEST,
                "A missing email parameter should produce a body-level 400");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_MISSING_LOGIN_PARAM,
                "Unexpected message for a missing login parameter");
    }

    @Test(groups = {"api", "regression"},
          description = "DELETE is not supported on /api/verifyLogin")
    public void deleteOnVerifyLoginIsRejected() {
        Response response = userApi.deleteVerifyLogin();

        Assert.assertEquals(responseCodeOf(response), AppConstants.API_METHOD_NOT_ALLOWED,
                "DELETE should be rejected with a body-level 405");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_METHOD_NOT_SUPPORTED,
                "Unexpected message for an unsupported method");
    }

    /**
     * The most valuable test in this class: it proves the API stored what it was
     * given, field by field. Asserting only on the "User created!" message would
     * pass even if the server had silently dropped every address field.
     */
    @Test(groups = {"api", "regression"},
          description = "The stored user details match, field by field, what was sent on registration")
    public void storedUserDetailsMatchWhatWasRegistered() {
        UserAccount sent = register();

        Response response = userApi.getUserDetailByEmail(sent.getEmail());
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK, "Body-level responseCode");

        UserAccount stored = userApi.userFrom(response);

        Assert.assertEquals(stored.getEmail(), sent.getEmail(), "Stored e-mail did not match");
        Assert.assertEquals(stored.getName(), sent.getName(), "Stored name did not match");
        Assert.assertEquals(stored.getFirstName(), sent.getFirstName(), "Stored first name did not match");
        Assert.assertEquals(stored.getLastName(), sent.getLastName(), "Stored last name did not match");
        Assert.assertEquals(stored.getCompany(), sent.getCompany(), "Stored company did not match");
        Assert.assertEquals(stored.getAddress1(), sent.getAddress1(), "Stored address did not match");
        Assert.assertEquals(stored.getCountry(), sent.getCountry(), "Stored country did not match");
        Assert.assertEquals(stored.getState(), sent.getState(), "Stored state did not match");
        Assert.assertEquals(stored.getCity(), sent.getCity(), "Stored city did not match");
        Assert.assertEquals(stored.getZipcode(), sent.getZipcode(), "Stored postcode did not match");
        Assert.assertEquals(stored.getTitle(), sent.getTitle(), "Stored title did not match");
    }

    @Test(groups = {"api", "regression"},
          description = "The user detail payload matches its published JSON schema")
    public void userDetailPayloadMatchesItsSchema() {
        UserAccount user = register();

        userApi.getUserDetailByEmail(user.getEmail())
                .then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/user-detail-schema.json"));
    }

    /**
     * Note this asserts on the DATA after the update, not just on the "User updated!"
     * message. A success message proves the request was accepted; only a read-back
     * proves anything was actually changed.
     */
    @Test(groups = {"api", "regression"},
          description = "PUT /api/updateAccount changes the stored details, not just the response message")
    public void updateAccountActuallyPersistsTheChange() {
        UserAccount user = register();

        user.setFirstName("Updated");
        user.setLastName("Surname");
        user.setCity("Howrah");
        user.setZipcode("711101");

        Response update = userApi.updateAccount(user);
        Assert.assertEquals(responseCodeOf(update), AppConstants.API_OK, "Body-level responseCode on update");
        Assert.assertEquals(messageOf(update), AppConstants.API_MSG_USER_UPDATED,
                "Unexpected message after updating the account");

        ExtentManager.logStep("Reading the account back to prove the update persisted");
        UserAccount stored = userApi.userFrom(userApi.getUserDetailByEmail(user.getEmail()));

        Assert.assertEquals(stored.getFirstName(), "Updated", "The updated first name was not stored");
        Assert.assertEquals(stored.getLastName(), "Surname", "The updated last name was not stored");
        Assert.assertEquals(stored.getCity(), "Howrah", "The updated city was not stored");
        Assert.assertEquals(stored.getZipcode(), "711101", "The updated postcode was not stored");
    }

    @Test(groups = {"api", "regression"},
          description = "DELETE /api/deleteAccount removes the account and it can no longer log in")
    public void deleteAccountRemovesTheUser() {
        UserAccount user = register();

        Response delete = userApi.deleteAccount(user.getEmail(), user.getPassword());
        Assert.assertEquals(delete.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(delete), AppConstants.API_OK, "Body-level responseCode on delete");
        Assert.assertEquals(messageOf(delete), AppConstants.API_MSG_ACCOUNT_DELETED,
                "Unexpected message after deleting the account");

        // Deleted really means gone, not merely flagged.
        Response verify = userApi.verifyLogin(user.getEmail(), user.getPassword());
        Assert.assertEquals(responseCodeOf(verify), AppConstants.API_NOT_FOUND,
                "A deleted account could still be verified, so it was not really removed");

        createdAccounts.remove(user);
    }

    @Test(groups = {"api", "regression"},
          description = "Asking for the details of an address that was never registered does not succeed")
    public void userDetailForAnUnknownAddressDoesNotSucceed() {
        Response response = userApi.getUserDetailByEmail(TestDataFactory.uniqueEmail());

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        // Documented from the live API: it does not answer 200 for an unknown address.
        // The assertion is deliberately "not 200" rather than a specific code, because
        // the value the site returns here is not part of its published contract.
        Assert.assertNotEquals(responseCodeOf(response), AppConstants.API_OK,
                "The API reported success for an e-mail address that was never registered");
    }

    @Test(groups = {"api", "regression"},
          description = "Registering the same e-mail address twice is refused")
    public void registeringTheSameAddressTwiceIsRefused() {
        UserAccount user = register();

        // Same address, everything else regenerated.
        UserAccount duplicate = TestDataFactory.newUser();
        duplicate.setEmail(user.getEmail());

        Response response = userApi.createAccount(duplicate);

        Assert.assertNotEquals(responseCodeOf(response), AppConstants.API_CREATED,
                "The API created a second account against an e-mail address that was already taken");
    }

    // ------------------------------------------------------------------ helpers

    /** Registers a fresh account and records it for cleanup. */
    private UserAccount register() {
        UserAccount user = TestDataFactory.newUser();
        Response response = userApi.createAccount(user);

        Assert.assertEquals(response.getStatusCode(), 200, "Transport-level HTTP status");
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_CREATED,
                "Registration should produce a body-level 201");
        Assert.assertEquals(messageOf(response), AppConstants.API_MSG_USER_CREATED,
                "Unexpected message after registering");

        createdAccounts.add(user);
        return user;
    }
}
