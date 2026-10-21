package dev.confluxlab.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ContractValidationTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);
    private static final UUID REQUEST_ID = UUID.fromString("e91aa057-68f9-4d38-9d24-e8eb7f747630");
    private static final String STATUS_URL = "/api/v1/requests/" + REQUEST_ID;
    private static final PersonPayload PRINCIPAL = new PersonPayload(
            "Alex", "Exemple", LocalDate.of(1990, 4, 12));
    private static final PersonPayload CAMILLE = new PersonPayload(
            "Camille", "Exemple", LocalDate.of(2015, 8, 21));
    private static final PersonPayload NOA = new PersonPayload(
            "Noa", "Exemple", LocalDate.of(2018, 2, 3));
    private static final List<PersonPayload> DEPENDENTS = List.of(CAMILLE, NOA);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .clockProvider(() -> FIXED_CLOCK)
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void acceptsFamilyWithOnePrincipalAndTwoDependents() {
        assertTrue(validator.validate(new CreateFamilyRequest(PRINCIPAL, DEPENDENTS)).isEmpty());
    }

    @Test
    void rejectsNullPrincipal() {
        assertViolation(new CreateFamilyRequest(null, DEPENDENTS), "principal", NotNull.class);
    }

    @Test
    void rejectsNullDependents() {
        assertViolation(new CreateFamilyRequest(PRINCIPAL, null), "dependents", NotNull.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void rejectsAnyDependentCountOtherThanTwo(int count) {
        var request = new CreateFamilyRequest(PRINCIPAL, Collections.nCopies(count, CAMILLE));

        assertViolation(request, "dependents", Size.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void rejectsNullDependentAtEitherPosition(int index) {
        var dependents = new ArrayList<>(DEPENDENTS);
        dependents.set(index, null);
        var request = new CreateFamilyRequest(PRINCIPAL, dependents);

        assertViolation(request, "dependents[" + index + "].<list element>", NotNull.class);
    }

    @Test
    void cascadesValidationIntoPrincipalAndBothDependents() {
        var principal = new PersonPayload(" ", "Exemple", PRINCIPAL.birthDate());
        var firstDependent = new PersonPayload("Camille", null, CAMILLE.birthDate());
        var secondDependent = new PersonPayload("Noa", "Exemple", TODAY.plusDays(1));
        var request = new CreateFamilyRequest(principal, List.of(firstDependent, secondDependent));

        assertViolations(request, Map.of(
                "principal.firstName", NotBlank.class,
                "dependents[0].lastName", NotBlank.class,
                "dependents[1].birthDate", PastOrPresent.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", " \t\n "})
    void rejectsNullOrBlankFirstName(String firstName) {
        var person = new PersonPayload(firstName, "Exemple", PRINCIPAL.birthDate());

        assertViolation(person, "firstName", NotBlank.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", " \t\n "})
    void rejectsNullOrBlankLastName(String lastName) {
        var person = new PersonPayload("Alex", lastName, PRINCIPAL.birthDate());

        assertViolation(person, "lastName", NotBlank.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void acceptsNamesUpToOneHundredCharacters(int length) {
        String name = "a".repeat(length);
        var person = new PersonPayload(name, name, PRINCIPAL.birthDate());

        assertTrue(validator.validate(person).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {101, 150})
    void rejectsNamesLongerThanOneHundredCharacters(int length) {
        String name = "a".repeat(length);

        assertViolation(new PersonPayload(name, "Exemple", PRINCIPAL.birthDate()), "firstName", Size.class);
        assertViolation(new PersonPayload("Alex", name, PRINCIPAL.birthDate()), "lastName", Size.class);
    }

    @Test
    void rejectsMissingBirthDate() {
        assertViolation(new PersonPayload("Alex", "Exemple", null), "birthDate", NotNull.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-22", "2027-10-21"})
    void rejectsBirthDatesAfterTodayAccordingToFixedClock(String birthDate) {
        var person = new PersonPayload("Alex", "Exemple", LocalDate.parse(birthDate));

        assertViolation(person, "birthDate", PastOrPresent.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-21", "2026-10-20", "1990-04-12"})
    void acceptsBirthDatesTodayOrInThePastAccordingToFixedClock(String birthDate) {
        var person = new PersonPayload("Alex", "Exemple", LocalDate.parse(birthDate));

        assertTrue(validator.validate(person).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void acceptsEveryDefinedStatusInBothResponseContracts(RequestStatus status) {
        assertTrue(validator.validate(new RequestAccepted(REQUEST_ID, status, STATUS_URL)).isEmpty());
        assertTrue(validator.validate(new RequestView(REQUEST_ID, status, FIXED_CLOCK.instant())).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidRequestAcceptedFields")
    void rejectsMissingOrBlankRequestAcceptedFields(
            RequestAccepted accepted, String path, Class<? extends Annotation> constraint) {
        assertViolation(accepted, path, constraint);
    }

    static Stream<Arguments> invalidRequestAcceptedFields() {
        return Stream.of(
                Arguments.of(new RequestAccepted(null, RequestStatus.PENDING, STATUS_URL),
                        "requestId", NotNull.class),
                Arguments.of(new RequestAccepted(REQUEST_ID, null, STATUS_URL),
                        "status", NotNull.class),
                Arguments.of(new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, null),
                        "statusUrl", NotBlank.class),
                Arguments.of(new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, ""),
                        "statusUrl", NotBlank.class),
                Arguments.of(new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, " \t\n "),
                        "statusUrl", NotBlank.class));
    }

    @ParameterizedTest
    @MethodSource("invalidRequestViewFields")
    void rejectsMissingRequestViewFields(RequestView view, String path) {
        assertViolation(view, path, NotNull.class);
    }

    static Stream<Arguments> invalidRequestViewFields() {
        return Stream.of(
                Arguments.of(new RequestView(null, RequestStatus.PENDING, FIXED_CLOCK.instant()), "requestId"),
                Arguments.of(new RequestView(REQUEST_ID, null, FIXED_CLOCK.instant()), "status"),
                Arguments.of(new RequestView(REQUEST_ID, RequestStatus.PENDING, null), "createdAt"));
    }

    private static <T> void assertViolation(
            T value, String path, Class<? extends Annotation> constraint) {
        assertViolations(value, Map.of(path, constraint));
    }

    private static <T> void assertViolations(
            T value, Map<String, Class<? extends Annotation>> expected) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        assertEquals(expected.size(), violations.size(), () -> "Unexpected violations: " + violations);
        Map<String, Class<? extends Annotation>> actual = violations.stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getConstraintDescriptor().getAnnotation().annotationType()));

        assertEquals(expected, actual);
    }
}
