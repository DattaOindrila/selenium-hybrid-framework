package com.qa.constants;

/**
 * Single home for values that are referenced from more than one place and never
 * change between environments.
 *
 * Anything that DOES change between environments (URLs, browser, timeouts) lives
 * in config.properties instead - see {@link com.qa.utils.ConfigReader}.
 */
public final class AppConstants {

    private AppConstants() {
        // utility class - never instantiated
    }

    // ---------- Page titles ----------
    public static final String HOME_PAGE_TITLE = "Automation Exercise";
    public static final String LOGIN_PAGE_TITLE = "Automation Exercise - Signup / Login";
    public static final String SIGNUP_PAGE_TITLE = "Automation Exercise - Signup";
    public static final String PRODUCTS_PAGE_TITLE = "Automation Exercise - All Products";
    public static final String PRODUCT_DETAILS_PAGE_TITLE = "Automation Exercise - Product Details";
    public static final String CART_PAGE_TITLE = "Automation Exercise - Checkout";
    public static final String CONTACT_US_PAGE_TITLE = "Automation Exercise - Contact Us";
    public static final String PAYMENT_PAGE_TITLE = "Automation Exercise - Payment";
    public static final String ORDER_PLACED_PAGE_TITLE = "Automation Exercise - Order Placed";
    public static final String ACCOUNT_CREATED_PAGE_TITLE = "Automation Exercise - Account Created";

    // ---------- Relative paths ----------
    public static final String LOGIN_PATH = "/login";
    public static final String SIGNUP_PATH = "/signup";
    public static final String PRODUCTS_PATH = "/products";
    public static final String CART_PATH = "/view_cart";
    public static final String CHECKOUT_PATH = "/checkout";
    public static final String PAYMENT_PATH = "/payment";
    public static final String CONTACT_US_PATH = "/contact_us";
    public static final String DELETE_ACCOUNT_PATH = "/delete_account";
    public static final String LOGOUT_PATH = "/logout";
    public static final String PRODUCT_DETAILS_PATH = "/product_details/";
    public static final String CATEGORY_PRODUCTS_PATH = "/category_products/";
    public static final String BRAND_PRODUCTS_PATH = "/brand_products/";

    // ---------- Expected UI messages ----------
    public static final String LOGIN_ERROR_MESSAGE = "Your email or password is incorrect!";
    public static final String SIGNUP_DUPLICATE_EMAIL_MESSAGE = "Email Address already exist!";
    public static final String ACCOUNT_CREATED_MESSAGE = "ACCOUNT CREATED!";
    public static final String ACCOUNT_DELETED_MESSAGE = "ACCOUNT DELETED!";
    public static final String ORDER_PLACED_MESSAGE = "ORDER PLACED!";
    public static final String ORDER_CONFIRMED_MESSAGE = "Congratulations! Your order has been confirmed!";
    public static final String CART_EMPTY_MESSAGE = "Cart is empty!";
    public static final String SUBSCRIPTION_SUCCESS_MESSAGE = "You have been successfully subscribed!";
    public static final String CONTACT_US_SUCCESS_MESSAGE = "Success! Your details have been submitted successfully.";
    public static final String REVIEW_SUCCESS_MESSAGE = "Thank you for your review.";
    public static final String CONTACT_US_ALERT_TEXT = "Press OK to proceed!";

    // ---------- API endpoints (paths only; the base URI is config-driven) ----------
    public static final String API_PRODUCTS_LIST = "/api/productsList";
    public static final String API_BRANDS_LIST = "/api/brandsList";
    public static final String API_SEARCH_PRODUCT = "/api/searchProduct";
    public static final String API_VERIFY_LOGIN = "/api/verifyLogin";
    public static final String API_CREATE_ACCOUNT = "/api/createAccount";
    public static final String API_DELETE_ACCOUNT = "/api/deleteAccount";
    public static final String API_UPDATE_ACCOUNT = "/api/updateAccount";
    public static final String API_USER_DETAIL_BY_EMAIL = "/api/getUserDetailByEmail";

    /**
     * The target API answers HTTP 200 for every request - including errors - and
     * puts the real status in a "responseCode" field inside the JSON body.
     * These constants make that indirection explicit in the assertions.
     */
    public static final String RESPONSE_CODE_FIELD = "responseCode";
    public static final int API_OK = 200;
    public static final int API_CREATED = 201;
    public static final int API_BAD_REQUEST = 400;
    public static final int API_NOT_FOUND = 404;
    public static final int API_METHOD_NOT_ALLOWED = 405;

    public static final String API_MSG_USER_EXISTS = "User exists!";
    public static final String API_MSG_USER_NOT_FOUND = "User not found!";
    public static final String API_MSG_USER_CREATED = "User created!";
    public static final String API_MSG_USER_UPDATED = "User updated!";
    public static final String API_MSG_ACCOUNT_DELETED = "Account deleted!";
    public static final String API_MSG_METHOD_NOT_SUPPORTED = "This request method is not supported.";
    public static final String API_MSG_MISSING_SEARCH_PARAM =
            "Bad request, search_product parameter is missing in POST request.";
    public static final String API_MSG_MISSING_LOGIN_PARAM =
            "Bad request, email or password parameter is missing in POST request.";

    // ---------- Excel sheet names ----------
    public static final String SHEET_LOGIN = "Login";
    public static final String SHEET_REGISTRATION = "Registration";
    public static final String SHEET_SEARCH = "Search";
    public static final String SHEET_CONTACT_US = "ContactUs";

    // ---------- Category ids, as published by the site ----------
    public static final int CATEGORY_WOMEN_DRESS = 1;
    public static final int CATEGORY_WOMEN_TOPS = 2;
    public static final int CATEGORY_MEN_TSHIRTS = 3;
    public static final int CATEGORY_MEN_JEANS = 6;
    public static final int CATEGORY_WOMEN_SAREE = 7;
}
