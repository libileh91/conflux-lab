package dev.confluxlab.experience;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.UUID;
import java.util.function.Supplier;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestView;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CapabilityClient {

    private final RestClient restClient;

    public CapabilityClient(RestClient capabilityRestClient) {
        this.restClient = capabilityRestClient;
    }

    public RequestAccepted createFamily(CreateFamilyRequest request) {
        RequestAccepted accepted = execute(() -> restClient.post()
                .uri("/api/v1/families")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (outgoing, response) -> {
                    throw mapStatus(response.getStatusCode());
                })
                .toEntity(RequestAccepted.class), HttpStatus.ACCEPTED);
        if (accepted.requestId() == null || accepted.status() == null) {
            throw new CapabilityException(HttpStatus.BAD_GATEWAY);
        }
        return accepted;
    }

    public RequestView getRequest(UUID requestId) {
        RequestView view = execute(() -> restClient.get()
                .uri("/api/v1/requests/{requestId}", requestId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (outgoing, response) -> {
                    throw mapStatus(response.getStatusCode());
                })
                .toEntity(RequestView.class), HttpStatus.OK);
        if (!requestId.equals(view.requestId()) || view.status() == null || view.createdAt() == null) {
            throw new CapabilityException(HttpStatus.BAD_GATEWAY);
        }
        return view;
    }

    private <T> T execute(Supplier<ResponseEntity<T>> call, HttpStatus expectedStatus) {
        try {
            ResponseEntity<T> response = call.get();
            if (response.getStatusCode().value() != expectedStatus.value() || response.getBody() == null) {
                throw new CapabilityException(HttpStatus.BAD_GATEWAY);
            }
            return response.getBody();
        } catch (ResourceAccessException exception) {
            throw new CapabilityException(isTimeout(exception)
                    ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new CapabilityException(HttpStatus.BAD_GATEWAY);
        }
    }

    private static CapabilityException mapStatus(HttpStatusCode status) {
        return new CapabilityException(switch (status.value()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_GATEWAY;
        });
    }

    private static boolean isTimeout(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
