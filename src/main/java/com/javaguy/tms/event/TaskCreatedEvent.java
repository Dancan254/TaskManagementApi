package com.javaguy.tms.event;

import com.javaguy.tms.models.entity.Task;
import org.springframework.context.ApplicationEvent;

public class TaskCreatedEvent extends ApplicationEvent {
    public TaskCreatedEvent(Task source) {
        super(source);
    }

    @Override
    public Task getSource() {
        return (Task) super.getSource();
    }
}
