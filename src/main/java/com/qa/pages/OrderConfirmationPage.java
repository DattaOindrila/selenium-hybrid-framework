package com.qa.pages;

import com.qa.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /payment_done/{amount} - the "Order Placed!" confirmation. */
public class OrderConfirmationPage extends BasePage {

    private final By orderPlacedHeading = By.cssSelector("h2[data-qa='order-placed']");
    private final By confirmationText = By.xpath("//p[contains(.,'Congratulations')]");
    private final By downloadInvoiceButton = By.cssSelector("a[href^='/download_invoice/']");
    private final By continueButton = By.cssSelector("a[data-qa='continue-button']");

    public OrderConfirmationPage(WebDriver driver) {
        super(driver);
    }

    public boolean isOrderPlaced() {
        return isDisplayed(orderPlacedHeading);
    }

    public String getOrderPlacedMessage() {
        return getText(orderPlacedHeading);
    }

    public String getConfirmationText() {
        return getText(confirmationText);
    }

    public boolean isDownloadInvoiceVisible() {
        return isDisplayed(downloadInvoiceButton);
    }

    /**
     * The invoice link is /download_invoice/{total}, so the order total can be read
     * back from the confirmation page without downloading anything.
     */
    public int getInvoiceAmount() {
        String href = getAttribute(downloadInvoiceButton, "href");
        return Integer.parseInt(href.substring(href.lastIndexOf('/') + 1));
    }

    public HomePage clickContinue() {
        click(continueButton);
        return new HomePage(driver);
    }
}
