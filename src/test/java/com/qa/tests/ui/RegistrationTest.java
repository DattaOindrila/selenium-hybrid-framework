package com.qa.tests.ui;

import com.qa.api.model.UserAccount;
import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.dataproviders.TestDataProviders;
import com.qa.helpers.TestAccountManager;
import com.qa.pages.AccountCreatedPage;
import com.qa.pages.AccountDeletedPage;
import com.qa.pages.LoginPage;
import com.qa.pages.SignupPage;
import com.qa.utils.ExtentManager;
import com.qa.utils.TestDataFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Registration through the browser: the full eighteen-field account information
 * form, its validation, and the delete-account path that cleans up afterwards.
 *
 * Every account created here is deleted again before the class finishes. The
 * application under test is a shared public practice site, and a suite that leaves
 * a new account behind on every run is a suite that slowly pollutes it.
 */
public class RegistrationTest extends BaseTest {

    private final TestAccountManager accounts = new TestAccountManager();

    @AfterClass(alwaysRun = true)
    public void removeAccountsCreatedThroughTheApi() {
        accounts.cleanUp();
    }

    /**
     * The e-mail address is generated rather than read from the spreadsheet.
     *
     * The site rejects an address that is already registered, so a fixed address in
     * the data file would pass on the first run and fail on every run afterwards.
     * Every other field on the form comes from the spreadsheet - the address is the
     * one value that cannot be static.
     */
    @Test(dataProvider = "registrationData", dataProviderClass = TestDataProviders.class,
          groups = {"ui", "regression", "registration"},
          description = "A new user can register with the data from each spreadsheet row, and can then delete the account")
    public void userCanRegisterAndThenDeleteTheAccount(Map<String, String> row) {
        UserAccount user = accountFrom(row);

        ExtentManager.logStep("Registering " + row.get("testCaseId") + " - " + row.get("description"));
        LoginPage loginPage = openLoginPage();
        SignupPage signupPage = loginPage.signup(user.getName(), user.getEmail());

        Assert.assertTrue(signupPage.isAccountInformationFormVisible(),
                "The 'Enter Account Information' form did not appear after submitting the signup box");

        AccountCreatedPage accountCreated = signupPage.createAccount(user);
        Assert.assertTrue(accountCreated.isAccountCreatedMessageVisible(),
                "The 'Account Created!' confirmation was not shown for " + user.getEmail());
        Assert.assertEquals(accountCreated.getMessage().toUpperCase(), AppConstants.ACCOUNT_CREATED_MESSAGE,
                "Unexpected confirmation heading after creating the account");

        ExtentManager.logStep("Continuing to the home page as the newly registered user");
        accountCreated.clickContinue();
        Assert.assertTrue(homePage.header().isUserLoggedIn(),
                "The header did not show a logged-in user straight after registration");
        Assert.assertEquals(homePage.header().getLoggedInUsername(), user.getName(),
                "The header showed a different name from the one used to register");

        ExtentManager.logStep("Deleting the account again so the practice site is left clean");
        homePage.header().clickDeleteAccount();
        AccountDeletedPage accountDeleted = new AccountDeletedPage(driver);
        Assert.assertTrue(accountDeleted.isAccountDeletedMessageVisible(),
                "The 'Account Deleted!' confirmation was not shown for " + user.getEmail());
        Assert.assertEquals(accountDeleted.getMessage().toUpperCase(), AppConstants.ACCOUNT_DELETED_MESSAGE,
                "Unexpected confirmation heading after deleting the account");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "Signing up with an e-mail address that is already registered is rejected")
    public void signupWithAnAlreadyRegisteredEmailIsRejected() {
        // The address is made genuinely unavailable through the API first. Registering
        // it through the browser would work too, but it would make this test depend on
        // the whole eighteen-field form succeeding before it can test anything.
        UserAccount existing = accounts.createAccountViaApi();

        ExtentManager.logStep("Attempting to sign up again with " + existing.getEmail());
        LoginPage loginPage = openLoginPage();
        loginPage.submitSignup(TestDataFactory.randomName(), existing.getEmail());

        Assert.assertTrue(loginPage.isSignupErrorVisible(),
                "No error was shown when signing up with an address that is already registered");
        Assert.assertEquals(loginPage.getSignupErrorMessage(), AppConstants.SIGNUP_DUPLICATE_EMAIL_MESSAGE,
                "Unexpected message for a duplicate e-mail address");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "The account information form carries over the name and e-mail from the signup box")
    public void accountInformationFormIsPrefilledFromTheSignupBox() {
        String name = TestDataFactory.randomName();
        String email = TestDataFactory.uniqueEmail();

        SignupPage signupPage = openLoginPage().signup(name, email);

        Assert.assertEquals(signupPage.getPrefilledName(), name,
                "The name typed into the signup box was not carried over to the account form");
        Assert.assertEquals(signupPage.getPrefilledEmail(), email,
                "The e-mail typed into the signup box was not carried over to the account form");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "The account form offers exactly the seven countries the site supports")
    public void countryDropdownOffersTheSupportedCountries() {
        SignupPage signupPage = openLoginPage()
                .signup(TestDataFactory.randomName(), TestDataFactory.uniqueEmail());

        // Seven is not a magic number: it is the count the application currently
        // publishes. If the site adds a country this test should fail and be updated -
        // that is the point of asserting on it.
        Assert.assertEquals(signupPage.getCountryOptionCount(), 7,
                "The country dropdown no longer offers seven options");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "The newsletter and special-offers checkboxes can both be selected")
    public void newsletterAndOptinCheckboxesCanBeSelected() {
        UserAccount user = TestDataFactory.newUser();
        SignupPage signupPage = openLoginPage().signup(user.getName(), user.getEmail());

        // fillAccountInformation ticks both boxes as part of completing the form.
        signupPage.fillAccountInformation(user);

        Assert.assertTrue(signupPage.isNewsletterChecked(),
                "The 'Sign up for our newsletter!' checkbox was not selected");
        Assert.assertTrue(signupPage.isOptinChecked(),
                "The 'Receive special offers' checkbox was not selected");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "The login page shows both the login form and the new-user signup form")
    public void loginPageShowsBothForms() {
        LoginPage loginPage = openLoginPage();

        Assert.assertTrue(loginPage.isLoginFormVisible(), "The 'Login to your account' form was not shown");
        Assert.assertTrue(loginPage.isSignupFormVisible(), "The 'New User Signup!' form was not shown");
        Assert.assertEquals(loginPage.getSignupHeading(), "New User Signup!",
                "Unexpected heading above the signup form");
        Assert.assertEquals(loginPage.getLoginHeading(), "Login to your account",
                "Unexpected heading above the login form");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "An account registered through the browser can immediately log in again")
    public void accountRegisteredThroughTheBrowserCanLogInAgain() {
        UserAccount user = TestDataFactory.newUser();

        ExtentManager.logStep("Registering " + user.getEmail() + " through the form");
        openLoginPage().signup(user.getName(), user.getEmail()).createAccount(user).clickContinue();
        Assert.assertTrue(homePage.header().isUserLoggedIn(), "Registration did not leave the user logged in");

        ExtentManager.logStep("Logging out, then logging back in with the same credentials");
        homePage.header().clickLogout();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(user.getEmail(), user.getPassword());

        Assert.assertTrue(homePage.header().isUserLoggedIn(),
                "The account created through the browser could not log in again");
        Assert.assertEquals(homePage.header().getLoggedInUsername(), user.getName(),
                "Logged in as a different user than the one just registered");

        // Registered through the UI, so clean up through the UI as well.
        homePage.header().clickDeleteAccount();
        Assert.assertTrue(new AccountDeletedPage(driver).isAccountDeletedMessageVisible(),
                "The account created by this test could not be deleted");
    }

