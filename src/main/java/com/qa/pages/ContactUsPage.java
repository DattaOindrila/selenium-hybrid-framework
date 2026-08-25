package com.qa.pages;

import com.qa.base.BasePage;
import com.qa.constants.AppConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * /contact_us.
 *
 * Submitting fires a native confirm("Press OK to proceed!"). Selenium cannot touch
 * the page again until that dialog is handled, which is why submit() accepts the
 * alert rather than leaving it to the caller to remember.
 */
public class ContactUsPage extends BasePage {

    private final By getInTouchHeading = By.xpath("//div[@class='contact-form']/h2");
    private final By nameField = By.cssSelector("input[data-qa='name']");
    private final By emailField = By.cssSelector("input[data-qa='email']");
    private final By subjectField = By.cssSelector("input[data-qa='subject']");
    private final By messageField = By.cssSelector("textarea[data-qa='message']");
    private final By uploadFileField = By.cssSelector("input[name='upload_file']");
    private final By submitButton = By.cssSelector("input[data-qa='submit-button']");
    private final By successMessage = By.cssSelector(".status.alert.alert-success");
    private final By homeButton = By.cssSelector("#form-section a.btn");

    public ContactUsPage(WebDriver driver) {
        super(driver);
    }

    public ContactUsPage open() {
        openPath(AppConstants.CONTACT_US_PATH);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(getInTouchHeading);
    }

    public String getHeading() {
        return getText(getInTouchHeading);
    }

    public ContactUsPage fillForm(String name, String email, String subject, String message) {
        type(nameField, name);
        type(emailField, email);
        type(subjectField, subject);
        type(messageField, message);
        return this;
    }

    /** Attaches a real file from disk; the input is a plain file field, so sendKeys works. */
    public ContactUsPage attachFile(String absolutePath) {
        wait.waitForPresence(uploadFileField).sendKeys(absolutePath);
        return this;
    }

    /**
     * Submits and accepts the confirm dialog.
     *
     * @return the alert text, so a test can assert on the dialog itself
     */
    public String submitAndAcceptAlert() {
        click(submitButton);
        return acceptAlert();
    }

    /** Submits and dismisses instead - used by the negative test. */
    public String submitAndDismissAlert() {
        click(submitButton);
        return dismissAlert();
    }

    public boolean isSuccessMessageVisible() {
        return isDisplayed(successMessage);
    }

    public String getSuccessMessage() {
        return getText(successMessage);
    }

    public HomePage clickHome() {
        click(homeButton);
        return new HomePage(driver);
    }

    public boolean isEmailFieldRequired() {
        return "required".equals(getAttribute(emailField, "required"))
                || getAttribute(emailField, "required") != null;
    }
}
