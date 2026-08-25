package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/** The landing page: featured products, the category accordion and the brand list. */
public class HomePage extends BasePage {

    private final By sliderCarousel = By.id("slider-carousel");
    private final By featuredItemsHeading = By.cssSelector(".features_items h2.title");
    private final By productCards = By.cssSelector(".features_items .product-image-wrapper");
    private final By recommendedItemsHeading = By.cssSelector(".recommended_items h2.title");
    private final By recommendedAddToCart = By.cssSelector(".recommended_items .add-to-cart");
    private final By scrollUpArrow = By.id("scrollUp");
    private final By categoryPanel = By.id("accordian");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        openPath("/");
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(sliderCarousel);
    }

    public boolean isFeaturedItemsVisible() {
        return isDisplayed(featuredItemsHeading);
    }

    public int getProductCardCount() {
        return countOf(productCards);
    }

    public List<String> getProductNames() {
        return findAll(productCards).stream()
                .map(card -> productNameText(card.findElement(By.cssSelector(".productinfo > p"))))
                .collect(Collectors.toList());
    }

    public boolean isCategoryPanelVisible() {
        return isDisplayed(categoryPanel);
    }

    public boolean isRecommendedItemsVisible() {
        scrollToBottom();
        return isDisplayed(recommendedItemsHeading);
    }

    /**
     * The recommended carousel renders three copies of the same product for its
     * slider, so this clicks the first *visible* one rather than the first in the DOM.
     */
    public void addFirstRecommendedItemToCart() {
        scrollToBottom();
        List<WebElement> buttons = findAll(recommendedAddToCart);
        for (WebElement button : buttons) {
            if (button.isDisplayed()) {
                click(button);
                return;
            }
        }
        throw new IllegalStateException("No visible 'Add to cart' button in the recommended items carousel");
    }

    public boolean isScrollUpArrowVisible() {
        scrollToBottom();
        return isDisplayed(scrollUpArrow);
    }

    public void clickScrollUpArrow() {
        click(scrollUpArrow);
    }

    /** True once the page is scrolled back to the very top. */
    public boolean isAtTop() {
        Long offset = (Long) ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return Math.round(window.pageYOffset);");
        return offset != null && offset <= 5;
    }

    public String getExpectedTitle() {
        return AppConstants.HOME_PAGE_TITLE;
    }
}
