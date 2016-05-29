package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

/**
 * This event is fired when a task has been selected.
 * 
 * @author Jean-Francois Brazeau
 */
public class TaskSelectedEvent extends AbstractEvent {

	/** The identifier of the task that is selected */
	private Long taskId;
	
	/**
	 * Default constructor.
	 * @param source the event's source.
	 * @param taskId the selected task identifier.
	 */
	public TaskSelectedEvent(ILogic<?> source, Long taskId) {
		super(source);
		this.taskId = taskId;
	}

	/**
	 * @return the selected task.
	 */
	public Long getSelectedTaskId() {
		return taskId;
	}

}
