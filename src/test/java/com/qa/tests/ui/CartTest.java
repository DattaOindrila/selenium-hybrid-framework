package com.qa.tests.ui;

import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.pages.CartPage;
import com.qa.pages.ProductDetailsPage;
import com.qa.pages.ProductsPage;
import com.qa.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Shopping cart behaviour: what lands in the cart, what the cart computes, and
 * what it refuses to do for a visitor who is not logged in.
 *
 * ON QUANTITY
 * -----------
 * The cart renders its quantity as a DISABLED button, not an input. There is no
 * in-cart quantity editor on this site, so "order four of something" can only be
 * expressed on the product details page BEFORE the item is added. That is why the
 * quantity scenarios below go through ProductDetailsPage.setQuantity() instead of
 * trying to type into the cart - a test that tried to edit the cart directly would
 * be testing a control that does not exist.
 *
 * ON SESSION STATE
 * ----------------
 * BaseTest starts a fresh browser for every test method, so each test below begins
 * with a genuinely empty cart. Nothing here has to clean up after itself, and the
 * "empty cart" test is a real assertion rather than a side effect of ordering.
 *
 * Every test runs anonymously. None of them creates an account, so there is no
 * account teardown to do.
 */
public class CartTest extends BaseTest {

    // Product ids 1..34 exist on the site. Two are enough for every scenario here;
    // naming them by id rather than by product name keeps the tests working if the
    // shop ever renames an item.
    private static final int PRODUCT_ID_A = 1;
    private static final int PRODUCT_ID_B = 2;

    /** Deliberately more than one, so a quantity bug cannot hide behind "x 1". */
    private static final int BULK_QUANTITY = 4;

    // ------------------------------------------------------------ adding items

    @Test(groups = {"ui", "regression", "cart", "smoke"},
          description = "Adding a single product from the products page puts exactly one row in the cart, with a quantity of one")
    public void singleProductAppearsInCartWithQuantityOne() {
        ExtentManager.logStep("Opening the products page and adding one product to the cart");
        CartPage cartPage = addProductAndOpenCart(PRODUCT_ID_A);

        ExtentManager.logStep("Checking the cart holds that one product");
        Assert.assertTrue(cartPage.containsProduct(PRODUCT_ID_A),
                "Product " + PRODUCT_ID_A + " was added but has no row in the cart");
        Assert.assertEquals(cartPage.getItemCount(), 1,
                "Cart should hold exactly one row after adding one product");
        Assert.assertEquals(cartPage.getQuantity(PRODUCT_ID_A), 1,
                "A product added straight from the listing should arrive with quantity 1");
    }

    @Test(groups = {"ui", "regression", "cart"},
          description = "Adding two different products gives the cart two separate rows, one for each product")
    public void twoDifferentProductsProduceTwoCartRows() {
        ExtentManager.logStep("Adding two different products to the cart");
        CartPage cartPage = addTwoProductsAndOpenCart();

        ExtentManager.logStep("Checking both products have their own row");
        Assert.assertEquals(cartPage.getItemCount(), 2,
                "Two different products should produce two cart rows, not one merged row");
        Assert.assertTrue(cartPage.containsProduct(PRODUCT_ID_A),
                "Product " + PRODUCT_ID_A + " is missing from a cart that should hold both products");
        Assert.assertTrue(cartPage.containsProduct(PRODUCT_ID_B),
                "Product " + PRODUCT_ID_B + " is missing from a cart that should hold both products");
    }

    // ----------------------------------------------------------- cart arithmetic

    @Test(groups = {"ui", "regression", "cart"},
          description = "The total shown against a cart line equals that product's unit price multiplied by its quantity")
    public void lineTotalEqualsUnitPriceTimesQuantity() {
        ExtentManager.logStep("Adding one product so there is a line to check");
        CartPage cartPage = addProductAndOpenCart(PRODUCT_ID_A);

        int unitPrice = cartPage.getUnitPriceValue(PRODUCT_ID_A);
        int quantity = cartPage.getQuantity(PRODUCT_ID_A);
        int lineTotal = cartPage.getLineTotalValue(PRODUCT_ID_A);
        ExtentManager.logStep("Cart shows unit price " + unitPrice + " x quantity " + quantity
                + " and a line total of " + lineTotal);

        // The explicit arithmetic and the page object's own check are both asserted:
        // the first says what the expected number is when it fails, the second is the
        // reusable rule the rest of the suite leans on.
        Assert.assertEquals(lineTotal, unitPrice * quantity,
                "Line total for product " + PRODUCT_ID_A + " should be unit price x quantity");
        Assert.assertTrue(cartPage.isLineTotalCorrect(PRODUCT_ID_A),
                "Cart did not compute the line total for product " + PRODUCT_ID_A + " correctly");
    }

