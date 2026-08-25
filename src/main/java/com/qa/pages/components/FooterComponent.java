package com.qa.pages.components;

import com.qa.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The newsletter subscription block in the footer of every page.
 *
 * NOTE the id: "susbscribe_email". That transposition is the application's, not a
 * typo here - copying it exactly is the difference between a locator that works
 * and one that does not.
 */
public class FooterComponent extends BasePage {

    private final By subscriptionHeading = By.xpath("//div[@class='single-widget']/h2");
    private final By emailInput = By.id("susbscribe_email");
    private final By subscribeButton = By.id("subscribe");
    private final By successMessage = By.cssSelector("div.alert-success.alert");

    public FooterComponent(WebDriver driver) {
        super(driver);
    }

    public void subscribeWith(String email) {
        scrollToBottom();
        type(emailInput, email);
        click(subscribeButton);
    }

    public boolean isSubscriptionHeadingVisible() {
        scrollToBottom();
        return isDisplayed(subscriptionHeading);
    }

    public String getSubscriptionHeading() {
        scrollToBottom();
        return getText(subscriptionHeading);
    }

    public boolean isSubscriptionSuccessVisible() {
        return isDisplayed(successMessage);
    }

    public String getSubscriptionSuccessMessage() {
        return getText(successMessage);
    }

    /** The browser blocks the form's own validation message from Selenium, so read the attribute. */
    public boolean isEmailFieldRequired() {
        return getAttribute(emailInput, "type").equals("email");
    }
}
