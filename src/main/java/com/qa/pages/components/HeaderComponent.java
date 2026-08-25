package com.qa.pages.components;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The navigation bar that appears on every page.
 *
 * Declared once here instead of being copied into every page object: when the site
 * renames the "Delete Account" link, this is the only file that changes.
 */
public class HeaderComponent extends BasePage {

    private final By homeLink = By.cssSelector(".shop-menu a[href='/']");
    private final By productsLink = By.cssSelector(".shop-menu a[href='/products']");
    private final By cartLink = By.cssSelector(".shop-menu a[href='/view_cart']");
    private final By signupLoginLink = By.cssSelector(".shop-menu a[href='/login']");
    private final By testCasesLink = By.cssSelector(".shop-menu a[href='/test_cases']");
    private final By apiTestingLink = By.cssSelector(".shop-menu a[href='/api_list']");
    private final By contactUsLink = By.cssSelector(".shop-menu a[href='/contact_us']");
    private final By logoutLink = By.cssSelector(".shop-menu a[href='/logout']");
    private final By deleteAccountLink = By.cssSelector(".shop-menu a[href='/delete_account']");
    private final By loggedInAsLabel = By.xpath("//a[contains(normalize-space(.),'Logged in as')]");

    public HeaderComponent(WebDriver driver) {
        super(driver);
    }

    public void goToHome() {
        click(homeLink);
    }

    public void goToProducts() {
        click(productsLink);
    }

    public void goToCart() {
        click(cartLink);
    }

    public void goToSignupLogin() {
        click(signupLoginLink);
    }

    public void goToTestCases() {
        click(testCasesLink);
    }

    public void goToApiTesting() {
        click(apiTestingLink);
    }

    public void goToContactUs() {
        click(contactUsLink);
    }

    public void clickLogout() {
        click(logoutLink);
    }

    public void clickDeleteAccount() {
        click(deleteAccountLink);
    }

    /**
     * The "Logged in as X" label is the site's own indicator of an authenticated
     * session, so it is what the login tests assert on rather than a URL or a
     * cookie.
     */
    public boolean isUserLoggedIn() {
        return isDisplayed(loggedInAsLabel);
    }

    public String getLoggedInUsername() {
        // Rendered as "Logged in as Alice"
        return getText(loggedInAsLabel).replace("Logged in as", "").trim();
    }

    public boolean isLogoutVisible() {
        return isDisplayed(logoutLink);
    }

    public boolean isSignupLoginVisible() {
        return isDisplayed(signupLoginLink);
    }

    public boolean isDeleteAccountVisible() {
        return isDisplayed(deleteAccountLink);
    }

    /** Direct navigation, used by teardown where clicking through the UI adds no value. */
    public void openLogoutUrl() {
        openPath(AppConstants.LOGOUT_PATH);
    }
}