    @Test(groups = {"ui", "regression", "cart"},
          description = "Choosing a quantity of four on the product details page shows four in the cart, priced at four times the unit price")
    public void quantityChosenOnDetailsPageIsCarriedIntoTheCart() {
        // Quantity cannot be edited inside the cart - it is a disabled button there -
        // so the only way to order more than one of something is to set it here first.
        ExtentManager.logStep("Opening product " + PRODUCT_ID_A + " and setting the quantity to " + BULK_QUANTITY);
        ProductDetailsPage detailsPage = new ProductDetailsPage(driver).open(PRODUCT_ID_A);
        Assert.assertTrue(detailsPage.isLoaded(),
                "Product details page for product " + PRODUCT_ID_A + " did not load");

        int unitPrice = detailsPage.getPriceValue();
        detailsPage.setQuantity(BULK_QUANTITY);
        Assert.assertEquals(detailsPage.getQuantityValue(), String.valueOf(BULK_QUANTITY),
                "Quantity box on the details page did not accept the value that was typed into it");

        ExtentManager.logStep("Adding it to the cart and opening the cart");
        CartPage cartPage = detailsPage.addToCart().viewCart();

        ExtentManager.logStep("Checking the cart kept the chosen quantity and priced it accordingly");
        Assert.assertEquals(cartPage.getQuantity(PRODUCT_ID_A), BULK_QUANTITY,
                "Cart lost the quantity chosen on the details page");
        Assert.assertEquals(cartPage.getLineTotalValue(PRODUCT_ID_A), unitPrice * BULK_QUANTITY,
                "Line total should be " + BULK_QUANTITY + " x the unit price of " + unitPrice);
        Assert.assertTrue(cartPage.isLineTotalCorrect(PRODUCT_ID_A),
                "Cart did not compute the line total correctly for a quantity of " + BULK_QUANTITY);
    }

    @Test(groups = {"ui", "regression", "cart"},
          description = "The cart total for a two-product cart equals the sum of the two line totals")
    public void cartTotalEqualsTheSumOfTheLineTotals() {
        ExtentManager.logStep("Building a cart with two different products");
        CartPage cartPage = addTwoProductsAndOpenCart();

        int firstLine = cartPage.getLineTotalValue(PRODUCT_ID_A);
        int secondLine = cartPage.getLineTotalValue(PRODUCT_ID_B);
        ExtentManager.logStep("Line totals are " + firstLine + " and " + secondLine);

        // Each line is checked first: without this, a wrong grand total and a wrong
        // line total could cancel each other out and the test would still pass.
        Assert.assertTrue(cartPage.isLineTotalCorrect(PRODUCT_ID_A),
                "Line total for product " + PRODUCT_ID_A + " is wrong, so the cart total cannot be trusted");
        Assert.assertTrue(cartPage.isLineTotalCorrect(PRODUCT_ID_B),
                "Line total for product " + PRODUCT_ID_B + " is wrong, so the cart total cannot be trusted");
        Assert.assertEquals(cartPage.getCartTotalValue(), firstLine + secondLine,
                "Cart total should be the sum of the two line totals");
    }

    // --------------------------------------------------------------- removing