    @Test(groups = {"ui", "regression", "registration"},
          description = "A deleted account can no longer log in")
    public void deletedAccountCanNoLongerLogIn() {
        UserAccount user = TestDataFactory.newUser();

        openLoginPage().signup(user.getName(), user.getEmail()).createAccount(user).clickContinue();
        homePage.header().clickDeleteAccount();
        new AccountDeletedPage(driver).clickContinue();

        ExtentManager.logStep("Attempting to log in with the credentials of the deleted account");
        LoginPage loginPage = openLoginPage();
        loginPage.login(user.getEmail(), user.getPassword());

        Assert.assertTrue(loginPage.isLoginErrorVisible(),
                "A deleted account was still able to reach a logged-in state");
        Assert.assertEquals(loginPage.getLoginErrorMessage(), AppConstants.LOGIN_ERROR_MESSAGE,
                "Unexpected error message when logging in with deleted credentials");
    }

    // ------------------------------------------------------------------ helpers

    private LoginPage openLoginPage() {
        homePage.header().goToSignupLogin();
        return new LoginPage(driver);
    }

    /** Builds the account from a spreadsheet row, supplying only the e-mail address. */
    private UserAccount accountFrom(Map<String, String> row) {
        return UserAccount.builder()
                .name(row.get("name"))
                .email(TestDataFactory.uniqueEmail())
                .password(row.get("password"))
                .title(row.get("title"))
                .birthDay(row.get("birthDay"))
                .birthMonth(row.get("birthMonth"))
                .birthYear(row.get("birthYear"))
                .firstName(row.get("firstName"))
                .lastName(row.get("lastName"))
                .company(row.get("company"))
                .address1(row.get("address1"))
                .address2(row.get("address2"))
                .country(row.get("country"))
                .state(row.get("state"))
                .city(row.get("city"))
                .zipcode(row.get("zipcode"))
                .mobileNumber(row.get("mobileNumber"))
                .build();
    }
}
