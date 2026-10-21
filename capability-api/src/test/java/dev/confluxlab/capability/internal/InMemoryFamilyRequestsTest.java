package dev.confluxlab.capability.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import dev.confluxlab.capability.api.RequestNotFoundException;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestStatus;
import dev.confluxlab.contracts.RequestView;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
    void retrievesStoredMetadataWithoutUpdatingItsCreationTime() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        RequestAccepted accepted = service.createFamily(new CreateFamilyRequest(PRINCIPAL, DEPENDENTS));
        InMemoryFamilyRequests laterReader = new InMemoryFamilyRequests(
                store, validator, Clock.offset(CLOCK, Duration.ofHours(1)));
        RequestView expected = new RequestView(accepted.requestId(), RequestStatus.PENDING, CLOCK.instant());

        assertEquals(expected, laterReader.getRequest(accepted.requestId()));
        assertEquals(expected, laterReader.getRequest(accepted.requestId()));
        assertEquals(RequestStatus.PENDING, store.findById(accepted.requestId()).orElseThrow().status());
    }

    @Test
    void rejectsAnUnknownIdWithoutInventingARequest() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        UUID unknownId = UUID.randomUUID();

        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> service.getRequest(unknownId));

        assertEquals("Request not found.", exception.getMessage());
        assertTrue(store.findById(unknownId).isEmpty());
    }

    @Test
    void rejectsANullIdBeforeAccessingTheStore() {
        InMemoryFamilyRequestStore store = mock(InMemoryFamilyRequestStore.class);
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);

        assertThrows(IllegalArgumentException.class, () -> service.getRequest(null));
        verifyNoInteractions(store);
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void readsTheStoredStatusWithoutWritingToTheStore(RequestStatus status) {
        InMemoryFamilyRequestStore store = mock(InMemoryFamilyRequestStore.class);
        UUID requestId = UUID.randomUUID();
        StoredFamilyRequest stored = StoredFamilyRequest.pending(requestId,
                new CreateFamilyRequest(PRINCIPAL, DEPENDENTS), CLOCK.instant());
        if (status != RequestStatus.PENDING) {
            stored = stored.transitionTo(RequestStatus.PROCESSING, CLOCK.instant());
        }
        if (status == RequestStatus.SUCCEEDED || status == RequestStatus.FAILED) {
            stored = stored.transitionTo(status, CLOCK.instant());
        }
        when(store.findById(requestId)).thenReturn(Optional.of(stored));
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);

        assertEquals(new RequestView(requestId, status, CLOCK.instant()),
                service.getRequest(requestId));
        verify(store).findById(requestId);
        verifyNoMoreInteractions(store);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void directJavaCallsCannotStoreInvalidContractData(String description, CreateFamilyRequest request) {
        InMemoryFamilyRequestStore store = mock(InMemoryFamilyRequestStore.class);
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);

        InvalidFamilyRequestException exception = assertThrows(InvalidFamilyRequestException.class,
                () -> service.createFamily(request));
        assertEquals("Invalid family request.", exception.getMessage());
        verifyNoInteractions(store);
    }

    @Test
    void acceptsNamesAtThe100CharacterBoundary() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        PersonPayload person = new PersonPayload("A".repeat(100), "B".repeat(100), PRINCIPAL.birthDate());
        CreateFamilyRequest request = new CreateFamilyRequest(person, List.of(person, person));

        RequestAccepted accepted = service.createFamily(request);

        assertEquals(request, store.findById(accepted.requestId()).orElseThrow().payload());
        assertEquals(RequestStatus.PENDING, service.getRequest(accepted.requestId()).status());
    }

    @Test
    void keepsTwoPayloadsAndTheirDatesSeparateAcrossAnInterveningRefusal() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests firstService = new InMemoryFamilyRequests(store, validator, CLOCK);
        Clock laterClock = Clock.offset(CLOCK, Duration.ofHours(1));
        InMemoryFamilyRequests secondService = new InMemoryFamilyRequests(store, validator, laterClock);
        CreateFamilyRequest firstRequest = new CreateFamilyRequest(PRINCIPAL, DEPENDENTS);
        CreateFamilyRequest secondRequest = anotherValidRequest();

        RequestAccepted first = firstService.createFamily(firstRequest);
        assertThrows(InvalidFamilyRequestException.class,
                () -> firstService.createFamily(new CreateFamilyRequest(null, DEPENDENTS)));
        RequestAccepted second = secondService.createFamily(secondRequest);

        assertNotEquals(first.requestId(), second.requestId());
        assertEquals(firstRequest, store.findById(first.requestId()).orElseThrow().payload());
        assertEquals(secondRequest, store.findById(second.requestId()).orElseThrow().payload());
        assertEquals(new RequestView(first.requestId(), RequestStatus.PENDING, CLOCK.instant()),
                secondService.getRequest(first.requestId()));
        assertEquals(new RequestView(second.requestId(), RequestStatus.PENDING, laterClock.instant()),
                firstService.getRequest(second.requestId()));
    }

    @Test
    void concurrentCallersKeepDistinctRequestsAndTheirOwnPayloads() throws Exception {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        InMemoryFamilyRequests service = new InMemoryFamilyRequests(store, validator, CLOCK);
        CreateFamilyRequest firstRequest = new CreateFamilyRequest(PRINCIPAL, DEPENDENTS);
        CreateFamilyRequest secondRequest = anotherValidRequest();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RequestAccepted> firstFuture = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for the test start signal.");
                }
                return service.createFamily(firstRequest);
            });
            Future<RequestAccepted> secondFuture = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for the test start signal.");
                }
                return service.createFamily(secondRequest);
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both test callers must be ready.");
            start.countDown();
            RequestAccepted first = firstFuture.get(5, TimeUnit.SECONDS);
            RequestAccepted second = secondFuture.get(5, TimeUnit.SECONDS);

            assertNotEquals(first.requestId(), second.requestId());
            assertEquals(firstRequest, store.findById(first.requestId()).orElseThrow().payload());
            assertEquals(secondRequest, store.findById(second.requestId()).orElseThrow().payload());
            assertEquals(new RequestView(first.requestId(), RequestStatus.PENDING, CLOCK.instant()),
                    service.getRequest(first.requestId()));
            assertEquals(new RequestView(second.requestId(), RequestStatus.PENDING, CLOCK.instant()),
                    service.getRequest(second.requestId()));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Test workers must stop.");
        }
    }

    private static CreateFamilyRequest anotherValidRequest() {
        return new CreateFamilyRequest(
                new PersonPayload("Robin", "Autre", LocalDate.of(1992, 3, 10)),
                List.of(new PersonPayload("Lina", "Autre", LocalDate.of(2014, 5, 6)),
                        new PersonPayload("Sam", "Autre", LocalDate.of(2019, 7, 8))));
    }

    private static Stream<Arguments> invalidRequests() {
        Stream<Arguments> structure = Stream.of(
                Arguments.of("null request", null),
                Arguments.of("null principal", new CreateFamilyRequest(null, DEPENDENTS)),
                Arguments.of("null dependents", new CreateFamilyRequest(PRINCIPAL, null)),
                Arguments.of("empty dependents", new CreateFamilyRequest(PRINCIPAL, List.of())),
                Arguments.of("one dependent", new CreateFamilyRequest(PRINCIPAL, List.of(CAMILLE))),
                Arguments.of("three dependents", new CreateFamilyRequest(PRINCIPAL, List.of(CAMILLE, NOA, CAMILLE))),
                Arguments.of("null dependent element", new CreateFamilyRequest(PRINCIPAL, Arrays.asList(CAMILLE, null))));
        Stream<Arguments> people = Stream.of(
                invalidPerson("null firstName", new PersonPayload(null, "Exemple", PRINCIPAL.birthDate())),
                invalidPerson("empty firstName", new PersonPayload("", "Exemple", PRINCIPAL.birthDate())),
                invalidPerson("blank firstName", new PersonPayload(" \t\n ", "Exemple", PRINCIPAL.birthDate())),
                invalidPerson("firstName too long", new PersonPayload("A".repeat(101), "Exemple", PRINCIPAL.birthDate())),
                invalidPerson("null lastName", new PersonPayload("Alex", null, PRINCIPAL.birthDate())),
                invalidPerson("blank lastName", new PersonPayload("Alex", " ", PRINCIPAL.birthDate())),
                invalidPerson("lastName too long", new PersonPayload("Alex", "B".repeat(101), PRINCIPAL.birthDate())),
                invalidPerson("null birthDate", new PersonPayload("Alex", "Exemple", null)),
                invalidPerson("future birthDate", new PersonPayload("Alex", "Exemple", LocalDate.now(CLOCK).plusDays(1))))
                .flatMap(stream -> stream);
        return Stream.concat(structure, people);
    }

    private static Stream<Arguments> invalidPerson(String description, PersonPayload person) {
        return Stream.of(
                Arguments.of("principal: " + description, new CreateFamilyRequest(person, DEPENDENTS)),
                Arguments.of("dependent: " + description, new CreateFamilyRequest(PRINCIPAL, List.of(person, NOA))));
    }
}
