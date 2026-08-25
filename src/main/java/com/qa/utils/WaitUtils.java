package com.qa.utils;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Every wait in this framework goes through here.
 *
 * Design rule: EXPLICIT WAITS ONLY. There is no Thread.sleep() anywhere in the
 * repository and no implicit wait is ever set. Mixing implicit and explicit waits
 * is a classic source of unpredictable timeouts, because the implicit wait keeps
 * polling inside the explicit wait's own polling loop.
 */
public final class WaitUtils {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Duration timeout;
    private final Duration pollInterval;

    public WaitUtils(WebDriver driver, Duration timeout, Duration pollInterval) {
        this.driver = driver;
        this.timeout = timeout;
        this.pollInterval = pollInterval;
        this.wait = new WebDriverWait(driver, timeout, pollInterval);
    }

    public WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisibility(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public boolean waitForInvisibility(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForTitle(String title) {
        return wait.until(ExpectedConditions.titleIs(title));
    }

    public boolean waitForUrlContains(String fragment) {
        return wait.until(ExpectedConditions.urlContains(fragment));
    }

    public boolean waitForTextInElement(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public Alert waitForAlert() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    /**
     * Used where the page rewrites a node while we are reading it - the cart table
     * re-renders after a delete, for example. FluentWait lets us ignore the
     * StaleElementReferenceException and retry rather than fail the test.
     */
    public WebElement waitIgnoringStaleness(By locator) {
        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(pollInterval)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class);
        return fluentWait.until(d -> {
            WebElement element = d.findElement(locator);
            return element.isDisplayed() ? element : null;
        });
    }

    /**
     * Non-throwing presence check. Returns false on timeout instead of raising,
     * which is what negative tests want ("assert the error banner is NOT shown").
     */
    public boolean isVisibleWithin(By locator, Duration shortTimeout) {
        try {
            new WebDriverWait(driver, shortTimeout, pollInterval)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }
}
