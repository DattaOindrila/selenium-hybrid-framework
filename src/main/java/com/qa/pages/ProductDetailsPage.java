package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /product_details/{id} - detail view, quantity selector and the review form. */
public class ProductDetailsPage extends BasePage {

    private final By productInformation = By.cssSelector(".product-information");
    private final By productName = By.cssSelector(".product-information h2");
    private final By productCategory = By.xpath("//div[@class='product-information']/p[1]");
    private final By productPrice = By.cssSelector(".product-information span span");
    private final By availability = By.xpath("//div[@class='product-information']//p[b[text()='Availability:']]");
    private final By condition = By.xpath("//div[@class='product-information']//p[b[text()='Condition:']]");
    private final By brand = By.xpath("//div[@class='product-information']//p[b[text()='Brand:']]");

    private final By quantityInput = By.id("quantity");
    private final By addToCartButton = By.cssSelector("button.cart");
    private final By viewCartLinkInModal = By.cssSelector("#cartModal a[href='/view_cart']");
    private final By continueShoppingButton = By.cssSelector("#cartModal .close-modal");
    private final By cartModal = By.id("cartModal");

    private final By writeReviewTab = By.cssSelector("a[href='#reviews']");
    private final By reviewName = By.id("name");
    private final By reviewEmail = By.id("email");
    private final By reviewText = By.id("review");
    private final By reviewSubmit = By.id("button-review");
    private final By reviewSuccess = By.cssSelector("#review-section .alert-success span");

    public ProductDetailsPage(WebDriver driver) {
        super(driver);
    }

    public ProductDetailsPage open(int productId) {
        openPath(AppConstants.PRODUCT_DETAILS_PATH + productId);
        // Wait for the page's own container before anything else touches it. Without
        // this, a slow response surfaces later as "could not find #quantity", which
        // points at the wrong thing entirely.
        wait.waitForPresence(productInformation);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(productInformation);
    }

    /**
     * Whether the page is actually showing a product.
     *
     * Verified against the live site: an id that does not exist does NOT produce a
     * 404. The application renders the page shell with an EMPTY product-information
     * block, so isLoaded() is true even though there is no product. This method is
     * the honest check, and the two are deliberately kept separate.
     */
    public boolean hasProductDetails() {
        return isPresent(productName) && !ownText(wait.waitForPresence(productName)).isBlank();
    }

    public String getProductName() {
        return getText(productName);
    }

    /** "Category: Women > Tops" -> "Women > Tops" */
    public String getCategory() {
        return getText(productCategory).replace("Category:", "").trim();
    }

    public String getPrice() {
        return getText(productPrice);
    }

    public int getPriceValue() {
        return Integer.parseInt(getPrice().replaceAll("[^0-9]", ""));
    }

    public String getAvailability() {
        return getText(availability).replace("Availability:", "").trim();
    }

    public String getCondition() {
        return getText(condition).replace("Condition:", "").trim();
    }

    public String getBrand() {
        return getText(brand).replace("Brand:", "").trim();
    }

    // ---------------------------------------------------------------- quantity

    /**
     * This is the only place on the site where quantity can be chosen. The cart
     * renders its quantity as a disabled button, so "update the quantity" always
     * means "set it here before adding".
     */
    public ProductDetailsPage setQuantity(int quantity) {
        type(quantityInput, String.valueOf(quantity));
        return this;
    }

    public String getQuantityValue() {
        return getValue(quantityInput);
    }

    public ProductDetailsPage addToCart() {
        click(addToCartButton);
        return this;
    }

    public boolean isCartModalVisible() {
        return isDisplayed(cartModal);
    }

    public CartPage viewCart() {
        click(viewCartLinkInModal);
        return new CartPage(driver);
    }

    public ProductDetailsPage continueShopping() {
        click(continueShoppingButton);
        wait.waitForInvisibility(cartModal);
        return this;
    }

    // ------------------------------------------------------------------ review

    public ProductDetailsPage openReviewTab() {
        click(writeReviewTab);
        return this;
    }

    public ProductDetailsPage submitReview(String name, String email, String review) {
        log.info("Submitting a review as {}", email);
        type(reviewName, name);
        type(reviewEmail, email);
        type(reviewText, review);
        click(reviewSubmit);
        return this;
    }

    public boolean isReviewSuccessVisible() {
        return isDisplayed(reviewSuccess);
    }

    public String getReviewSuccessMessage() {
        return getText(reviewSuccess);
    }

    public boolean isReviewFormVisible() {
        return isDisplayed(reviewText);
    }
}
