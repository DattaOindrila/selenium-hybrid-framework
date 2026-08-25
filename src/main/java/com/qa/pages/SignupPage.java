package com.qa.pages;

import com.qa.api.model.UserAccount;
import com.qa.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * /signup - the "Enter Account Information" form reached after the signup box on
 * the login page is submitted.
 *
 * fillAccountInformation() takes the same {@link UserAccount} object the API client
 * sends to POST /api/createAccount. One model for both layers is what makes the
 * cross-validation tests trustworthy: the UI and the API are given identical data
 * by construction, so a mismatch in the assertion is a real application difference.
 */
public class SignupPage extends BasePage {

    private final By enterAccountInfoHeading = By.xpath("//h2[b[normalize-space()='Enter Account Information']]");
    private final By titleMr = By.id("id_gender1");
    private final By titleMrs = By.id("id_gender2");
    private final By nameField = By.cssSelector("input[data-qa='name']");
    private final By emailField = By.cssSelector("input[data-qa='email']");
    private final By passwordField = By.cssSelector("input[data-qa='password']");
    private final By daysDropdown = By.id("days");
    private final By monthsDropdown = By.id("months");
    private final By yearsDropdown = By.id("years");
    private final By newsletterCheckbox = By.id("newsletter");
    private final By optinCheckbox = By.id("optin");

    private final By firstNameField = By.cssSelector("input[data-qa='first_name']");
    private final By lastNameField = By.cssSelector("input[data-qa='last_name']");
    private final By companyField = By.cssSelector("input[data-qa='company']");
    private final By address1Field = By.cssSelector("input[data-qa='address']");
    private final By address2Field = By.cssSelector("input[data-qa='address2']");
    private final By countryDropdown = By.cssSelector("select[data-qa='country']");
    private final By stateField = By.cssSelector("input[data-qa='state']");
    private final By cityField = By.cssSelector("input[data-qa='city']");
    private final By zipcodeField = By.cssSelector("input[data-qa='zipcode']");
    private final By mobileNumberField = By.cssSelector("input[data-qa='mobile_number']");
    private final By createAccountButton = By.cssSelector("button[data-qa='create-account']");

    public SignupPage(WebDriver driver) {
        super(driver);
    }

    public boolean isAccountInformationFormVisible() {
        return isDisplayed(enterAccountInfoHeading);
    }

    public String getHeading() {
        return getText(enterAccountInfoHeading);
    }

    /** The name and e-mail carried over from the previous page are pre-filled. */
    public String getPrefilledName() {
        return getValue(nameField);
    }

    public String getPrefilledEmail() {
        return getValue(emailField);
    }

    public SignupPage fillAccountInformation(UserAccount user) {
        log.info("Completing account information for {}", user.getEmail());

        click("Mrs".equalsIgnoreCase(user.getTitle()) ? titleMrs : titleMr);
        type(passwordField, user.getPassword());
        selectByValue(daysDropdown, user.getBirthDay());
        selectByValue(monthsDropdown, user.getBirthMonth());
        selectByValue(yearsDropdown, user.getBirthYear());
        check(newsletterCheckbox);
        check(optinCheckbox);

        type(firstNameField, user.getFirstName());
        type(lastNameField, user.getLastName());
        type(companyField, user.getCompany());
        type(address1Field, user.getAddress1());
        type(address2Field, user.getAddress2());
        selectByVisibleText(countryDropdown, user.getCountry());
        type(stateField, user.getState());
        type(cityField, user.getCity());
        type(zipcodeField, user.getZipcode());
        type(mobileNumberField, user.getMobileNumber());
        return this;
    }

    public AccountCreatedPage clickCreateAccount() {
        click(createAccountButton);
        return new AccountCreatedPage(driver);
    }

    /** Convenience for the many tests that only need "get me a registered user". */
    public AccountCreatedPage createAccount(UserAccount user) {
        return fillAccountInformation(user).clickCreateAccount();
    }

    public boolean isNewsletterChecked() {
        return wait.waitForPresence(newsletterCheckbox).isSelected();
    }

    public boolean isOptinChecked() {
        return wait.waitForPresence(optinCheckbox).isSelected();
    }

    public int getCountryOptionCount() {
        return new org.openqa.selenium.support.ui.Select(
                wait.waitForVisibility(countryDropdown)).getOptions().size();
    }
}
