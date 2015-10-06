package org.activitymgr.ui.web.logic.spi;

import java.util.List;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.ILogicContext;

public interface ITaskCreationPatternHandler {
	
	String getLabel();
	
	List<Task> handle(ILogicContext context, Task newTask) throws ModelException;

}
