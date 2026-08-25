package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import com.qa.utils.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * /payment.
 *
 * The card values come from config.properties, never from a literal in a test.
 * This site has no payment gateway behind it - nothing is processed and no real
 * card is ever involved.
 */
public class PaymentPage extends BasePage {

    private final By nameOnCard = By.cssSelector("input[data-qa='name-on-card']");
    private final By cardNumber = By.cssSelector("input[data-qa='card-number']");
    private final By cvc = By.cssSelector("input[data-qa='cvc']");
    private final By expiryMonth = By.cssSelector("input[data-qa='expiry-month']");
    private final By expiryYear = By.cssSelector("input[data-qa='expiry-year']");
    private final By payButton = By.cssSelector("button[data-qa='pay-button']");

    public PaymentPage(WebDriver driver) {
        super(driver);
    }

    public PaymentPage open() {
        openPath(AppConstants.PAYMENT_PATH);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(nameOnCard);
    }

    /** Fills the form from config.properties. */
    public PaymentPage fillCardDetailsFromConfig() {
        return fillCardDetails(
                ConfigReader.get("payment.card.name"),
                ConfigReader.get("payment.card.number"),
                ConfigReader.get("payment.card.cvc"),
                ConfigReader.get("payment.card.expiry.month"),
                ConfigReader.get("payment.card.expiry.year"));
    }

    public PaymentPage fillCardDetails(String name, String number, String cvcValue,
                                       String month, String year) {
        type(nameOnCard, name);
        type(cardNumber, number);
        type(cvc, cvcValue);
        type(expiryMonth, month);
        type(expiryYear, year);
        return this;
    }

    public OrderConfirmationPage payAndConfirm() {
        log.info("Submitting the payment form");
        click(payButton);
        return new OrderConfirmationPage(driver);
    }

    /** Convenience: config card details, submitted. */
    public OrderConfirmationPage payWithConfiguredCard() {
        return fillCardDetailsFromConfig().payAndConfirm();
    }

    public boolean areAllCardFieldsRequired() {
        return isPresent(nameOnCard) && isPresent(cardNumber)
                && isPresent(cvc) && isPresent(expiryMonth) && isPresent(expiryYear);
    }
}
