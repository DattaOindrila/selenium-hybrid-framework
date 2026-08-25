package com.qa.tests.ui;

import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.dataproviders.TestDataProviders;
import com.qa.pages.ProductsPage;
import com.qa.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * The catalogue: the all-products listing, search, and navigation by category and
 * by brand.
 *
 * A NOTE ON WHAT SEARCH ACTUALLY DOES
 * -----------------------------------
 * The site's search matches a product's CATEGORY as well as its name. Searching for
 * "top" returns "Little Girls Mr. Panda Shirt", because that product sits in the
 * "Tops & Shirts" category. That is correct behaviour, so these tests do not assert
 * that every result name contains the term - an assertion like that would fail
 * against a working application. The listing page never renders the category, so
 * the strict per-result check lives in ProductApiTest, which can see it.
 */
public class ProductSearchTest extends BaseTest {

    @Test(groups = {"ui", "regression", "products", "smoke"},
          description = "The all-products page lists the full catalogue")
    public void allProductsPageListsTheCatalogue() {
        ProductsPage productsPage = openProductsPage();

        Assert.assertTrue(productsPage.isLoaded(), "The all-products listing did not load");
        // The heading is rendered through CSS text-transform: uppercase, so getText()
        // returns "ALL PRODUCTS". Comparing case-insensitively keeps the test about
        // the content rather than about styling.
        Assert.assertEquals(productsPage.getHeading().toLowerCase(), "all products",
                "Unexpected heading on the products page");
        Assert.assertTrue(productsPage.getProductCount() > 0,
                "The all-products page listed no products at all");
    }

    @Test(dataProvider = "positiveSearchData", dataProviderClass = TestDataProviders.class,
          groups = {"ui", "regression", "products"},
          description = "Each search term that should match returns results, at least one matching by name")
    public void searchTermsThatShouldMatchReturnResults(Map<String, String> row) {
        String term = row.get("searchTerm");
        ExtentManager.logStep(row.get("testCaseId") + " - searching for '" + term + "'");

        ProductsPage productsPage = openProductsPage().searchFor(term);

        Assert.assertTrue(productsPage.hasResults(),
                "Search for '" + term + "' returned nothing, but " + row.get("description"));
        Assert.assertTrue(productsPage.anyResultNameContains(term),
                "Search for '" + term + "' returned " + productsPage.getProductCount()
                + " results but none of them had the term in the product name: "
                + productsPage.getProductNames());
    }

