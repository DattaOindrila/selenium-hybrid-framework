package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * /login - which hosts two independent forms, "Login to your account" and
 * "New User Signup!". Both live here because they are one page; the account
 * information form that signup leads to is a separate page object.
 */
public class LoginPage extends BasePage {

    private final By loginHeading = By.xpath("//div[@class='login-form']/h2");
    private final By loginEmail = By.cssSelector("input[data-qa='login-email']");
    private final By loginPassword = By.cssSelector("input[data-qa='login-password']");
    private final By loginButton = By.cssSelector("button[data-qa='login-button']");

    private final By signupHeading = By.xpath("//div[@class='signup-form']/h2");
    private final By signupName = By.cssSelector("input[data-qa='signup-name']");
    private final By signupEmail = By.cssSelector("input[data-qa='signup-email']");
    private final By signupButton = By.cssSelector("button[data-qa='signup-button']");

    // The site renders both failure messages as a red <p>. Scoping each to its own
    // form keeps "wrong password" and "email already taken" distinguishable.
    private final By loginErrorMessage = By.cssSelector(".login-form p[style*='color: red']");
    private final By signupErrorMessage = By.cssSelector(".signup-form p[style*='color: red']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        openPath(AppConstants.LOGIN_PATH);
        return this;
    }

    // ------------------------------------------------------------------- login

    public boolean isLoginFormVisible() {
        return isDisplayed(loginHeading);
    }

    public String getLoginHeading() {
        return getText(loginHeading);
    }

    /**
     * Fills and submits the login form.
     *
     * Returns void rather than a page object because the destination depends on the
     * outcome: success lands on the home page, failure stays here. The test asserts
     * which one happened.
     */
    public void login(String email, String password) {
        log.info("Logging in as {}", email);
        type(loginEmail, email);
        type(loginPassword, password);
        click(loginButton);
    }

    public boolean isLoginErrorVisible() {
        return isDisplayed(loginErrorMessage);
    }

    public String getLoginErrorMessage() {
        return getText(loginErrorMessage);
    }

    /** Reads HTML5 validation state, used by the empty-field tests. */
    public boolean isLoginEmailFieldValid() {
        return Boolean.TRUE.equals(((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return arguments[0].checkValidity();",
                        wait.waitForPresence(loginEmail)));
    }

    public String getLoginEmailValidationMessage() {
        return (String) ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return arguments[0].validationMessage;",
                        wait.waitForPresence(loginEmail));
    }

    public void clickLoginButton() {
        click(loginButton);
    }

    // ------------------------------------------------------------------ signup

    public boolean isSignupFormVisible() {
        return isDisplayed(signupHeading);
    }

    public String getSignupHeading() {
        return getText(signupHeading);
    }

    /** On success the browser lands on the account information page. */
    public SignupPage signup(String name, String email) {
        log.info("Starting signup for {}", email);
        type(signupName, name);
        type(signupEmail, email);
        click(signupButton);
        return new SignupPage(driver);
    }

    /** For the duplicate-email case, where no SignupPage is ever reached. */
    public void submitSignup(String name, String email) {
        type(signupName, name);
        type(signupEmail, email);
        click(signupButton);
    }

    public boolean isSignupErrorVisible() {
        return isDisplayed(signupErrorMessage);
    }

    public String getSignupErrorMessage() {
        return getText(signupErrorMessage);
    }
}
