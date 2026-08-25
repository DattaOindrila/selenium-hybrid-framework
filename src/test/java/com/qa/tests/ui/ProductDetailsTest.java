package com.qa.tests.ui;

import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.pages.CartPage;
import com.qa.pages.ProductDetailsPage;
import com.qa.pages.ProductsPage;
import com.qa.utils.ExtentManager;
import com.qa.utils.TestDataFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/** The product detail view, the quantity selector and the review form. */
public class ProductDetailsTest extends BaseTest {

    /** Product 1 is "Blue Top", a Polo product in Women > Tops. Stable enough to assert on. */
    private static final int SAMPLE_PRODUCT_ID = 1;

    @Test(groups = {"ui", "regression", "products"},
          description = "The product detail page shows name, category, price, availability, condition and brand")
    public void productDetailsShowAllPublishedAttributes() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID);

        Assert.assertTrue(details.isLoaded(), "The product information block did not load");
        Assert.assertFalse(details.getProductName().isBlank(), "The product name was blank");
        Assert.assertFalse(details.getCategory().isBlank(), "The product category was blank");
        Assert.assertTrue(details.getPrice().matches("Rs\\. ?\\d+"),
                "Price was not in the documented 'Rs. N' format but was '" + details.getPrice() + "'");
        Assert.assertEquals(details.getAvailability(), "In Stock",
                "Unexpected availability for product " + SAMPLE_PRODUCT_ID);
        Assert.assertEquals(details.getCondition(), "New",
                "Unexpected condition for product " + SAMPLE_PRODUCT_ID);
        Assert.assertFalse(details.getBrand().isBlank(), "The product brand was blank");
    }

    /**
     * A consistency check rather than two independent assertions: the name and price
     * shown in the listing must be the ones shown on the detail page for the same
     * product. Asserting each page in isolation would pass even if they disagreed.
     */
    @Test(groups = {"ui", "regression", "products"},
          description = "The name and price on the detail page match what the listing showed")
    public void listingAndDetailPageAgreeOnNameAndPrice() {
        ProductsPage productsPage = new ProductsPage(driver).open();

        String nameInListing = productsPage.getProductNameByIndex(0);
        String priceInListing = productsPage.getProductPriceByIndex(0);
        ExtentManager.logStep("Listing shows '" + nameInListing + "' at " + priceInListing);

        ProductDetailsPage details = productsPage.viewProductByIndex(0);

        Assert.assertEquals(details.getProductName(), nameInListing,
                "The detail page named the product differently from the listing");
        Assert.assertEquals(details.getPrice().replace(" ", ""), priceInListing.replace(" ", ""),
                "The detail page priced the product differently from the listing");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "The quantity field defaults to 1")
    public void quantityDefaultsToOne() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID);

        Assert.assertEquals(details.getQuantityValue(), "1",
                "The quantity field did not default to 1");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "A larger quantity can be chosen before adding the product to the cart")
    public void quantityCanBeIncreasedBeforeAddingToCart() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID).setQuantity(4);

        Assert.assertEquals(details.getQuantityValue(), "4",
                "The quantity field did not accept the value 4");

        details.addToCart();
        Assert.assertTrue(details.isCartModalVisible(),
                "The 'Added!' confirmation modal did not appear after adding to the cart");

        CartPage cartPage = details.viewCart();
        Assert.assertEquals(cartPage.getQuantity(SAMPLE_PRODUCT_ID), 4,
                "The cart did not carry over the quantity chosen on the detail page");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "The review form is available under the 'Write Your Review' tab")
    public void reviewFormIsAvailable() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID).openReviewTab();

        Assert.assertTrue(details.isReviewFormVisible(),
                "The review form was not shown under the 'Write Your Review' tab");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "A review can be submitted and is acknowledged")
    public void reviewCanBeSubmitted() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID).openReviewTab();

        ExtentManager.logStep("Submitting a review");
        details.submitReview(
                TestDataFactory.randomName(),
                TestDataFactory.uniqueEmail(),
                TestDataFactory.randomSentence());

        Assert.assertTrue(details.isReviewSuccessVisible(),
                "No acknowledgement was shown after submitting a review");
        Assert.assertEquals(details.getReviewSuccessMessage(), AppConstants.REVIEW_SUCCESS_MESSAGE,
                "Unexpected acknowledgement text after submitting a review");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "A review with an empty message is not accepted")
    public void reviewWithAnEmptyMessageIsNotAccepted() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID).openReviewTab();

        // The textarea is marked required, so the browser blocks the submit and no
        // acknowledgement can appear.
        details.submitReview(TestDataFactory.randomName(), TestDataFactory.uniqueEmail(), "");

        Assert.assertFalse(details.isReviewSuccessVisible(),
                "An empty review was acknowledged as if it had been accepted");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "Adding from the detail page then continuing keeps the shopper on the product")
    public void continueShoppingReturnsToTheProduct() {
        ProductDetailsPage details = openProduct(SAMPLE_PRODUCT_ID).addToCart();
        Assert.assertTrue(details.isCartModalVisible(), "The 'Added!' modal did not appear");

        details.continueShopping();

        Assert.assertTrue(details.isLoaded(),
                "Dismissing the modal did not leave the shopper on the product detail page");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "A product id that does not exist does not render a product")
    public void nonExistentProductDoesNotRenderProductInformation() {
        // Documented behaviour, confirmed against the live site by a test that first
        // failed here: the application does NOT return a 404. It renders the page
        // shell with an EMPTY product-information block. So the honest assertion is
        // that no product data is shown - not that the block is absent, which it is
        // not, and not that an error page appears, which the site never produces.
        ProductDetailsPage details = openProduct(99999);

        Assert.assertFalse(details.hasProductDetails(),
                "Product data was rendered for a product id that does not exist");
    }

    // ------------------------------------------------------------------ helpers

    private ProductDetailsPage openProduct(int productId) {
        return new ProductDetailsPage(driver).open(productId);
    }
}
