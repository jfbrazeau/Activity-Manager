package org.activitymgr.ui.web.logic.impl.internal;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IConstraintsValidator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultConstraintsValidator implements IConstraintsValidator {
	
	private static final IStatus USED_TASK_ERROR_STATUS = new ErrorStatus("This task has contributions");
	
	@Inject
	private IModelMgr modelMgr;

	@Override
	public IStatus canCreateSubTaskUnder(Task task) {
		try {
			return !modelMgr.isLeaf(task.getId()) || modelMgr.getContributionsSum(null, task, null, null) == 0 ? OK_STATUS : USED_TASK_ERROR_STATUS;
		}
		catch (ModelException e) {
			throw new IllegalStateException(e);
		}
	}

}