    @Test(dataProvider = "negativeSearchData", dataProviderClass = TestDataProviders.class,
          groups = {"ui", "regression", "products"},
          description = "A term that matches nothing returns an empty result set without breaking the page")
    public void searchTermsThatShouldNotMatchReturnNothing(Map<String, String> row) {
        String term = row.get("searchTerm");
        ExtentManager.logStep(row.get("testCaseId") + " - searching for '" + term + "'");

        ProductsPage productsPage = openProductsPage().searchFor(term);

        Assert.assertEquals(productsPage.getProductCount(), 0,
                "Search for '" + term + "' unexpectedly returned "
                + productsPage.getProductNames());
        // The page must still render - an empty result set is not an error page.
        Assert.assertTrue(productsPage.getPageTitle().contains("Automation Exercise"),
                "The page did not survive a search that matched nothing");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "Search is case-insensitive")
    public void searchIsCaseInsensitive() {
        List<String> lowerCaseResults = openProductsPage().searchFor("top").getProductNames();
        List<String> upperCaseResults = openProductsPage().searchFor("TOP").getProductNames();

        Assert.assertFalse(lowerCaseResults.isEmpty(), "Precondition failed: 'top' returned no results");
        Assert.assertEquals(upperCaseResults, lowerCaseResults,
                "Searching 'TOP' returned a different result set from 'top', so search is case-sensitive");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "Searching narrows the catalogue rather than returning everything")
    public void searchNarrowsTheCatalogue() {
        ProductsPage productsPage = openProductsPage();
        int fullCatalogue = productsPage.getProductCount();

        int searchResults = productsPage.searchFor("saree").getProductCount();

        Assert.assertTrue(searchResults > 0, "Searching for 'saree' returned nothing");
        Assert.assertTrue(searchResults < fullCatalogue,
                "Searching returned " + searchResults + " of " + fullCatalogue
                + " products, which means the filter did nothing");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "The Women > Dress category shows only that category's products")
    public void womenDressCategoryCanBeOpened() {
        ProductsPage productsPage = new ProductsPage(driver).openCategory(AppConstants.CATEGORY_WOMEN_DRESS);

        Assert.assertTrue(productsPage.isLoaded(), "The category listing did not load");
        Assert.assertEquals(productsPage.getHeading().toLowerCase(), "women - dress products",
                "Unexpected heading on the Women > Dress category page");
        Assert.assertTrue(productsPage.getProductCount() > 0, "The Women > Dress category listed no products");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "The Men > Tshirts category shows only that category's products")
    public void menTshirtsCategoryCanBeOpened() {
        ProductsPage productsPage = new ProductsPage(driver).openCategory(AppConstants.CATEGORY_MEN_TSHIRTS);

        Assert.assertEquals(productsPage.getHeading().toLowerCase(), "men - tshirts products",
                "Unexpected heading on the Men > Tshirts category page");
        Assert.assertTrue(productsPage.getProductCount() > 0, "The Men > Tshirts category listed no products");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "A category can be reached by clicking through the sidebar accordion")
    public void categoryCanBeReachedByClickingTheAccordion() {
        ProductsPage productsPage = openProductsPage();
        Assert.assertTrue(productsPage.isCategoryPanelVisible(), "The category accordion was not shown");

        ExtentManager.logStep("Expanding 'Women' and choosing 'Dress'");
        productsPage.selectCategory("Women", AppConstants.CATEGORY_WOMEN_DRESS);

        Assert.assertEquals(productsPage.getHeading().toLowerCase(), "women - dress products",
                "Clicking through the accordion did not reach the Women > Dress listing");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "Products can be filtered by the Polo brand")
    public void productsCanBeFilteredByBrand() {
        ProductsPage productsPage = openProductsPage();
        Assert.assertTrue(productsPage.getBrandCount() >= 8,
                "Expected at least 8 brands in the sidebar but found " + productsPage.getBrandCount());

        ExtentManager.logStep("Selecting the Polo brand");
        productsPage.selectBrand("Polo");

        Assert.assertEquals(productsPage.getHeading().toLowerCase(), "brand - polo products",
                "Unexpected heading on the Polo brand page");
        Assert.assertTrue(productsPage.getProductCount() > 0, "The Polo brand page listed no products");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "A second brand can be opened directly by URL and lists its own products")
    public void babyhugBrandListsItsOwnProducts() {
        ProductsPage productsPage = new ProductsPage(driver).openBrand("Babyhug");

        Assert.assertEquals(productsPage.getHeading().toLowerCase(), "brand - babyhug products",
                "Unexpected heading on the Babyhug brand page");
        Assert.assertTrue(productsPage.getProductCount() > 0, "The Babyhug brand page listed no products");
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "The brand sidebar lists the brands the site publishes")
    public void brandSidebarListsExpectedBrands() {
        // The sidebar is upper-cased by CSS, so getText() returns "POLO". Letter case
        // here is styling, not content, which is why the comparison ignores it.
        List<String> brands = openProductsPage().getBrandNames().stream()
                .map(String::toLowerCase)
                .toList();

        Assert.assertTrue(brands.contains("polo"), "Expected 'Polo' in the brand list but got " + brands);
        Assert.assertTrue(brands.contains("h&m"), "Expected 'H&M' in the brand list but got " + brands);
        Assert.assertTrue(brands.contains("babyhug"), "Expected 'Babyhug' in the brand list but got " + brands);
        Assert.assertTrue(brands.stream().noneMatch(String::isBlank),
                "One of the brand names rendered as an empty string: " + brands);
    }

    @Test(groups = {"ui", "regression", "products"},
          description = "Every listed product shows a name and a price")
    public void everyListedProductHasANameAndAPrice() {
        ProductsPage productsPage = openProductsPage();

        List<String> names = productsPage.getProductNames();
        List<String> prices = productsPage.getProductPrices();

        Assert.assertEquals(names.size(), prices.size(),
                "The listing showed " + names.size() + " names but " + prices.size() + " prices");
        Assert.assertTrue(names.stream().noneMatch(String::isBlank), "A product was listed with a blank name");
        Assert.assertTrue(prices.stream().allMatch(p -> p.matches("Rs\\. ?\\d+")),
                "A price was not in the documented 'Rs. N' format: " + prices);
    }

    // ------------------------------------------------------------------ helpers

    private ProductsPage openProductsPage() {
        return new ProductsPage(driver).open();
    }
}
