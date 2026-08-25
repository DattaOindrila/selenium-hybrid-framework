package com.qa.tests.ui;

import com.qa.api.model.UserAccount;
import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.dataproviders.TestDataProviders;
import com.qa.helpers.TestAccountManager;
import com.qa.pages.CartPage;
import com.qa.pages.LoginPage;
import com.qa.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Login, logout and the negative paths around them.
 *
 * The account used by the positive tests is created once through the API in
 * @BeforeClass. Driving the registration form through the browser before every
 * login test would triple the runtime of this class and would make a login test
 * fail whenever registration broke - which is the registration test's job to catch,
 * not this one's.
 */
public class LoginLogoutTest extends BaseTest {

    private final TestAccountManager accounts = new TestAccountManager();
    private UserAccount registeredUser;

    @BeforeClass(alwaysRun = true)
    public void createTheAccountUsedByTheseTests() {
        registeredUser = accounts.createAccountViaApi();
    }

    @AfterClass(alwaysRun = true)
    public void removeTheAccount() {
        accounts.cleanUp();
    }

    @Test(groups = {"ui", "regression", "login", "smoke"},
          description = "A registered user can log in and the header greets them by name")
    public void registeredUserCanLogIn() {
        ExtentManager.logStep("Logging in as " + registeredUser.getEmail());
        LoginPage loginPage = openLoginPage();
        loginPage.login(registeredUser.getEmail(), registeredUser.getPassword());

        Assert.assertTrue(homePage.header().isUserLoggedIn(),
                "The header did not show a logged-in user after submitting valid credentials");
        Assert.assertEquals(homePage.header().getLoggedInUsername(), registeredUser.getName(),
                "The header greeted a different user than the one who logged in");
        Assert.assertTrue(homePage.header().isLogoutVisible(),
                "The 'Logout' link should be available once logged in");
        Assert.assertFalse(homePage.header().isSignupLoginVisible(),
                "The 'Signup / Login' link should disappear once logged in");
    }

