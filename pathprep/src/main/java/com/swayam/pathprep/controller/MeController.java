package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.request.DeleteUserRequest;
import com.swayam.pathprep.dto.request.UpdateUserRequest;
import com.swayam.pathprep.dto.response.UserProfile;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.security.CookieHelper;
import com.swayam.pathprep.service.MeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private static final Set<String> ALLOWED_PATCH_FIELDS = Set.of(
            "name",
            "branch",
            "gradYear",
            "targetTrackId"
    );

    private final MeService meService;
    private final CookieHelper cookieHelper;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<UserProfile> getMe() {
        UserProfile profile = meService.getCurrentUserProfile();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(profile);
    }

    @PatchMapping
    public ResponseEntity<UserProfile> updateMe(@RequestBody String rawBody) throws IOException {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new BadRequestException("MALFORMED_REQUEST", "Malformed JSON request payload");
        }

        if (node == null || !node.isObject()) {
            throw new BadRequestException("MALFORMED_REQUEST", "Request body must be a JSON object");
        }

        for (java.util.Map.Entry<String, JsonNode> entry : node.properties()) {
            String field = entry.getKey();
            if (!ALLOWED_PATCH_FIELDS.contains(field)) {
                log.warn("PATCH /me rejected: unknown field '{}'", field);
                throw new BadRequestException("MALFORMED_REQUEST", "Unknown field in request: " + field);
            }
        }

        UpdateUserRequest request = objectMapper.treeToValue(node, UpdateUserRequest.class);
        UserProfile updatedProfile = meService.updateProfile(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(updatedProfile);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMe(@Valid @RequestBody DeleteUserRequest request) {
        meService.deleteAccount(request);
        ResponseCookie clearCookie = cookieHelper.createClearRefreshCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