    @Test(groups = {"ui", "regression", "cart"},
          description = "Removing one product from a two-product cart deletes only that row and leaves one item behind")
    public void removingOneProductRemovesItsRowAndDropsTheCount() {
        ExtentManager.logStep("Building a cart with two different products");
        CartPage cartPage = addTwoProductsAndOpenCart();
        Assert.assertEquals(cartPage.getItemCount(), 2,
                "Test setup failed: the cart should hold two products before anything is removed");

        ExtentManager.logStep("Removing product " + PRODUCT_ID_A);
        cartPage.removeProduct(PRODUCT_ID_A);

        Assert.assertFalse(cartPage.containsProduct(PRODUCT_ID_A),
                "Removed product " + PRODUCT_ID_A + " still has a row in the cart");
        Assert.assertTrue(cartPage.containsProduct(PRODUCT_ID_B),
                "Removing one product also removed product " + PRODUCT_ID_B + ", which was left alone");
        Assert.assertEquals(cartPage.getItemCount(), 1,
                "Item count should drop from two to one after removing a single product");
    }

    @Test(groups = {"ui", "regression", "cart"},
          description = "Removing the last remaining product empties the cart and shows the 'Cart is empty!' message")
    public void removingTheOnlyProductLeavesTheEmptyCartMessage() {
        ExtentManager.logStep("Adding a single product, then removing it again");
        CartPage cartPage = addProductAndOpenCart(PRODUCT_ID_A);
        Assert.assertEquals(cartPage.getItemCount(), 1,
                "Test setup failed: the cart should hold exactly one product before it is removed");

        cartPage.removeProduct(PRODUCT_ID_A);

        ExtentManager.logStep("Checking the cart falls back to its empty state");
        Assert.assertTrue(cartPage.isEmpty(),
                "Cart should show its empty state once the last product is removed");
        Assert.assertEquals(cartPage.getItemCount(), 0,
                "An empty cart should report zero items");
        // The banner reads "Cart is empty! Click here to buy products.", so the
        // expected constant is a substring of it rather than the whole text.
        Assert.assertTrue(cartPage.getEmptyCartMessage().contains(AppConstants.CART_EMPTY_MESSAGE),
                "Empty cart banner should contain '" + AppConstants.CART_EMPTY_MESSAGE
                        + "' but read '" + cartPage.getEmptyCartMessage() + "'");
    }

    // ------------------------------------------------------- empty / persistence

    @Test(groups = {"ui", "regression", "cart"},
          description = "A visitor who has just arrived on the site sees an empty cart")
    public void cartIsEmptyForABrandNewSession() {
        // BaseTest opens a brand new browser for each test, so nothing has ever been
        // added in this session. This is the baseline every other cart test assumes.
        ExtentManager.logStep("Opening the cart without adding anything first");
        CartPage cartPage = new CartPage(driver).open();

        Assert.assertTrue(cartPage.isLoaded(), "Cart page did not load");
        Assert.assertTrue(cartPage.isEmpty(),
                "A cart should be empty at the start of a fresh browser session");
        Assert.assertEquals(cartPage.getItemCount(), 0,
                "A fresh session's cart should report zero items");
        Assert.assertTrue(cartPage.getEmptyCartMessage().contains(AppConstants.CART_EMPTY_MESSAGE),
                "Empty cart banner should contain '" + AppConstants.CART_EMPTY_MESSAGE
                        + "' but read '" + cartPage.getEmptyCartMessage() + "'");
    }

    @Test(groups = {"ui", "regression", "cart"},
          description = "A product stays in the cart after the shopper browses away to the home page and comes back")
    public void cartSurvivesNavigatingAwayAndBack() {
        ExtentManager.logStep("Adding a product, then browsing away to the home page");
        ProductsPage productsPage = new ProductsPage(driver).open();
        productsPage.addProductToCartById(PRODUCT_ID_A);
        productsPage.continueShopping();
        productsPage.header().goToHome();
        Assert.assertTrue(homePage.isLoaded(), "Home page did not load after navigating away from the cart");

        ExtentManager.logStep("Returning to the cart through the header link");
        homePage.header().goToCart();
        CartPage cartPage = new CartPage(driver);

        Assert.assertFalse(cartPage.isEmpty(),
                "Cart was emptied simply by navigating to another page");
        Assert.assertTrue(cartPage.containsProduct(PRODUCT_ID_A),
                "Product " + PRODUCT_ID_A + " should still be in the cart after browsing elsewhere");
        Assert.assertEquals(cartPage.getItemCount(), 1,
                "The cart should still hold the one product added before navigating away");
    }

    // --------------------------------------------------- the "Added!" modal

