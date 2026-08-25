package com.qa.base;

import com.qa.pages.components.FooterComponent;
import com.qa.pages.components.HeaderComponent;
import com.qa.utils.ConfigReader;
import com.qa.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;

/**
 * Parent of every page object.
 *
 * It owns the driver reference and the wait helper, and provides the small set of
 * interactions the page objects need. Keeping click/type/read here means a page
 * object contains locators and business methods only - which is the whole point of
 * the Page Object Model: when the site's markup changes, exactly one file changes.
 */
public abstract class BasePage {

    protected static final Logger log = LogManager.getLogger(BasePage.class);

    protected final WebDriver driver;
    protected final WaitUtils wait;
    protected final String baseUrl;

    // The header and the subscription footer appear on every page. Modelling them
    // as components rather than copying their locators into ten page classes is the
    // "reusable components" half of the framework: the logout link is declared once.
    // Created lazily - eager creation here would recurse, because the components
    // themselves extend BasePage.
    private HeaderComponent header;
    private FooterComponent footer;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.baseUrl = ConfigReader.get("base.url");
        this.wait = new WaitUtils(
                driver,
                Duration.ofSeconds(ConfigReader.getInt("explicit.wait")),
                Duration.ofMillis(ConfigReader.getInt("poll.interval")));
    }

    /** Header navigation shared by every page. */
    public HeaderComponent header() {
        if (header == null) {
            header = new HeaderComponent(driver);
        }
        return header;
    }

    /** Subscription block in the footer, shared by every page. */
    public FooterComponent footer() {
        if (footer == null) {
            footer = new FooterComponent(driver);
        }
        return footer;
    }

    // ---------------------------------------------------------------- navigation

    protected void openPath(String relativePath) {
        String url = baseUrl + relativePath;
        log.info("Navigating to {}", url);
        driver.get(url);
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // ------------------------------------------------------------- interactions

    protected void click(By locator) {
        WebElement element = wait.waitForClickable(locator);
        scrollIntoView(element);
        try {
            element.click();
        } catch (org.openqa.selenium.ElementClickInterceptedException e) {
            // The target site injects ad iframes that can cover a button for a moment.
            // Falling back to a JS click keeps a real functional failure distinguishable
            // from an advert landing on top of the element.
            log.warn("Native click on {} was intercepted; retrying via JavaScript", locator);
            jsClick(element);
        }
    }

    protected void click(WebElement element) {
        WebElement clickable = wait.waitForClickable(element);
        scrollIntoView(clickable);
        try {
            clickable.click();
        } catch (org.openqa.selenium.ElementClickInterceptedException e) {
            log.warn("Native click was intercepted; retrying via JavaScript");
            jsClick(clickable);
        }
    }

    protected void type(By locator, String text) {
        WebElement element = wait.waitForVisibility(locator);
        scrollIntoView(element);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return wait.waitForVisibility(locator).getText().trim();
    }

    /**
     * The element's text with every run of whitespace collapsed to a single space.
     *
     * The application renders some headings from a template that leaves a stray
     * double space, for example "Men -  Tshirts Products". Asserting on the exact
     * spacing would make a test fail over invisible formatting rather than over
     * content, so anything compared against an expected string goes through here.
     */
    protected String getNormalisedText(By locator) {
        return normaliseSpaces(wait.waitForVisibility(locator).getText());
    }

    protected static String normaliseSpaces(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim();
    }

    /**
     * Only the element's OWN text, excluding anything contributed by child elements.
     *
     * The target site serves third-party advertisements, and one of them is
     * intermittently injected as a child node INSIDE a product's name element. When
     * that happens getText() returns "Sleeves Printed Top - WhiteProduct Photography
     * Service" - the product name with an advert glued to the end of it. Reading only
     * the direct text nodes returns the real product name whatever the ad network
     * decided to inject on this particular page load.
     *
     * This was found by a failing test, not predicted: see the README's Known
     * Limitations section.
     */
    /**
     * The product name from a card's name paragraph, resilient to both advert shapes
     * that were observed corrupting it on the live site:
     *
     *   <p>Blue Top<ins>Some Advert Text</ins></p>   -> advert appended as a sibling node
     *   <p><a class="google-anno">Men Tshirt</a></p> -> name moved inside an advert anchor
     *
     * The ad hosts are blocked in DriverFactory so neither shape should normally
     * occur. This is the second line of defence, because an ad network changing what
     * it serves must not turn into a mysterious assertion failure at 2am.
     */
    protected String productNameText(WebElement paragraph) {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "const p = arguments[0];"
                + "const wrapped = p.querySelector('a.google-anno');"
                + "if (wrapped) { return wrapped.textContent.trim(); }"
                + "return Array.from(p.childNodes)"
                + "  .filter(n => n.nodeType === Node.TEXT_NODE)"
                + "  .map(n => n.nodeValue).join('').trim();", paragraph);
        return result == null ? "" : result.toString().trim();
    }

    protected String ownText(WebElement element) {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "return Array.from(arguments[0].childNodes)"
                + ".filter(n => n.nodeType === Node.TEXT_NODE)"
                + ".map(n => n.nodeValue).join('').trim();", element);
        return result == null ? "" : result.toString();
    }

    protected String getAttribute(By locator, String attribute) {
        return wait.waitForPresence(locator).getDomAttribute(attribute);
    }

    protected String getValue(By locator) {
        return wait.waitForPresence(locator).getDomProperty("value");
    }

    protected boolean isDisplayed(By locator) {
        return wait.isVisibleWithin(locator, Duration.ofSeconds(ConfigReader.getInt("short.wait")));
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected int countOf(By locator) {
        return driver.findElements(locator).size();
    }

    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    protected void selectByVisibleText(By locator, String text) {
        new Select(wait.waitForVisibility(locator)).selectByVisibleText(text);
    }

    protected void selectByValue(By locator, String value) {
        new Select(wait.waitForVisibility(locator)).selectByValue(value);
    }

    protected void check(By locator) {
        WebElement box = wait.waitForClickable(locator);
        if (!box.isSelected()) {
            scrollIntoView(box);
            box.click();
        }
    }

    // -------------------------------------------------------------- JS helpers

    protected void scrollIntoView(WebElement element) {
        // "center" rather than the default "start": the site has a fixed header that
        // would otherwise sit on top of the element we just scrolled to.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", element);
    }

    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    protected void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    // ------------------------------------------------------------------ alerts

    /**
     * The contact form fires a native confirm() dialog. Selenium cannot see the page
     * again until that dialog is dismissed, so this is not optional politeness.
     */
    protected String acceptAlert() {
        Alert alert = wait.waitForAlert();
        String text = alert.getText();
        log.info("Accepting browser alert: '{}'", text);
        alert.accept();
        return text;
    }

    protected String dismissAlert() {
        Alert alert = wait.waitForAlert();
        String text = alert.getText();
        alert.dismiss();
        return text;
    }
}
