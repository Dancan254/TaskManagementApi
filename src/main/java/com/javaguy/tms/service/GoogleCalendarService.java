package com.javaguy.tms.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.javaguy.tms.models.entity.Task;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {
    private final TaskRepository taskRepository;

    @Value( "${google.calendar.key-path}")
    private String keyPath;
    @Value( "${google.calendar.calendar-id}")
    private String calendarId;
    @Value( "${google.calendar.application-name}")
    private String applicationName;
    @Value("${google.calendar.sync-enabled}")
    private boolean syncEnabled;
    @Value("${google.calendar.webhook-url}")
    private String webhookUrl;
    @Value("${google.calendar.sync-token-path}")
    private String syncTokenPath;

    private String channelId;

    private Calendar calendarService;
    private final TaskService taskService;

    @PostConstruct
    public void init(){
        try{
            log.info("Initializing Google Calendar Service");
            this.calendarService = buildCalendarService();
            log.info("Google Calendar Service initialized successfully");
            if (syncEnabled){
                registerWebHookUrl();
            }
        }catch (Exception e){
            log.error("Error initializing Google Calendar Service", e);
            throw new RuntimeException("Calendar initialization failed",e);
        }
    }
    private Calendar buildCalendarService() throws Exception{
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        //load service account details
        GoogleCredentials credentials;
        try(FileInputStream serviceAccount = new FileInputStream(keyPath)){
            credentials = GoogleCredentials.fromStream(serviceAccount)
                    .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));
            return new Calendar.Builder(
                    httpTransport,
                    jsonFactory,
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName(applicationName)
                    .build();
        }catch (Exception e){
            log.error("Error loading service account credentials", e);
            throw new RuntimeException("Error loading service account credentials", e);
        }
    }

    private void registerWebHookUrl(){
        if (webhookUrl == null || webhookUrl.isEmpty()){
            log.warn("No webhook URL provided, skipping");
            return;
        }
        try{
            this.channelId = UUID.randomUUID().toString();

            Channel channel = new Channel()
                    .setId(this.channelId)
                    .setType("web_hook")
                    .setAddress(webhookUrl + "/api/v1/notifications/google-calendar");
            Channel createdChannel = calendarService.events().watch(calendarId, channel).execute();

            log.info("Successfully registered webhook URL: {}, Expiration: {}", createdChannel.getId(), createdChannel.getExpiration());

        }catch (IOException e){
            log.error("Error registering webhook URL", e);
        }
    }
    private String getSyncToken(){
        try {
            if (Files.exists(getSyncTokenPath())){
                return new String(Files.readAllBytes(getSyncTokenPath()));
            }
        }catch (IOException e){
            log.error("Error reading sync token file", e);
        }
        return null;
    }
    private void saveSyncToken(String syncToken){
        Path syncTokenPath = getSyncTokenPath();

        try {
            Path parentDir = syncTokenPath.getParent();

            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("Created missing directory for sync token: {}", parentDir);
            }

            Files.write(syncTokenPath, syncToken.getBytes());
            log.info("Successfully saved new sync token to: {}", syncTokenPath);

        }catch (IOException e){
            log.error("Error writing sync token file", e);
        }
    }

    @Async
    @Transactional
    public void processCalendarUpdates(){
        log.info("Processing calendar updates.....");
        String syncToken = getSyncToken();
        try{
            Calendar.Events.List request = calendarService.events().list(calendarId);
            if (syncToken != null){
                request.setSyncToken(syncToken);
            } else{
                log.info("No sync token found, fetching all events");
                request.setTimeMin(new DateTime(System.currentTimeMillis()));
            }
            Events events;
            do {
                events = request.execute();
                for (Event event : events.getItems()){
                    if ("cancelled".equals(event.getStatus())){
                        handleCancelledEvent(event);
                    } else{
                        handleUpdatedEvent(event);
                    }
                }
                request.setPageToken(events.getNextPageToken());
            }while (events.getNextPageToken() != null);
            saveSyncToken(events.getNextSyncToken());
            log.info("Finished processing calendar updates");
        }catch (IOException e){
            if (e.getMessage().contains("410")){
                log.warn("Sync token expired, clearing sync token file");
                saveSyncToken(null);
                processCalendarUpdates(); //recursive for full sync
            }else{
                log.error("Error processing calendar updates", e);
            }
        }

    }

    private void handleUpdatedEvent(Event event) {
        // Check for our custom property to see if this event belongs to our app
        Event.ExtendedProperties properties = event.getExtendedProperties();
        if (properties == null || properties.getPrivate() == null || !properties.getPrivate().containsKey("taskId")) {
            log.debug("Skipping event {} as it does not have a taskId", event.getId());
            return;
        }

        Long taskId = Long.parseLong(properties.getPrivate().get("taskId").toString());
        Optional<Task> taskOptional = taskRepository.findById(taskId);

        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            log.info("Syncing update from Calendar Event {} to Task {}", event.getId(), taskId);

            DateTime calendarEventUpdated = event.getUpdated();
            if (calendarEventUpdated == null || task.getUpdatedAt() == null ||
                    calendarEventUpdated.getValue() > task.getUpdatedAt().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()) {

                if (event.getSummary() != null) {
                    task.setTitle(event.getSummary());
                }
                if (event.getDescription() != null) {
                    task.setDescription(event.getDescription());
                }
                if (event.getStart() != null && event.getStart().getDateTime() != null) {
                    task.setDueDate(java.time.Instant.ofEpochMilli(event.getStart().getDateTime().getValue()).atZone(java.time.ZoneId.of("UTC")).toLocalDateTime());
                }
                taskRepository.save(task);
            } else {
                log.info("Skipping update from Calendar Event {} as Task {} was more recently updated.", event.getId(), taskId);
            }
        }
    }

    private void handleCancelledEvent(Event event) {
        Event.ExtendedProperties properties = event.getExtendedProperties();
        if (properties != null && properties.getPrivate() != null && properties.getPrivate().containsKey("taskId")) {
            Long taskId = Long.parseLong(properties.getPrivate().get("taskId"));
            log.info("Syncing deletion of Calendar Event {} to Task {}", event.getId(), taskId);
            taskRepository.findById(taskId).ifPresent(task -> {
                task.setStatus(TaskStatus.CANCELLED);
                task.setCalendarEventId(null);
                taskRepository.save(task);
            });
        }
    }

    //create calender invite
    @Async
    public void syncCreateEvent(Task task){
        if (!syncEnabled || task.getDueDate() == null){
            log.warn("Task {} has no due date, skipping", task.getId());
            return;
        }
        try{
            Event event = buildEventFromTask(task);
            Event createdEvent = calendarService.events()
                    .insert(calendarId, event)
                    .execute();
            //update task with calen id
            task.setCalendarEventId(createdEvent.getId());
            taskRepository.save(task);

            log.info("Created event {} for task {}", createdEvent.getId(), task.getId());
        }catch (IOException e){
            log.error("Error creating event for task {}", task.getId(), e);
        }
    }

    @Async
    public void syncUpdateEvent(Task task){
        if (!syncEnabled || task.getCalendarEventId() == null){
            log.warn("Task {} has no calendar event id, skipping", task.getId());
            syncCreateEvent(task);
            return;
        }
        if (task.getDueDate() == null){
            log.warn("Task {} has no due date, deleting event", task.getId());
            syncDeleteEvent(task.getCalendarEventId());
            task.setCalendarEventId(null);
            taskRepository.save(task);
            return;
        }
        try{
            Event event = buildEventFromTask(task);
            event.setId(task.getCalendarEventId());
            calendarService.events()
                    .update(calendarId, task.getCalendarEventId(), event)
                    .execute();
            log.info("Updated event {} for task {}", task.getCalendarEventId(), task.getId());
        } catch (IOException e) {
            log.error("Error updating event for task {}", task.getId(), e);
        }
    }
    @Async
    public void syncDeleteEvent(String calendarEventId){
        if (!syncEnabled || calendarEventId == null){
            return;
        }
        try{
            calendarService.events()
                    .delete(calendarId, calendarEventId)
                    .execute();
            log.info("Deleted event {}", calendarEventId);
        }catch (IOException e){
            log.error("Error deleting event {}", calendarEventId, e);
        }
    }
    private Event buildEventFromTask(Task task){
        Event event = new Event()
                .setSummary(task.getTitle())
                .setDescription(task.getDescription());
        //time and zones
        ZonedDateTime startZdt = task.getDueDate().atZone(ZoneId.of("UTC"));
        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                .setTimeZone("UTC");
        event.setStart(start);

        //end time
        ZonedDateTime endZdt = startZdt.plusHours(1);
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                .setTimeZone("UTC");
        event.setEnd(end);

        //set reminder
        EventReminder[] reminderOverrides = new EventReminder[]{
                new EventReminder().setMethod("email").setMinutes(24 * 60),
                new EventReminder().setMethod("popup").setMinutes(30)
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        //color based on status
        event.setColorId(getColorIdForStatus(task.getStatus()));
        //link back to task
        Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
        extendedProperties.setPrivate(Map.of(
                "taskId", task.getId().toString(),
                "source", "task-management-api"
        ));
        event.setExtendedProperties(extendedProperties);
        event.setUpdated(new DateTime(task.getUpdatedAt().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()));
        return event;
    }
    private String getColorIdForStatus(TaskStatus status) {
        return switch (status) {
            case TODO -> "5";          // Yellow
            case IN_PROGRESS -> "9";   // Blue
            case COMPLETED -> "10";    // Green
            case CANCELLED -> "8";     // Gray
        };
    }
    private Path getSyncTokenPath() {
        return Paths.get(syncTokenPath);
    }
}
