package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

public class ContributionChangeEvent extends AbstractEvent {

	private String propertyId;
	private long oldDuration;
	private long newDuration;
	private long taskId;
	
	/**
	 * Default constructor.
	 */
	public ContributionChangeEvent(ILogic<?> source, long taskId, String propertyId, long oldDuration, long newDuration) {
		super(source);
		this.taskId = taskId;
		this.propertyId = propertyId;
		this.oldDuration = oldDuration;
		this.newDuration = newDuration;
	}

	public long getTaskId() {
		return taskId;
	}

	public long getNewDuration() {
		return newDuration;
	}
	
	public long getOldDuration() {
		return oldDuration;
	}
	
	public String getPropertyId() {
		return propertyId;
	}

}
