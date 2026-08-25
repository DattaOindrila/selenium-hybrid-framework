package com.qa.utils;

import com.qa.api.model.UserAccount;
import net.datafaker.Faker;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds unique, throwaway user accounts.
 *
 * WHY generate instead of using one fixed test account:
 * registration is one of the flows under test, and the site rejects a duplicate
 * e-mail address. A hardcoded account would pass on the first run and fail on
 * every run after it. Each generated address carries a timestamp and a counter, so
 * two tests in the same millisecond on different threads still get different
 * addresses.
 *
 * Accounts created by the suite are deleted again in teardown, so the practice
 * site is not slowly filled with abandoned records.
 */
public final class TestDataFactory {

    private static final Faker FAKER = new Faker(new Locale("en", "IN"));
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestDataFactory() {
    }

    /** e.g. qa.auto.1787585894123.7@mailinator.com */
    public static String uniqueEmail() {
        return String.format("qa.auto.%d.%d@mailinator.com",
                System.currentTimeMillis(), COUNTER.incrementAndGet());
    }

    public static UserAccount newUser() {
        String first = FAKER.name().firstName().replaceAll("[^A-Za-z]", "");
        String last = FAKER.name().lastName().replaceAll("[^A-Za-z]", "");
        return UserAccount.builder()
                .name(first)
                .email(uniqueEmail())
                .password("Qa@" + FAKER.number().numberBetween(100000, 999999))
                .title("Mr")
                .birthDay(String.valueOf(FAKER.number().numberBetween(1, 28)))
                .birthMonth(String.valueOf(FAKER.number().numberBetween(1, 12)))
                .birthYear(String.valueOf(FAKER.number().numberBetween(1970, 2003)))
                .firstName(first)
                .lastName(last)
                .company(FAKER.company().name().replaceAll("[^A-Za-z ]", ""))
                .address1(FAKER.address().streetAddress())
                .address2(FAKER.address().secondaryAddress())
                .country("India")
                .state("West Bengal")
                .city("Kolkata")
                .zipcode(String.valueOf(FAKER.number().numberBetween(700001, 700100)))
                .mobileNumber(String.valueOf(FAKER.number().numberBetween(9000000000L, 9999999999L)))
                .build();
    }

    public static String randomSentence() {
        return FAKER.lorem().sentence(12);
    }

    public static String randomName() {
        return FAKER.name().firstName().replaceAll("[^A-Za-z]", "");
    }
}
