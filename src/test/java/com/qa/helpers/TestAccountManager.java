package com.qa.helpers;

import com.qa.api.client.BaseApiClient;
import com.qa.api.client.UserApiClient;
import com.qa.api.model.UserAccount;
import com.qa.constants.AppConstants;
import com.qa.utils.TestDataFactory;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Creates and disposes of throwaway accounts for tests that need to be logged in.
 *
 * WHY THIS EXISTS
 * ---------------
 * A cart test is about the cart. Driving eighteen registration form fields through
 * the browser first makes it slower, and makes it fail for reasons that belong to
 * the registration test. Creating the account through POST /api/createAccount takes
 * well under a second and leaves the browser free to do the thing under test.
 *
 * Using the API as a setup shortcut for UI tests - rather than as the thing being
 * tested - is the point of a hybrid framework.
 *
 * Every account created here is deleted in teardown, so the shared practice site is
 * not left with a growing pile of abandoned records.
 */
public final class TestAccountManager {

    private static final Logger log = LogManager.getLogger(TestAccountManager.class);
    private final UserApiClient userApi = new UserApiClient();
    private final List<UserAccount> created = Collections.synchronizedList(new ArrayList<>());

    /** Registers a fresh random account through the API and returns it. */
    public UserAccount createAccountViaApi() {
        return createAccountViaApi(TestDataFactory.newUser());
    }

    public UserAccount createAccountViaApi(UserAccount user) {
        Response response = userApi.createAccount(user);
        int code = BaseApiClient.responseCodeOf(response);
        if (code != AppConstants.API_CREATED) {
            throw new IllegalStateException(
                    "Test setup failed: could not create the account through the API. "
                    + "responseCode=" + code + " body=" + response.asString());
        }
        created.add(user);
        log.info("Test account created via API: {}", user.getEmail());
        return user;
    }

    /**
     * Deletes every account this instance created.
     * Never throws: a cleanup problem must not turn a passing test red.
     */
    public void cleanUp() {
        synchronized (created) {
            for (UserAccount user : created) {
                userApi.deleteAccountQuietly(user.getEmail(), user.getPassword());
            }
            created.clear();
        }
    }

    public UserApiClient api() {
        return userApi;
    }
}
