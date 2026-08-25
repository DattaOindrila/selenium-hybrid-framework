package com.qa.base;

import com.qa.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.Locale;

/**
 * Creates and hands out WebDriver instances.
 *
 * WHY ThreadLocal
 * ---------------
 * TestNG can run test methods in parallel threads. A single static WebDriver field
 * would be shared by all of them, so thread A would navigate the browser that
 * thread B is asserting against. ThreadLocal gives each thread its own driver, so
 * the same page-object code is safe whether the suite runs single-threaded or with
 * thread-count=3.
 *
 * quit() must remove() as well as quit(): the thread is returned to TestNG's pool
 * and reused, so a stale entry left behind would be picked up by the next test.
 *
 * WHY NO WebDriverManager
 * -----------------------
 * Selenium Manager is built into Selenium 4.6+. It detects the installed browser
 * version and downloads a matching driver automatically. An extra third-party
 * dependency for that job is redundant in 2026.
 */
public final class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    /**
     * Third-party advertising hosts, resolved to 127.0.0.1 so their scripts never load.
     *
     * WHY THIS IS HERE - this was not a guess, it came from failing tests.
     * The site runs Google auto-ads. Two things they did to the product grid:
     *   1. appended advert text inside a product's name element, so the name read back
     *      as "Sleeves Printed Top - WhiteProduct Photography Service";
     *   2. wrapped a product name in <a class="google-anno">, moving the text into a
     *      child node so the name read back as empty.
     * Both are non-deterministic - they depend on what the ad network served on that
     * page load - and neither has anything to do with the application under test.
     * Blocking the ad hosts makes the runs deterministic. The name extraction in
     * ProductsPage still defends against both shapes, in case an advert slips through.
     */
    private static final String[] AD_HOSTS = {
            "*googlesyndication.com",
            "*doubleclick.net",
            "*googleadservices.com",
            "*google-analytics.com",
            "*googletagservices.com",
            "*adtrafficquality.google",
            "*ezoic.net",
            "*ezodn.com"
    };

    /** Chrome and Edge understand --host-resolver-rules. */
    private static String hostResolverRules() {
        StringBuilder rules = new StringBuilder("--host-resolver-rules=");
        for (int i = 0; i < AD_HOSTS.length; i++) {
            if (i > 0) {
                rules.append(',');
            }
            rules.append("MAP ").append(AD_HOSTS[i]).append(" 127.0.0.1");
        }
        return rules.toString();
    }

    /** Firefox has no equivalent flag, but network.dns.localDomains does the same job. */
    private static String firefoxLocalDomains() {
        return String.join(",", java.util.Arrays.stream(AD_HOSTS)
                .map(host -> host.replace("*", ""))
                .toArray(String[]::new));
    }

    public static WebDriver initDriver(String browserName, boolean headless) {
        Browser browser = Browser.from(browserName);
        log.info("Initialising {} driver (headless={}) on thread {}",
                browser, headless, Thread.currentThread().getName());

        WebDriver driver = switch (browser) {
            case CHROME -> new ChromeDriver(chromeOptions(headless));
            case FIREFOX -> new FirefoxDriver(firefoxOptions(headless));
            case EDGE -> new EdgeDriver(edgeOptions(headless));
        };

        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigReader.getInt("page.load.timeout")));

        // NOTE: no implicitlyWait() call here, on purpose. See WaitUtils.
        if (headless) {
            // Headless browsers default to a small window; several elements on the
            // target site are only reachable at desktop width.
            driver.manage().window().setSize(new Dimension(1920, 1080));
        } else {
            driver.manage().window().maximize();
        }

        DRIVER.set(driver);
        return driver;
    }

    /** @return the driver bound to the calling thread. */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No WebDriver for thread '" + Thread.currentThread().getName()
                    + "'. Did the test class extend BaseTest?");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Driver quit raised {} - continuing so the suite is not blocked", e.toString());
            } finally {
                // Must remove, not just quit: TestNG reuses pooled threads.
                DRIVER.remove();
            }
        }
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");                 // required on CI containers
        options.addArguments("--disable-dev-shm-usage");      // small /dev/shm on CI containers
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        // See AD_HOSTS: blocking the ad network is what makes the product grid
        // deterministic enough to assert on.
        options.addArguments(hostResolverRules());
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("network.dns.localDomains", firefoxLocalDomains());
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-notifications");
        options.addArguments(hostResolverRules());
        return options;
    }

    /** Typed browser list, so an unknown -Dbrowser value fails immediately and clearly. */
    public enum Browser {
        CHROME, FIREFOX, EDGE;

        public static Browser from(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Browser name was null or empty");
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unsupported browser '" + value + "'. Supported: chrome, firefox, edge.", e);
            }
        }
    }
}
