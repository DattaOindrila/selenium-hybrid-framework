package com.qa.tests.ui;

import com.qa.api.client.ProductApiClient;
import com.qa.api.client.UserApiClient;
import com.qa.api.model.Product;
import com.qa.api.model.UserAccount;
import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.helpers.TestAccountManager;
import com.qa.pages.AccountDeletedPage;
import com.qa.pages.LoginPage;
import com.qa.pages.ProductDetailsPage;
import com.qa.pages.ProductsPage;
import com.qa.pages.SignupPage;
import com.qa.utils.ExtentManager;
import com.qa.utils.TestDataFactory;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qa.api.client.BaseApiClient.responseCodeOf;

/**
 * ============================================================================
 *  CROSS-LAYER VALIDATION - the point of a hybrid framework
 * ============================================================================
 *
 * Every other class in this repository tests one layer in isolation. A UI test can
 * only prove that the browser showed something plausible; an API test can only
 * prove that an endpoint answered correctly. Neither can prove the two layers agree.
 *
 * These tests write through one layer and read through the other. If the API stores
 * a user the UI cannot log in as, or the catalogue the API publishes differs from
 * the one the shop renders, exactly one kind of test catches it: this kind.
 *
 * That is also why this class extends BaseTest rather than BaseApiTest - it needs a
 * real browser AND the Rest Assured clients at the same time.
 */
public class ApiUiCrossValidationTest extends BaseTest {

    private final TestAccountManager accounts = new TestAccountManager();
    private final UserApiClient userApi = new UserApiClient();
    private final ProductApiClient productApi = new ProductApiClient();

    @AfterClass(alwaysRun = true)
    public void cleanUpAccounts() {
        accounts.cleanUp();
    }

    /**
     * DIRECTION: API writes -> UI reads.
     *
     * Proves that an account created purely over HTTP is a real, usable account in
     * the shop - not just a row the API is willing to acknowledge. An API test alone
     * would stop at "responseCode 201"; that says nothing about whether a human
     * could ever log in.
     */
    @Test(groups = {"ui", "api", "regression", "crossvalidation"},
          description = "An account created through the API can log in through the browser")
    public void accountCreatedViaApiCanLogInThroughTheUi() {
        UserAccount user = accounts.createAccountViaApi();
        ExtentManager.logStep("Created " + user.getEmail() + " over the API; now logging in with a browser");

        homePage.header().goToSignupLogin();
        new LoginPage(driver).login(user.getEmail(), user.getPassword());

        Assert.assertTrue(homePage.header().isUserLoggedIn(),
                "An account the API reported as created could not log in through the UI");
        Assert.assertEquals(homePage.header().getLoggedInUsername(), user.getName(),
                "The UI greeted a different name from the one sent to POST /api/createAccount");
    }

    /**
     * DIRECTION: UI writes -> API reads. The reverse of the test above.
     *
     * Proves the registration form persists what the user typed, by reading it back
     * through a completely different channel. A UI-only test would assert on the
     * site's own confirmation page - the application agreeing with itself.
     */
    @Test(groups = {"ui", "api", "regression", "crossvalidation"},
          description = "An account registered through the browser is readable through the API with matching details")
    public void accountRegisteredThroughTheUiIsVisibleToTheApi() {
        UserAccount typed = TestDataFactory.newUser();

        ExtentManager.logStep("Registering " + typed.getEmail() + " through the browser form");
        homePage.header().goToSignupLogin();
        SignupPage signupPage = new LoginPage(driver).signup(typed.getName(), typed.getEmail());
        signupPage.createAccount(typed).clickContinue();
        Assert.assertTrue(homePage.header().isUserLoggedIn(), "Precondition failed: registration did not complete");

        ExtentManager.logStep("Reading the same account back through GET /api/getUserDetailByEmail");
        Response response = userApi.getUserDetailByEmail(typed.getEmail());
        Assert.assertEquals(responseCodeOf(response), AppConstants.API_OK,
                "The API could not find an account that was just registered through the browser");

        UserAccount stored = userApi.userFrom(response);
        Assert.assertEquals(stored.getFirstName(), typed.getFirstName(),
                "First name typed into the form did not reach the API");
        Assert.assertEquals(stored.getLastName(), typed.getLastName(),
                "Last name typed into the form did not reach the API");
        Assert.assertEquals(stored.getCity(), typed.getCity(),
                "City typed into the form did not reach the API");
        Assert.assertEquals(stored.getCountry(), typed.getCountry(),
                "Country chosen in the form did not reach the API");
        Assert.assertEquals(stored.getZipcode(), typed.getZipcode(),
                "Postcode typed into the form did not reach the API");

        // Registered through the UI, so remove it through the UI too.
        homePage.header().clickDeleteAccount();
        Assert.assertTrue(new AccountDeletedPage(driver).isAccountDeletedMessageVisible(),
                "Could not delete the account this test registered");
    }

