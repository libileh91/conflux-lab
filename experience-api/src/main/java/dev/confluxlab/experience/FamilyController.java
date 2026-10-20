package dev.confluxlab.experience;

import java.net.URI;
import java.util.UUID;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestView;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FamilyController {

    private final CapabilityClient capabilityClient;

    public FamilyController(CapabilityClient capabilityClient) {
        this.capabilityClient = capabilityClient;
    }

    @PostMapping("/families")
    public ResponseEntity<RequestAccepted> createFamily(@Valid @RequestBody CreateFamilyRequest request) {
        RequestAccepted accepted = capabilityClient.createFamily(request);
        String statusUrl = "/api/v1/requests/" + accepted.requestId();
        RequestAccepted publicResponse = new RequestAccepted(accepted.requestId(), accepted.status(), statusUrl);
        return ResponseEntity.accepted()
                .location(URI.create(statusUrl))
                .body(publicResponse);
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<RequestView> getRequest(@PathVariable("requestId") UUID requestId) {
        return ResponseEntity.ok(capabilityClient.getRequest(requestId));
    }
}
