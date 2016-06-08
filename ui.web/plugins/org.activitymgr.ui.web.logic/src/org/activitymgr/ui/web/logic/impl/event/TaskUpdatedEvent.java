package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.core.dto.Task;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

public class TaskUpdatedEvent extends AbstractEvent {

	private Task task;
	private String property;
	private long oldValue;
	private long newValue;

	public TaskUpdatedEvent(ILogic<?> source, Task task, String property, long oldValue, long newValue) {
		super(source);
		this.task = task;
		this.property = property;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public Task getTask() {
		return task;
	}
	
	public String getProperty() {
		return property;
	}
	
	public long getOldValue() {
		return oldValue;
	}
	
	public long getNewValue() {
		return newValue;
	}

}