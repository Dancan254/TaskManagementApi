package com.javaguy.tms.controller;

import com.javaguy.tms.service.GoogleCalendarService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Hidden
public class WebHookController {
    private final GoogleCalendarService googleCalendarService;

    @PostMapping("/google-calendar")
    public ResponseEntity<Void> handleGoogleCalendarNotification(
            @RequestHeader("X-Goog-Channel-ID") String channelId,
            @RequestHeader("X-Goog-Resource-State") String resourceState,
            @RequestHeader Map<String, String> headers){
        log.info("Received calendar notification for channel {} with resource state {}", channelId, resourceState);
        log.debug("All Headers: {}", headers);

        if ("exists".equalsIgnoreCase(resourceState) || "sync".equalsIgnoreCase(resourceState)){
            googleCalendarService.processCalendarUpdates();
        }

        return ResponseEntity.ok().build();
    }
}
