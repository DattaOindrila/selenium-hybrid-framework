package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /view_cart.
 *
 * Rows carry id="product-{productId}", so every read is scoped to a specific
 * product rather than to a row index that shifts when something is deleted.
 */
public class CartPage extends BasePage {

    private final By cartTable = By.id("cart_info");
    private final By cartRows = By.cssSelector("#cart_info tbody tr");
    private final By emptyCartMessage = By.id("empty_cart");
    private final By proceedToCheckoutButton = By.cssSelector("a.check_out");
    private final By registerLoginLinkInModal = By.cssSelector("#checkoutModal a[href='/login']");
    private final By checkoutModal = By.id("checkoutModal");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage open() {
        openPath(AppConstants.CART_PATH);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(cartTable) || isDisplayed(emptyCartMessage);
    }

    public boolean isEmpty() {
        return isDisplayed(emptyCartMessage);
    }

    public String getEmptyCartMessage() {
        return getText(emptyCartMessage);
    }

    public int getItemCount() {
        // The empty state renders no rows at all.
        return isPresent(cartTable) ? countOf(cartRows) : 0;
    }

    public boolean containsProduct(int productId) {
        return isPresent(By.id("product-" + productId));
    }

    public List<String> getProductNames() {
        return findAll(By.cssSelector("#cart_info tbody tr td.cart_description h4 a")).stream()
                .map(e -> e.getText().trim())
                .collect(Collectors.toList());
    }

    public String getProductName(int productId) {
        return getText(By.cssSelector("#product-" + productId + " td.cart_description h4 a"));
    }

    public String getUnitPrice(int productId) {
        return getText(By.cssSelector("#product-" + productId + " td.cart_price p"));
    }

    public int getUnitPriceValue(int productId) {
        return Integer.parseInt(getUnitPrice(productId).replaceAll("[^0-9]", ""));
    }

    public int getQuantity(int productId) {
        return Integer.parseInt(getText(By.cssSelector("#product-" + productId + " td.cart_quantity button")).trim());
    }

    public String getLineTotal(int productId) {
        return getText(By.cssSelector("#product-" + productId + " td.cart_total p.cart_total_price"));
    }

    public int getLineTotalValue(int productId) {
        return Integer.parseInt(getLineTotal(productId).replaceAll("[^0-9]", ""));
    }

    /** unit price x quantity, as the cart itself should compute it. */
    public boolean isLineTotalCorrect(int productId) {
        return getLineTotalValue(productId) == getUnitPriceValue(productId) * getQuantity(productId);
    }

    public int getCartTotalValue() {
        return findAll(cartRows).stream()
                .mapToInt(row -> {
                    List<WebElement> totals = row.findElements(By.cssSelector("p.cart_total_price"));
                    return totals.isEmpty() ? 0
                            : Integer.parseInt(totals.get(0).getText().replaceAll("[^0-9]", ""));
                })
                .sum();
    }

    public CartPage removeProduct(int productId) {
        log.info("Removing product {} from the cart", productId);
        int before = getItemCount();
        click(By.cssSelector("a.cart_quantity_delete[data-product-id='" + productId + "']"));
        // The table re-renders through AJAX, so wait for the row count to actually
        // drop rather than asserting against the pre-delete DOM.
        wait.waitForInvisibility(By.id("product-" + productId));
        log.info("Cart went from {} to {} item(s)", before, getItemCount());
        return this;
    }

    // --------------------------------------------------------------- checkout

    public CheckoutPage proceedToCheckout() {
        click(proceedToCheckoutButton);
        return new CheckoutPage(driver);
    }

    /**
     * An anonymous visitor is shown a "Register / Login" modal instead of the
     * checkout page. Tests use this to assert that checkout requires an account.
     */
    public boolean isCheckoutLoginModalVisible() {
        click(proceedToCheckoutButton);
        return isDisplayed(checkoutModal);
    }

    public LoginPage registerLoginFromModal() {
        click(registerLoginLinkInModal);
        return new LoginPage(driver);
    }
}
