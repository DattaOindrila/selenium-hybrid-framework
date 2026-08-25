package com.qa.pages;

import com.qa.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The "Account Created!" confirmation. */
public class AccountCreatedPage extends BasePage {

    private final By accountCreatedHeading = By.cssSelector("h2[data-qa='account-created']");
    private final By continueButton = By.cssSelector("a[data-qa='continue-button']");

    public AccountCreatedPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAccountCreatedMessageVisible() {
        return isDisplayed(accountCreatedHeading);
    }

    public String getMessage() {
        return getText(accountCreatedHeading);
    }

    public HomePage clickContinue() {
        click(continueButton);
        return new HomePage(driver);
    }
}
