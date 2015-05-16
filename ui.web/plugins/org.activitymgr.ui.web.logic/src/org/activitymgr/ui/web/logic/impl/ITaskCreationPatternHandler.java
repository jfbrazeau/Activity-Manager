package org.activitymgr.ui.web.logic.impl;

import java.util.List;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;

public interface ITaskCreationPatternHandler {
	
	List<Task> handle(LogicContext context, Task newTask) throws ModelException;

}
