package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /products, and the category and brand listings, which reuse the same markup.
 *
 * One page object covers all three because they are the same page rendered with a
 * different filter - a second class would duplicate every locator to gain nothing.
 */
public class ProductsPage extends BasePage {

    private final By allProductsHeading = By.cssSelector(".features_items h2.title");
    private final By searchInput = By.id("search_product");
    private final By searchButton = By.id("submit_search");
    private final By productCards = By.cssSelector(".features_items .product-image-wrapper");
    private final By productNames = By.cssSelector(".features_items .productinfo p");
    private final By productPrices = By.cssSelector(".features_items .productinfo h2");
    private final By brandLinks = By.cssSelector(".brands_products ul li a");
    private final By categoryAccordion = By.id("accordian");

    private final By cartModal = By.id("cartModal");
    private final By cartModalTitle = By.cssSelector("#cartModal .modal-title");
    private final By continueShoppingButton = By.cssSelector("#cartModal .close-modal");
    private final By viewCartLinkInModal = By.cssSelector("#cartModal a[href='/view_cart']");

    public ProductsPage(WebDriver driver) {
        super(driver);
    }

    public ProductsPage open() {
        openPath(AppConstants.PRODUCTS_PATH);
        return this;
    }

    public ProductsPage openCategory(int categoryId) {
        openPath(AppConstants.CATEGORY_PRODUCTS_PATH + categoryId);
        return this;
    }

    public ProductsPage openBrand(String brandName) {
        openPath(AppConstants.BRAND_PRODUCTS_PATH + brandName);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(allProductsHeading);
    }

    /** Whitespace-normalised: the site's own template emits "Men -  Tshirts Products". */
    public String getHeading() {
        return getNormalisedText(allProductsHeading);
    }

    // ------------------------------------------------------------------ search

    public ProductsPage searchFor(String term) {
        log.info("Searching products for '{}'", term);
        type(searchInput, term);
        click(searchButton);
        return this;
    }

    public int getProductCount() {
        return countOf(productCards);
    }

    /**
     * Reads one name per product card rather than with a flat page-wide selector.
     *
     * Two separate defences against the same problem - the site's third-party ads
     * are injected into the product grid at unpredictable moments:
     *   - scoping to ".productinfo > p" ignores any paragraph an advert adds deeper
     *     in the tree, and findElement takes the FIRST direct child, which is the name;
     *   - ownText ignores any advert injected as a child OF the name element itself.
     * A flat "all p under .features_items" selector picked up advert paragraphs and
     * produced blank and corrupted names. Both problems were found by failing tests.
     */
    public List<String> getProductNames() {
        return findAll(productCards).stream()
                .map(card -> productNameText(card.findElement(By.cssSelector(".productinfo > p"))))
                .collect(Collectors.toList());
    }

    /** Scoped per card for the same reason as getProductNames. */
    public List<String> getProductPrices() {
        return findAll(productCards).stream()
                .map(card -> ownText(card.findElement(By.cssSelector(".productinfo > h2"))))
                .collect(Collectors.toList());
    }

    public boolean hasResults() {
        return getProductCount() > 0;
    }

    /**
     * How many results have the search term in their NAME.
     *
     * Deliberately not "do all results contain the term". The site's search also
     * matches the product's category, so a search for "top" legitimately returns
     * "Little Girls Mr. Panda Shirt", whose category is "Tops & Shirts". A test that
     * asserted every name contains the term would fail against correct behaviour.
     * The listing page does not render the category, so the strict check belongs in
     * the API tests, which can see it - and in the cross-validation test, which
     * compares the two layers against each other.
     */
    public long countResultsWithNameContaining(String term) {
        return getProductNames().stream()
                .filter(name -> name.toLowerCase().contains(term.toLowerCase()))
                .count();
    }

    /** True when at least one result matched on its name. */
    public boolean anyResultNameContains(String term) {
        return countResultsWithNameContaining(term) > 0;
    }

    // ------------------------------------------------------------- cart actions

    /**
     * Hovers nothing and clicks the overlay button directly: the card's visible
     * "Add to cart" and the hover-overlay copy are two separate anchors with the
     * same class, and only the first is reachable without a real mouse hover.
     */
    public ProductsPage addProductToCartByIndex(int index) {
        List<WebElement> cards = findAll(productCards);
        if (index >= cards.size()) {
            throw new IllegalArgumentException(
                    "Asked for product " + index + " but only " + cards.size() + " are listed");
        }
        WebElement button = cards.get(index).findElement(By.cssSelector(".productinfo a.add-to-cart"));
        click(button);
        return this;
    }

    public ProductsPage addProductToCartById(int productId) {
        click(By.cssSelector(".productinfo a.add-to-cart[data-product-id='" + productId + "']"));
        return this;
    }

    public String getProductNameByIndex(int index) {
        return productNameText(findAll(productCards).get(index)
                .findElement(By.cssSelector(".productinfo > p")));
    }

    public String getProductPriceByIndex(int index) {
        return ownText(findAll(productCards).get(index)
                .findElement(By.cssSelector(".productinfo > h2")));
    }

    public ProductDetailsPage viewProductByIndex(int index) {
        WebElement link = findAll(productCards).get(index)
                .findElement(By.cssSelector("a[href*='/product_details/']"));
        click(link);
        return new ProductDetailsPage(driver);
    }

    // ------------------------------------------------------------- "Added!" modal

    public boolean isCartModalVisible() {
        return isDisplayed(cartModal);
    }

    public String getCartModalTitle() {
        return getText(cartModalTitle);
    }

    public ProductsPage continueShopping() {
        click(continueShoppingButton);
        // The modal fades out; waiting for it to go keeps the next click from
        // landing on the overlay instead of the page.
        wait.waitForInvisibility(cartModal);
        return this;
    }

    public CartPage viewCartFromModal() {
        click(viewCartLinkInModal);
        return new CartPage(driver);
    }

    // ------------------------------------------------------- categories & brands

    public boolean isCategoryPanelVisible() {
        return isDisplayed(categoryAccordion);
    }

    public void expandCategory(String usertype) {
        click(By.cssSelector("a[href='#" + usertype + "']"));
    }

    public void selectCategory(String usertype, int categoryId) {
        expandCategory(usertype);
        click(By.cssSelector("#" + usertype + " a[href='/category_products/" + categoryId + "']"));
    }

    public int getBrandCount() {
        return countOf(brandLinks);
    }

    /**
     * Rendered as "(6)Polo", and upper-cased by CSS, so getText() yields "(6)POLO".
     * The count prefix is stripped here; letter case is left alone and the tests
     * compare case-insensitively, because the case is styling rather than content.
     */
    public List<String> getBrandNames() {
        return findAll(brandLinks).stream()
                .map(e -> e.getText().replaceAll("^\\(\\d+\\)", "").trim())
                .collect(Collectors.toList());
    }

    public void selectBrand(String brandName) {
        click(By.cssSelector(".brands_products a[href='/brand_products/" + brandName + "']"));
    }
}
