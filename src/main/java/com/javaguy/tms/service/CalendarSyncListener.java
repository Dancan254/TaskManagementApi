package com.javaguy.tms.service;

import com.javaguy.tms.event.TaskCreatedEvent;
import com.javaguy.tms.event.TaskDeletedEvent;
import com.javaguy.tms.event.TaskUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncListener {

    private final GoogleCalendarService googleCalendarService;

    @Async
    @EventListener
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.info("Handling task created event for task {}", event.getSource().getId());
        googleCalendarService.syncCreateEvent(event.getSource());
    }

    @Async
    @EventListener
    public void handleTaskUpdated(TaskUpdatedEvent event) {
        log.info("Handling task updated event for task {}", event.getSource().getId());
        googleCalendarService.syncUpdateEvent(event.getSource());
    }

    @Async
    @EventListener
    public void handleTaskDeleted(TaskDeletedEvent event) {
        log.info("Handling task deleted event for task {}", event.getSource().getId());
        googleCalendarService.syncDeleteEvent(event.getSource().getCalendarEventId());
    }
}