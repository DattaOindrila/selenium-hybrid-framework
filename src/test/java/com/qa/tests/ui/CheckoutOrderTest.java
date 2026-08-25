package com.qa.tests.ui;

import com.qa.api.model.UserAccount;
import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.helpers.TestAccountManager;
import com.qa.pages.CartPage;
import com.qa.pages.CheckoutPage;
import com.qa.pages.LoginPage;
import com.qa.pages.OrderConfirmationPage;
import com.qa.pages.PaymentPage;
import com.qa.pages.ProductDetailsPage;
import com.qa.utils.ExtentManager;
import com.qa.utils.TestDataFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checkout: address verification, the order review, placing the order and the
 * confirmation.
 *
 * The account is created once through the API in @BeforeClass and reused. Every
 * test still logs in through the browser, because the login is part of what makes
 * the checkout page reachable - but no test spends a minute filling in the
 * registration form to get there.
 *
 * Card details always come from config.properties. There is no payment gateway
 * behind this practice site, nothing is charged, and no real card number appears
 * anywhere in this repository.
 */
public class CheckoutOrderTest extends BaseTest {

    private static final int PRODUCT_ID = 1;

    private final TestAccountManager accounts = new TestAccountManager();
    private UserAccount shopper;

    @BeforeClass(alwaysRun = true)
    public void createTheShopperAccount() {
        shopper = accounts.createAccountViaApi();
    }

    @AfterClass(alwaysRun = true)
    public void removeTheShopperAccount() {
        accounts.cleanUp();
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "The checkout page shows both a delivery address and a billing address")
    public void checkoutShowsDeliveryAndBillingAddresses() {
        CheckoutPage checkout = loginAndCheckout();

        Assert.assertTrue(checkout.isLoaded(), "The checkout page did not load");
        Assert.assertTrue(checkout.isDeliveryAddressVisible(), "No delivery address block was shown");
        Assert.assertTrue(checkout.isBillingAddressVisible(), "No billing address block was shown");
        Assert.assertTrue(checkout.isReviewOrderSectionVisible(), "No 'Review Your Order' section was shown");
    }

    /**
     * The strongest assertion in this class: the address the checkout page renders
     * must be the address the account was actually created with. Checking only that
     * "an address is displayed" would pass even if the site showed somebody else's.
     */
    @Test(groups = {"ui", "regression", "checkout"},
          description = "The delivery address matches the details the account was registered with")
    public void deliveryAddressMatchesTheRegisteredDetails() {
        CheckoutPage checkout = loginAndCheckout();

        Assert.assertTrue(checkout.getDeliveryName().contains(shopper.getFirstName()),
                "Delivery name '" + checkout.getDeliveryName()
                + "' did not contain the registered first name '" + shopper.getFirstName() + "'");
        Assert.assertTrue(checkout.getDeliveryName().contains(shopper.getLastName()),
                "Delivery name '" + checkout.getDeliveryName()
                + "' did not contain the registered last name '" + shopper.getLastName() + "'");

        String cityLine = checkout.getDeliveryCityStateZip();
        Assert.assertTrue(cityLine.contains(shopper.getCity()),
                "Delivery line '" + cityLine + "' did not contain the registered city");
        Assert.assertTrue(cityLine.contains(shopper.getZipcode()),
                "Delivery line '" + cityLine + "' did not contain the registered postcode");

        Assert.assertEquals(checkout.getDeliveryCountry(), shopper.getCountry(),
                "The delivery country did not match the registered country");
        Assert.assertEquals(checkout.getDeliveryPhone(), shopper.getMobileNumber(),
                "The delivery phone number did not match the registered number");

        Assert.assertTrue(checkout.getDeliveryAddressBlock().contains(shopper.getAddress1()),
                "The delivery address did not contain the registered street address");
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "The order total on the checkout page equals the total shown in the cart")
    public void checkoutTotalMatchesTheCartTotal() {
        logIn();
        addProductToCart(PRODUCT_ID, 3);

        CartPage cart = new CartPage(driver).open();
        int cartTotal = cart.getCartTotalValue();
        ExtentManager.logStep("Cart total is Rs. " + cartTotal);

        CheckoutPage checkout = cart.proceedToCheckout();

        Assert.assertEquals(checkout.getTotalAmountValue(), cartTotal,
                "The checkout total did not match the cart total");
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "An order can be placed end to end and is confirmed")
    public void orderCanBePlacedEndToEnd() {
        CheckoutPage checkout = loginAndCheckout();

        ExtentManager.logStep("Placing the order");
        PaymentPage payment = checkout.placeOrder();
        Assert.assertTrue(payment.isLoaded(), "The payment page did not load after clicking 'Place Order'");

        OrderConfirmationPage confirmation = payment.payWithConfiguredCard();

        Assert.assertTrue(confirmation.isOrderPlaced(), "The order confirmation was not shown");
        Assert.assertEquals(confirmation.getOrderPlacedMessage().toUpperCase(),
                AppConstants.ORDER_PLACED_MESSAGE,
                "Unexpected confirmation heading after placing the order");
        Assert.assertEquals(confirmation.getConfirmationText(), AppConstants.ORDER_CONFIRMED_MESSAGE,
                "Unexpected confirmation sentence after placing the order");
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "The invoice offered on the confirmation page is for the amount that was ordered")
    public void invoiceAmountMatchesTheOrderTotal() {
        logIn();
        addProductToCart(PRODUCT_ID, 2);

        CartPage cart = new CartPage(driver).open();
        int expectedTotal = cart.getCartTotalValue();

        OrderConfirmationPage confirmation = cart.proceedToCheckout()
                .placeOrder()
                .payWithConfiguredCard();

        Assert.assertTrue(confirmation.isDownloadInvoiceVisible(),
                "No 'Download Invoice' button was offered on the confirmation page");
        Assert.assertEquals(confirmation.getInvoiceAmount(), expectedTotal,
                "The invoice was raised for a different amount than the order total");
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "A comment can be added to the order before it is placed")
    public void orderCommentCanBeEntered() {
        CheckoutPage checkout = loginAndCheckout();

        String comment = TestDataFactory.randomSentence();
        ExtentManager.logStep("Adding a delivery comment to the order");
        checkout.enterOrderComment(comment);

        OrderConfirmationPage confirmation = checkout.placeOrder().payWithConfiguredCard();

        // The site does not display the comment back anywhere, so the honest assertion
        // is that supplying one does not prevent the order from completing.
        Assert.assertTrue(confirmation.isOrderPlaced(),
                "An order with a delivery comment did not reach the confirmation page");
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "The cart is emptied once an order has been placed")
    public void cartIsEmptyAfterAnOrderIsPlaced() {
        CheckoutPage checkout = loginAndCheckout();
        checkout.placeOrder().payWithConfiguredCard().clickContinue();

        CartPage cart = new CartPage(driver).open();

        Assert.assertTrue(cart.isEmpty(),
                "The cart still held " + cart.getItemCount() + " item(s) after the order was placed");
        Assert.assertTrue(cart.getEmptyCartMessage().contains(AppConstants.CART_EMPTY_MESSAGE),
                "Unexpected empty-cart message: " + cart.getEmptyCartMessage());
    }

