package com.qa.base;

import com.qa.pages.HomePage;
import com.qa.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.lang.reflect.Method;

/**
 * Parent of every UI test class.
 *
 * One fresh browser per test method. That is slower than sharing a session, and it
 * is the right trade: a test that inherits cookies, a half-filled cart or a
 * logged-in session from whatever ran before it is not an independent test, and
 * the failure it eventually produces is unreproducible in isolation.
 *
 * The browser comes from, in order: the TestNG suite parameter, then -Dbrowser,
 * then config.properties.
 */
public abstract class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);

    protected WebDriver driver;
    protected HomePage homePage;

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser", "headless"})
    public void setUp(@Optional String browserParam,
                      @Optional String headlessParam,
                      Method method) {

        String browser = firstNonBlank(System.getProperty("browser"), browserParam, ConfigReader.get("browser"));
        boolean headless = Boolean.parseBoolean(
                firstNonBlank(System.getProperty("headless"), headlessParam, ConfigReader.get("headless")));

        log.info("===== START {}.{} [{}, headless={}] =====",
                method.getDeclaringClass().getSimpleName(), method.getName(), browser, headless);

        driver = DriverFactory.initDriver(browser, headless);
        homePage = new HomePage(driver);
        homePage.open();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Method method) {
        DriverFactory.quitDriver();
        log.info("===== END   {}.{} =====",
                method.getDeclaringClass().getSimpleName(), method.getName());
    }

    /**
     * A TestNG @Parameters value wins over config.properties, but a -D system
     * property wins over both, so one suite file can still be pointed at a
     * different browser from the command line.
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !value.startsWith("${")) {
                return value;
            }
        }
        throw new IllegalStateException("No value supplied for a required setup parameter");
    }
}
