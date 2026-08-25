package com.qa.pages;

import com.qa.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The "Account Deleted!" confirmation. */
public class AccountDeletedPage extends BasePage {

    private final By accountDeletedHeading = By.cssSelector("h2[data-qa='account-deleted']");
    private final By continueButton = By.cssSelector("a[data-qa='continue-button']");

    public AccountDeletedPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAccountDeletedMessageVisible() {
        return isDisplayed(accountDeletedHeading);
    }

    public String getMessage() {
        return getText(accountDeletedHeading);
    }

    public HomePage clickContinue() {
        click(continueButton);
        return new HomePage(driver);
    }
}
