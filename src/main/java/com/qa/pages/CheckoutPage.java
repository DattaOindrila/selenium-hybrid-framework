package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /checkout - address verification, order review and the comment box. */
public class CheckoutPage extends BasePage {

    private final By addressDetailsHeading = By.xpath("//h2[normalize-space()='Address Details']");
    private final By reviewOrderHeading = By.xpath("//h2[normalize-space()='Review Your Order']");

    private final By deliveryAddressBlock = By.id("address_delivery");
    private final By billingAddressBlock = By.id("address_invoice");

    private final By deliveryName = By.cssSelector("#address_delivery li.address_firstname");
    private final By deliveryCity = By.cssSelector("#address_delivery li.address_city");
    private final By deliveryCountry = By.cssSelector("#address_delivery li.address_country_name");
    private final By deliveryPhone = By.cssSelector("#address_delivery li.address_phone");
    private final By deliveryAddressLines = By.cssSelector("#address_delivery li.address_address1");

    private final By orderComment = By.cssSelector("textarea[name='message']");
    private final By placeOrderButton = By.cssSelector("a.check_out[href='/payment']");
    private final By totalAmount = By.cssSelector("#cart_info tbody tr:last-child p.cart_total_price");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public CheckoutPage open() {
        openPath(AppConstants.CHECKOUT_PATH);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(addressDetailsHeading);
    }

    public boolean isReviewOrderSectionVisible() {
        return isDisplayed(reviewOrderHeading);
    }

    public boolean isDeliveryAddressVisible() {
        return isDisplayed(deliveryAddressBlock);
    }

    public boolean isBillingAddressVisible() {
        return isDisplayed(billingAddressBlock);
    }

    /** Rendered as "Mr. Alice Smith", including the title. */
    public String getDeliveryName() {
        return getText(deliveryName);
    }

    /** Rendered as "Kolkata West Bengal 700001" on one line. */
    public String getDeliveryCityStateZip() {
        return getText(deliveryCity);
    }

    public String getDeliveryCountry() {
        return getText(deliveryCountry);
    }

    public String getDeliveryPhone() {
        return getText(deliveryPhone);
    }

    /**
     * The company, address line 1 and address line 2 all share the class
     * address_address1, so this returns the block as one joined string and the test
     * asserts that it contains the value it supplied.
     */
    public String getDeliveryAddressBlock() {
        return String.join(" | ", findAll(deliveryAddressLines).stream().map(e -> e.getText().trim()).toList());
    }

    public int getTotalAmountValue() {
        return Integer.parseInt(getText(totalAmount).replaceAll("[^0-9]", ""));
    }

    public CheckoutPage enterOrderComment(String comment) {
        type(orderComment, comment);
        return this;
    }

    public PaymentPage placeOrder() {
        click(placeOrderButton);
        return new PaymentPage(driver);
    }
}