    /**
     * Proves the shop renders the same catalogue the API publishes. A drift here -
     * a product live in the database but missing from the listing - is invisible to
     * either layer on its own.
     */
    @Test(groups = {"ui", "api", "regression", "crossvalidation"},
          description = "The product catalogue rendered by the shop matches the one the API publishes")
    public void uiCatalogueMatchesTheApiCatalogue() {
        List<Product> apiProducts = productApi.productsFrom(productApi.getAllProducts());
        Assert.assertFalse(apiProducts.isEmpty(), "Precondition failed: the API returned no products");

        List<String> uiNames = new ProductsPage(driver).open().getProductNames();
        ExtentManager.logStep("API lists " + apiProducts.size() + " products, the shop renders " + uiNames.size());

        Assert.assertEquals(uiNames.size(), apiProducts.size(),
                "The shop rendered " + uiNames.size() + " products but the API publishes "
                + apiProducts.size());

        Set<String> uiSet = new LinkedHashSet<>(uiNames);
        List<String> missingFromUi = apiProducts.stream()
                .map(Product::getName)
                .filter(name -> !uiSet.contains(name))
                .collect(Collectors.toList());

        Assert.assertTrue(missingFromUi.isEmpty(),
                "These products are published by the API but were not rendered by the shop: " + missingFromUi);
    }

    /**
     * Price is the field a shopper cares about most and the one most likely to drift
     * between a cached page and the database behind it.
     */
    @Test(groups = {"ui", "api", "regression", "crossvalidation"},
          description = "A product's price on its detail page matches the price the API reports")
    public void productPriceMatchesBetweenUiAndApi() {
        List<Product> apiProducts = productApi.productsFrom(productApi.getAllProducts());
        Product sample = apiProducts.get(0);
        ExtentManager.logStep("API reports '" + sample.getName() + "' at " + sample.getPrice());

        ProductDetailsPage details = new ProductDetailsPage(driver).open(sample.getId());

        Assert.assertEquals(details.getProductName(), sample.getName(),
                "Product " + sample.getId() + " is named differently in the shop and in the API");
        Assert.assertEquals(details.getPriceValue(), sample.getPriceValue(),
                "Product '" + sample.getName() + "' is priced at " + details.getPrice()
                + " in the shop but " + sample.getPrice() + " in the API");
    }

    /**
     * Search is a single feature exposed through two front doors. If the search box
     * and POST /api/searchProduct disagree, one of them is wrong - and no single-layer
     * test can tell you that.
     */
    @Test(groups = {"ui", "api", "regression", "crossvalidation"},
          description = "The search box and the search API return the same set of products")
    public void searchResultsMatchBetweenUiAndApi() {
        String term = "top";

        List<Product> apiResults = productApi.productsFrom(productApi.searchProduct(term));
        Set<String> apiNames = apiResults.stream().map(Product::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> uiNames = new LinkedHashSet<>(
                new ProductsPage(driver).open().searchFor(term).getProductNames());

        ExtentManager.logStep("API returned " + apiNames.size() + " results, the shop returned " + uiNames.size());

        Set<String> onlyInApi = new LinkedHashSet<>(apiNames);
        onlyInApi.removeAll(uiNames);
        Set<String> onlyInUi = new LinkedHashSet<>(uiNames);
        onlyInUi.removeAll(apiNames);

        Assert.assertTrue(onlyInApi.isEmpty() && onlyInUi.isEmpty(),
                "Search for '" + term + "' disagreed between the two layers."
                + " Only in the API: " + onlyInApi + "."
                + " Only in the shop: " + onlyInUi + ".");
    }

    /**
     * DIRECTION: API deletes -> UI must agree.
     *
     * The most security-relevant of these tests. "Account deleted!" from an endpoint
     * is worth nothing if the browser can still sign in with the same credentials.
     */
    @Test(groups = {"ui", "api", "regression", "crossvalidation"},
          description = "An account deleted through the API can no longer log in through the browser")
    public void accountDeletedViaApiCannotLogInThroughTheUi() {
        UserAccount user = accounts.createAccountViaApi();

        ExtentManager.logStep("Deleting " + user.getEmail() + " through DELETE /api/deleteAccount");
        Response delete = userApi.deleteAccount(user.getEmail(), user.getPassword());
        Assert.assertEquals(responseCodeOf(delete), AppConstants.API_OK,
                "Precondition failed: the API did not report the account as deleted");

        ExtentManager.logStep("Attempting to log in with the deleted credentials");
        homePage.header().goToSignupLogin();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(user.getEmail(), user.getPassword());

        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "An account deleted through the API could still log in through the browser");
        Assert.assertTrue(loginPage.isLoginErrorVisible(),
                "No login error was shown for credentials the API had already deleted");
        Assert.assertEquals(loginPage.getLoginErrorMessage(), AppConstants.LOGIN_ERROR_MESSAGE,
                "Unexpected error message for deleted credentials");
    }
}