    @Test(dataProvider = "loginData", dataProviderClass = TestDataProviders.class,
          groups = {"ui", "regression", "login"},
          description = "Each invalid credential combination is rejected with the expected message")
    public void invalidCredentialsAreRejected(Map<String, String> row) {
        ExtentManager.logStep(row.get("testCaseId") + " - " + row.get("description"));

        LoginPage loginPage = openLoginPage();
        loginPage.login(row.get("email"), row.get("password"));

        Assert.assertTrue(loginPage.isLoginErrorVisible(),
                "No error was shown for " + row.get("testCaseId") + " (" + row.get("description") + ")");
        Assert.assertEquals(loginPage.getLoginErrorMessage(), row.get("expectedError"),
                "Unexpected error message for " + row.get("testCaseId"));

        // The negative half that is easy to forget: proving the login did NOT happen.
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "A user was logged in despite invalid credentials for " + row.get("testCaseId"));
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "Submitting the login form with both fields empty is blocked by the browser")
    public void emptyCredentialsAreBlockedByFieldValidation() {
        LoginPage loginPage = openLoginPage();

        ExtentManager.logStep("Clicking Login without typing anything");
        loginPage.clickLoginButton();

        // The e-mail field is type="email" and required, so the browser refuses to
        // submit and shows its own validation bubble. There is no server round-trip
        // to assert on, which is why this asserts on the field's validity state.
        Assert.assertFalse(loginPage.isLoginEmailFieldValid(),
                "An empty e-mail field should fail HTML5 validation");
        Assert.assertFalse(loginPage.getLoginEmailValidationMessage().isBlank(),
                "The browser should supply a validation message for the empty e-mail field");
        Assert.assertTrue(loginPage.isLoginFormVisible(),
                "The browser should have stayed on the login page");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "A malformed e-mail address is blocked by field validation before it is submitted")
    public void malformedEmailIsBlockedByFieldValidation() {
        LoginPage loginPage = openLoginPage();
        loginPage.login("not-an-email-address", "Whatever@123");

        Assert.assertFalse(loginPage.isLoginEmailFieldValid(),
                "A value with no @ sign should fail the e-mail field's own validation");
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "A malformed e-mail must never produce a logged-in session");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "Logging out returns the visitor to the login page as an anonymous user")
    public void logoutReturnsTheUserToTheLoginPage() {
        openLoginPage().login(registeredUser.getEmail(), registeredUser.getPassword());
        Assert.assertTrue(homePage.header().isUserLoggedIn(), "Precondition failed: the user was not logged in");

        ExtentManager.logStep("Logging out");
        homePage.header().clickLogout();

        LoginPage loginPage = new LoginPage(driver);
        Assert.assertTrue(loginPage.isLoginFormVisible(),
                "Logging out should land on the login page");
        Assert.assertTrue(loginPage.getCurrentUrl().contains(AppConstants.LOGIN_PATH),
                "Expected to be on " + AppConstants.LOGIN_PATH + " but was at " + loginPage.getCurrentUrl());
        Assert.assertTrue(homePage.header().isSignupLoginVisible(),
                "The 'Signup / Login' link should return after logging out");
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "The header still showed a logged-in user after logging out");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "After logging out the session is gone, not merely hidden")
    public void sessionIsClearedAfterLogout() {
        openLoginPage().login(registeredUser.getEmail(), registeredUser.getPassword());
        homePage.header().clickLogout();

        // Navigating somewhere else and back proves the header is reading a cleared
        // session rather than simply having been re-rendered by the logout page.
        ExtentManager.logStep("Navigating to the cart and back to confirm the session is really gone");
        CartPage cartPage = new CartPage(driver).open();
        Assert.assertTrue(cartPage.isLoaded(), "The cart page should still be reachable when anonymous");
        Assert.assertFalse(cartPage.header().isUserLoggedIn(),
                "A logged-in session survived a logout followed by navigation");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "The correct e-mail with the wrong password is rejected")
    public void correctEmailWithWrongPasswordIsRejected() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(registeredUser.getEmail(), registeredUser.getPassword() + "X");

        Assert.assertTrue(loginPage.isLoginErrorVisible(),
                "A wrong password was not rejected for a valid e-mail address");
        Assert.assertEquals(loginPage.getLoginErrorMessage(), AppConstants.LOGIN_ERROR_MESSAGE,
                "Unexpected error message for a wrong password");
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "A session was created despite the wrong password");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "A password that differs only by letter case is rejected")
    public void passwordIsCaseSensitive() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(registeredUser.getEmail(), registeredUser.getPassword().toUpperCase());

        Assert.assertTrue(loginPage.isLoginErrorVisible(),
                "An upper-cased password was accepted, which means passwords are not case-sensitive");
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "A session was created with a case-altered password");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "Leading and trailing spaces around the password are not stripped")
    public void passwordWithSurroundingSpacesIsRejected() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(registeredUser.getEmail(), "  " + registeredUser.getPassword() + "  ");

        Assert.assertTrue(loginPage.isLoginErrorVisible(),
                "A password padded with spaces was accepted, so the field is silently trimming input");
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "A session was created with a space-padded password");
    }

    @Test(groups = {"ui", "regression", "login"},
          description = "A logged-in user sees the Delete Account link, an anonymous visitor does not")
    public void deleteAccountLinkIsOnlyVisibleWhenLoggedIn() {
        Assert.assertFalse(homePage.header().isDeleteAccountVisible(),
                "'Delete Account' should not be offered to an anonymous visitor");

        openLoginPage().login(registeredUser.getEmail(), registeredUser.getPassword());

        Assert.assertTrue(homePage.header().isDeleteAccountVisible(),
                "'Delete Account' should be offered once logged in");
    }

    // ------------------------------------------------------------------ helpers

    private LoginPage openLoginPage() {
        homePage.header().goToSignupLogin();
        return new LoginPage(driver);
    }
}