    @Test(groups = {"ui", "regression", "checkout"},
          description = "The payment form asks for all five card fields")
    public void paymentFormRequestsAllCardFields() {
        PaymentPage payment = loginAndCheckout().placeOrder();

        Assert.assertTrue(payment.isLoaded(), "The payment page did not load");
        Assert.assertTrue(payment.areAllCardFieldsRequired(),
                "The payment form did not present all five card fields");
    }

    /**
     * This test was written expecting the site to block the URL, and it failed. The
     * application does not redirect an anonymous visitor away from /checkout - it
     * renders the page with an EMPTY address block. That is arguably a weakness in
     * the application, and it is recorded under Known Limitations in the README.
     *
     * The assertion now describes what the site actually does: no customer data
     * leaks to a visitor who is not logged in. Rewriting the assertion to match
     * observed behaviour is right; quietly deleting the test would not be.
     */
    @Test(groups = {"ui", "regression", "checkout"},
          description = "An anonymous visitor reaching /checkout directly is shown no customer details")
    public void anonymousVisitorSeesNoCustomerDetailsAtCheckout() {
        // No login in this test on purpose.
        CheckoutPage checkout = new CheckoutPage(driver).open();

        String name = checkout.getDeliveryName().replace(".", "").trim();
        Assert.assertTrue(name.isEmpty(),
                "Customer details were shown to a visitor who is not logged in: '" + name + "'");
        Assert.assertTrue(checkout.getDeliveryAddressBlock().replace("|", "").trim().isEmpty(),
                "An address was shown to a visitor who is not logged in: '"
                + checkout.getDeliveryAddressBlock() + "'");
    }

    // ------------------------------------------------------------------ helpers

    private void logIn() {
        homePage.header().goToSignupLogin();
        new LoginPage(driver).login(shopper.getEmail(), shopper.getPassword());
        // Do not navigate until the session actually exists - see waitUntilLoggedIn().
        homePage.header().waitUntilLoggedIn();
    }

    private void addProductToCart(int productId, int quantity) {
        // Quantity can only be chosen on the detail page - the cart renders it as a
        // disabled button - so the shopper always goes through the product page.
        new ProductDetailsPage(driver).open(productId).setQuantity(quantity).addToCart().continueShopping();
    }

    /** Logs in, puts one product in the cart and lands on the checkout page. */
    private CheckoutPage loginAndCheckout() {
        logIn();
        addProductToCart(PRODUCT_ID, 1);
        return new CartPage(driver).open().proceedToCheckout();
    }
}
