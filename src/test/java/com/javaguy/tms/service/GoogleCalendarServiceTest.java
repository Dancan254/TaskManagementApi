package com.javaguy.tms.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.javaguy.tms.models.entity.Task;
import com.javaguy.tms.models.enums.TaskStatus;
import com.javaguy.tms.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("Google Calendar Service Tests")
class GoogleCalendarServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Calendar calendarService;

    @Mock
    private Calendar.Events calendarEvents;

    @Mock
    private Calendar.Events.Insert eventsInsert;

    @Mock
    private Calendar.Events.Update eventsUpdate;

    @Mock
    private Calendar.Events.Delete eventsDelete;

    @Mock
    private Calendar.Events.List eventsList;

    @InjectMocks
    private GoogleCalendarService googleCalendarService;

    private Task task;
    private Event googleEvent;

    @BeforeEach
    void setUp() throws IOException {

        // Inject mocked calendarService into googleCalendarService
        ReflectionTestUtils.setField(googleCalendarService, "calendarService", calendarService);
        ReflectionTestUtils.setField(googleCalendarService, "syncEnabled", true);
        ReflectionTestUtils.setField(googleCalendarService, "calendarId", "testCalendarId");
        ReflectionTestUtils.setField(googleCalendarService, "syncTokenPath", "./data/sync-token.txt");

        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Task Description");
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setStatus(TaskStatus.TODO);
        task.setVersion(0L);
        task.setUpdatedAt(LocalDateTime.now().minusDays(2));

        googleEvent = new Event();
        googleEvent.setId("calendarEventId");
        googleEvent.setSummary("Google Event Title");
        googleEvent.setDescription("Google Event Description");
        googleEvent.setStart(new EventDateTime().setDateTime(new DateTime(ZonedDateTime.now().plusDays(2).toInstant().toEpochMilli())));
        googleEvent.setEnd(new EventDateTime().setDateTime(new DateTime(ZonedDateTime.now().plusDays(2).plusHours(1).toInstant().toEpochMilli())));
        // Set a distinctly newer timestamp for googleEvent for reliable conflict resolution testing
        googleEvent.setUpdated(new DateTime(ZonedDateTime.now().toInstant().toEpochMilli()));
        Event.ExtendedProperties properties = new Event.ExtendedProperties();
        properties.setPrivate(Map.of("taskId", task.getId().toString(), "source", "task-management-api"));
        googleEvent.setExtendedProperties(properties);
    }

    @Test
    @DisplayName("syncCreateEvent should create a Google Calendar event and update task")
    void syncCreateEvent_shouldCreateEventAndUpdateTask() throws IOException {
        // Arrange
        when(calendarService.events()).thenReturn(calendarEvents);
        Event createdGoogleEvent = new Event().setId("newCalendarEventId");
        when(calendarEvents.insert(anyString(), any(Event.class))).thenReturn(eventsInsert);
        when(eventsInsert.execute()).thenReturn(createdGoogleEvent);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        googleCalendarService.syncCreateEvent(task);

        // Assert
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(calendarEvents).insert(eq("testCalendarId"), eventCaptor.capture());
        Event capturedEvent = eventCaptor.getValue();

        assertThat(capturedEvent.getSummary()).isEqualTo(task.getTitle());
        assertThat(capturedEvent.getDescription()).isEqualTo(task.getDescription());
        assertThat(task.getCalendarEventId()).isEqualTo("newCalendarEventId");
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("syncCreateEvent should skip creating event if task due date is null")
    void syncCreateEvent_shouldSkipIfDueDateNull() throws IOException {
        // Arrange
        task.setDueDate(null);

        // Act
        googleCalendarService.syncCreateEvent(task);

        // Assert
        verify(calendarEvents, never()).insert(anyString(), any(Event.class));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("syncUpdateEvent should update an existing Google Calendar event")
    void syncUpdateEvent_shouldUpdateExistingEvent() throws IOException {
        // Arrange
        task.setCalendarEventId("existingCalendarEventId");
        when(calendarService.events()).thenReturn(calendarEvents); // Added for this test
        when(calendarEvents.update(anyString(), anyString(), any(Event.class))).thenReturn(eventsUpdate);
        when(eventsUpdate.execute()).thenReturn(googleEvent);

        // Act
        googleCalendarService.syncUpdateEvent(task);

        // Assert
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(calendarEvents).update(eq("testCalendarId"), eq("existingCalendarEventId"), eventCaptor.capture());
        Event capturedEvent = eventCaptor.getValue();

        assertThat(capturedEvent.getSummary()).isEqualTo(task.getTitle());
        assertThat(capturedEvent.getDescription()).isEqualTo(task.getDescription());
    }

    @Test
    @DisplayName("syncUpdateEvent should delete event if task due date is null")
    void syncUpdateEvent_shouldDeleteEventIfDueDateNull() throws IOException {
        // Arrange
        task.setCalendarEventId("existingCalendarEventId");
        task.setDueDate(null);
        when(calendarService.events()).thenReturn(calendarEvents);
        when(calendarEvents.delete(anyString(), anyString())).thenReturn(eventsDelete);
        doNothing().when(eventsDelete).execute();

        // Act
        googleCalendarService.syncUpdateEvent(task);

        // Assert
        verify(calendarEvents).delete(eq("testCalendarId"), eq("existingCalendarEventId"));
        verify(taskRepository).save(task); // Task should be saved with calendarEventId=null
        assertThat(task.getCalendarEventId()).isNull();
    }

    @Test
    @DisplayName("syncDeleteEvent should delete a Google Calendar event")
    void syncDeleteEvent_shouldDeleteEvent() throws IOException {
        // Arrange
        when(calendarService.events()).thenReturn(calendarEvents);
        when(calendarEvents.delete(anyString(), anyString())).thenReturn(eventsDelete);
        doNothing().when(eventsDelete).execute();

        // Act
        googleCalendarService.syncDeleteEvent("calendarEventId");

        // Assert
        verify(calendarEvents).delete(eq("testCalendarId"), eq("calendarEventId"));
    }

    @Test
    @DisplayName("processCalendarUpdates should fetch and process updates from Google Calendar")
    void processCalendarUpdates_shouldFetchAndProcessUpdates() throws IOException {
        // Arrange
        Events events = new Events();
        events.setItems(Collections.singletonList(googleEvent));
        events.setNextSyncToken("newSyncToken");
        events.setUpdated(new DateTime(ZonedDateTime.now().toInstant().toEpochMilli()));
        when(calendarService.events()).thenReturn(calendarEvents);
        when(calendarEvents.list(anyString())).thenReturn(eventsList);
        when(eventsList.execute()).thenReturn(events);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Mock Files.exists to simulate no sync token file initially
        try (var mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            mockedFiles.when(() -> Files.readAllBytes(any(Path.class))).thenReturn("oldSyncToken".getBytes());
            mockedFiles.when(() -> Files.write(any(Path.class), any(byte[].class))).thenReturn(mock(Path.class));

            // Act
            googleCalendarService.processCalendarUpdates();

            // Assert
            verify(eventsList).execute();
            verify(taskRepository).findById(task.getId());
            verify(taskRepository).save(task);
            // Verify sync token is saved
            mockedFiles.verify(() -> Files.write(any(Path.class), eq("newSyncToken".getBytes())), times(1));
        }
    }

    @Test
    @DisplayName("handleUpdatedEvent should update task if calendar event is more recent")
    void handleUpdatedEvent_shouldUpdateTaskIfCalendarEventIsMoreRecent() {
        // Arrange
        task.setUpdatedAt(LocalDateTime.now().minusDays(1));
        googleEvent.setUpdated(new DateTime(LocalDateTime.now().plusDays(1).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()));

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        ReflectionTestUtils.invokeMethod(googleCalendarService, "handleUpdatedEvent", googleEvent);

        // Assert
        assertThat(task.getTitle()).isEqualTo(googleEvent.getSummary());
        assertThat(task.getDescription()).isEqualTo(googleEvent.getDescription());
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("handleUpdatedEvent should NOT update task if task is more recent")
    void handleUpdatedEvent_shouldNotUpdateTaskIfTaskIsMoreRecent() {
        // Arrange
        task.setUpdatedAt(LocalDateTime.now().plusDays(1));
        googleEvent.setUpdated(new DateTime(LocalDateTime.now().minusDays(1).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()));

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        // Act
        ReflectionTestUtils.invokeMethod(googleCalendarService, "handleUpdatedEvent", googleEvent);

        // Assert
        verify(taskRepository, never()).save(any(Task.class));
        assertThat(task.getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("handleCancelledEvent should set task status to CANCELLED")
    void handleCancelledEvent_shouldSetTaskStatusToCancelled() {
        // Arrange
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        ReflectionTestUtils.invokeMethod(googleCalendarService, "handleCancelledEvent", googleEvent);

        // Assert
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(task.getCalendarEventId()).isNull();
        verify(taskRepository).save(task);
    }
}
