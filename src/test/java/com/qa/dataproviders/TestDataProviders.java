package com.qa.dataproviders;

import com.qa.constants.AppConstants;
import com.qa.utils.ExcelReader;
import org.testng.annotations.DataProvider;

import java.util.Map;

/**
 * Every @DataProvider in the suite, in one place.
 *
 * Each provider hands the test a Map of column-name -> value read from
 * testdata.xlsx, so a test reads row.get("password") rather than row[2]. Adding a
 * column to the spreadsheet then cannot silently break an unrelated test.
 *
 * Providers are declared here rather than on the test classes so the same data set
 * can feed more than one test - the Login sheet drives both the UI login tests and
 * the API verifyLogin tests.
 */
public class TestDataProviders {

    @DataProvider(name = "loginData")
    public Object[][] loginData() {
        return ExcelReader.asDataProvider(AppConstants.SHEET_LOGIN);
    }

    @DataProvider(name = "registrationData")
    public Object[][] registrationData() {
        return ExcelReader.asDataProvider(AppConstants.SHEET_REGISTRATION);
    }

    @DataProvider(name = "searchData")
    public Object[][] searchData() {
        return ExcelReader.asDataProvider(AppConstants.SHEET_SEARCH);
    }

    @DataProvider(name = "contactUsData")
    public Object[][] contactUsData() {
        return ExcelReader.asDataProvider(AppConstants.SHEET_CONTACT_US);
    }

    /**
     * Only the rows whose search term is expected to match something.
     * Filtering here keeps the "no results" rows out of the tests that assert on
     * result contents, without needing a second sheet.
     */
    @DataProvider(name = "positiveSearchData")
    public Object[][] positiveSearchData() {
        return filterByFlag(AppConstants.SHEET_SEARCH, "expectMatches", "true");
    }

    @DataProvider(name = "negativeSearchData")
    public Object[][] negativeSearchData() {
        return filterByFlag(AppConstants.SHEET_SEARCH, "expectMatches", "false");
    }

    @SuppressWarnings("unchecked")
    private Object[][] filterByFlag(String sheet, String column, String value) {
        Object[][] all = ExcelReader.asDataProvider(sheet);
        return java.util.Arrays.stream(all)
                .filter(row -> value.equalsIgnoreCase(((Map<String, String>) row[0]).get(column)))
                .toArray(Object[][]::new);
    }

    /** Browsers to exercise in the cross-browser test. Not from Excel - it is configuration, not data. */
    @DataProvider(name = "browsers")
    public Object[][] browsers() {
        return new Object[][]{{"chrome"}, {"firefox"}, {"edge"}};
    }
}
