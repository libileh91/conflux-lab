package dev.confluxlab.capability.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import dev.confluxlab.capability.api.CapabilityNotReadyException;
import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class InMemoryFamilyRequestsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private static final PersonPayload PRINCIPAL = new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12));
    private static final PersonPayload CAMILLE = new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21));
    private static final PersonPayload NOA = new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3));
    private static final List<PersonPayload> DEPENDENTS = List.of(CAMILLE, NOA);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .clockProvider(() -> CLOCK)
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void storesThePayloadAsPendingBeforeReturningAcceptance() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        CreateFamilyRequest request = new CreateFamilyRequest(PRINCIPAL, DEPENDENTS);

        RequestAccepted accepted = service.createFamily(request);

        assertNotNull(accepted.requestId());
        assertEquals(RequestStatus.PENDING, accepted.status());
        assertEquals("/api/v1/requests/" + accepted.requestId(), accepted.statusUrl());
        StoredFamilyRequest stored = store.findById(accepted.requestId()).orElseThrow();
        assertEquals(accepted.requestId(), stored.requestId());
        assertEquals(RequestStatus.PENDING, stored.status());
        assertEquals(request, stored.payload());
        assertEquals(CLOCK.instant(), stored.createdAt());
    }

    @Test
    void storesTwoIdenticalRequestsUnderDistinctIds() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        CreateFamilyRequest request = new CreateFamilyRequest(PRINCIPAL, DEPENDENTS);

        RequestAccepted first = service.createFamily(request);
        RequestAccepted second = service.createFamily(request);

        assertNotEquals(first.requestId(), second.requestId());
        assertEquals(request, store.findById(first.requestId()).orElseThrow().payload());
        assertEquals(request, store.findById(second.requestId()).orElseThrow().payload());
    }

    @Test
    void keepsAnImmutableSnapshotWhenTheCallerChangesItsListAfterAcceptance() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        List<PersonPayload> callerList = new ArrayList<>(DEPENDENTS);

        RequestAccepted accepted = service.createFamily(new CreateFamilyRequest(PRINCIPAL, callerList));
        callerList.clear();

        StoredFamilyRequest stored = store.findById(accepted.requestId()).orElseThrow();
        assertEquals(DEPENDENTS, stored.payload().dependents());
        assertThrows(UnsupportedOperationException.class, () -> stored.payload().dependents().clear());
    }

    @Test
    void aNewStoreDoesNotRecoverPreviouslyAcceptedRequests() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        RequestAccepted accepted = service.createFamily(new CreateFamilyRequest(PRINCIPAL, DEPENDENTS));

        assertTrue(new InMemoryFamilyRequestStore().findById(accepted.requestId()).isEmpty());
        assertTrue(store.findById(accepted.requestId()).isPresent());
    }

    @Test
    void doesNotReturnAcceptanceWhenTheStoreThrows() {
        InMemoryFamilyRequestStore failingStore = mock(InMemoryFamilyRequestStore.class);
        IllegalStateException failure = new IllegalStateException("Simulated storage failure.");
        doThrow(failure).when(failingStore).save(any(StoredFamilyRequest.class));
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(failingStore, validator, CLOCK);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.createFamily(new CreateFamilyRequest(PRINCIPAL, DEPENDENTS))));
    }

    @Test
    void trackingRemainsUnavailableEvenForAnAcceptedRequest() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        RequestAccepted accepted = service.createFamily(new CreateFamilyRequest(PRINCIPAL, DEPENDENTS));

        assertThrows(CapabilityNotReadyException.class, () -> service.getRequest(accepted.requestId()));
        assertEquals(RequestStatus.PENDING, store.findById(accepted.requestId()).orElseThrow().status());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void directJavaCallsCannotStoreInvalidContractData(CreateFamilyRequest request) {
        InMemoryFamilyRequestStore store = mock(InMemoryFamilyRequestStore.class);
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);

        assertThrows(InvalidFamilyRequestException.class, () -> service.createFamily(request));
        verifyNoInteractions(store);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(new CreateFamilyRequest(null, DEPENDENTS)),
                Arguments.of(new CreateFamilyRequest(PRINCIPAL, null)),
                Arguments.of(new CreateFamilyRequest(PRINCIPAL, List.of(CAMILLE))),
                Arguments.of(new CreateFamilyRequest(PRINCIPAL, Arrays.asList(CAMILLE, null))),
                Arguments.of(new CreateFamilyRequest(
                        new PersonPayload(" ", "Exemple", PRINCIPAL.birthDate()), DEPENDENTS)),
                Arguments.of(new CreateFamilyRequest(
                        new PersonPayload("Alex", "Exemple", LocalDate.now(CLOCK).plusDays(1)), DEPENDENTS)));
    }
}
