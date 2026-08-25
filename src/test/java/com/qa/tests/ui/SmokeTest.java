package com.qa.tests.ui;

import com.qa.base.BaseTest;
import com.qa.constants.AppConstants;
import com.qa.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The two-minute proof that a clone works: the browser starts, the site answers
 * and the framework's own wiring (config, waits, reporting) is sound.
 */
public class SmokeTest extends BaseTest {

    @Test(groups = {"smoke", "ui"}, description = "Home page loads with the expected title")
    public void homePageLoadsWithCorrectTitle() {
        ExtentManager.logStep("Opening the home page");
        Assert.assertTrue(homePage.isLoaded(), "Home page slider carousel was not displayed");
        Assert.assertEquals(homePage.getPageTitle(), AppConstants.HOME_PAGE_TITLE,
                "Home page title did not match");
    }

    @Test(groups = {"smoke", "ui"}, description = "Header navigation is present and not logged in by default")
    public void headerShowsSignupLoginForAnonymousVisitor() {
        ExtentManager.logStep("Checking the header for an anonymous visitor");
        Assert.assertTrue(homePage.header().isSignupLoginVisible(),
                "'Signup / Login' link should be visible before logging in");
        Assert.assertFalse(homePage.header().isUserLoggedIn(),
                "'Logged in as' should not be shown before logging in");
    }
}