    @Test(groups = {"ui", "regression", "cart"},
          description = "Clicking 'Continue Shopping' in the added-to-cart popup closes it and leaves the shopper on the products page")
    public void continueShoppingKeepsTheUserOnTheProductsPage() {
        ExtentManager.logStep("Adding a product so the confirmation popup appears");
        ProductsPage productsPage = new ProductsPage(driver).open();
        productsPage.addProductToCartById(PRODUCT_ID_A);
        Assert.assertTrue(productsPage.isCartModalVisible(),
                "The added-to-cart popup should appear after adding a product");

        ExtentManager.logStep("Dismissing the popup with 'Continue Shopping'");
        productsPage.continueShopping();

        Assert.assertFalse(productsPage.isCartModalVisible(),
                "'Continue Shopping' should close the added-to-cart popup");
        Assert.assertTrue(productsPage.isLoaded(),
                "'Continue Shopping' should leave the shopper on the products page");
        Assert.assertTrue(productsPage.getCurrentUrl().contains(AppConstants.PRODUCTS_PATH),
                "URL should still be the products listing but was " + productsPage.getCurrentUrl());
    }

    @Test(groups = {"ui", "regression", "cart"},
          description = "Clicking 'View Cart' in the added-to-cart popup opens the cart page showing the product just added")
    public void viewCartFromTheModalOpensTheCartPage() {
        ExtentManager.logStep("Adding a product so the confirmation popup appears");
        ProductsPage productsPage = new ProductsPage(driver).open();
        productsPage.addProductToCartById(PRODUCT_ID_A);
        Assert.assertTrue(productsPage.isCartModalVisible(),
                "The added-to-cart popup should appear after adding a product");

        ExtentManager.logStep("Following the 'View Cart' link in the popup");
        CartPage cartPage = productsPage.viewCartFromModal();

        Assert.assertTrue(cartPage.isLoaded(), "Cart page did not load after clicking 'View Cart'");
        Assert.assertTrue(cartPage.getCurrentUrl().contains(AppConstants.CART_PATH),
                "'View Cart' should navigate to " + AppConstants.CART_PATH
                        + " but landed on " + cartPage.getCurrentUrl());
        Assert.assertTrue(cartPage.containsProduct(PRODUCT_ID_A),
                "The cart opened from the popup should already show the product that was just added");
    }

    // --------------------------------------------------------------- checkout

    @Test(groups = {"ui", "regression", "cart"},
          description = "A shopper who is not logged in is asked to register or log in when trying to check out")
    public void anonymousVisitorIsAskedToRegisterOrLoginAtCheckout() {
        // Negative path: checkout is only available to an authenticated user, so this
        // asserts the site stops an anonymous visitor rather than letting them through.
        ExtentManager.logStep("Filling a cart while staying logged out");
        CartPage cartPage = addProductAndOpenCart(PRODUCT_ID_A);
        Assert.assertTrue(homePage.header().isSignupLoginVisible(),
                "Test setup failed: this test must run as an anonymous visitor, but the header shows a session");

        ExtentManager.logStep("Clicking 'Proceed To Checkout' as an anonymous visitor");
        Assert.assertTrue(cartPage.isCheckoutLoginModalVisible(),
                "An anonymous visitor should be shown the Register / Login popup instead of the checkout page");
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Adds one product from the listing page and follows the popup through to the
     * cart. Written as a helper because six tests need a one-product cart before
     * they can assert anything, and repeating the three steps in each of them makes
     * the actual assertion harder to spot.
     */
    private CartPage addProductAndOpenCart(int productId) {
        ProductsPage productsPage = new ProductsPage(driver).open();
        productsPage.addProductToCartById(productId);
        return productsPage.viewCartFromModal();
    }

    /** Same idea, for the scenarios that need two different products in the cart. */
    private CartPage addTwoProductsAndOpenCart() {
        ProductsPage productsPage = new ProductsPage(driver).open();
        productsPage.addProductToCartById(PRODUCT_ID_A);
        // The popup must be dismissed before the second card is clickable - its
        // overlay covers the whole listing while it is open.
        productsPage.continueShopping();
        productsPage.addProductToCartById(PRODUCT_ID_B);
        return productsPage.viewCartFromModal();
    }
}
