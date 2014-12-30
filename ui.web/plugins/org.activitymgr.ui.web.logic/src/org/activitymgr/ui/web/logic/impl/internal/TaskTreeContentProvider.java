package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.LabelLogicImpl;
import org.activitymgr.ui.web.logic.impl.LogicContext;

class TaskTreeContentProvider extends AbstractSafeTreeTableCellProviderCallback<Long> {

	private static final String NAME_PROPERTY_ID = ITableCellProviderCallback.NAME_PROPERTY_ID;
	private static final String CODE_PROPERTY_ID = "CODE";
	private static final String BUDGET_PROPERTY_ID = "BUDGET";
	private static final String INITIAL_PROPERTY_ID = "INITIAL";
	private static final String COSUMMED_PROPERTY_ID = "CONSUMMED";
	private static final String ETC_PROPERTY_ID = "ETC";
	private static final String DELTA_PROPERTY_ID = "DELTA";
	private static final String COMMENT_PROPERTY_ID = "COMMENT";
	private static final List<String> PROPERTY_IDS = Arrays.asList(new String[] { NAME_PROPERTY_ID, CODE_PROPERTY_ID, BUDGET_PROPERTY_ID, 
			INITIAL_PROPERTY_ID, COSUMMED_PROPERTY_ID, ETC_PROPERTY_ID, DELTA_PROPERTY_ID, COMMENT_PROPERTY_ID } );

	private IModelMgr modelMgr;

	public TaskTreeContentProvider(ILogic<?> source, LogicContext context) {
		super(source, context);
		this.modelMgr = context.getComponent(IModelMgr.class);
	}

	@Override
	protected Collection<Long> unsafeGetChildren(Long taskId) {
		Task[] subtasks = modelMgr.getSubtasks(taskId);
		Collection<Long> taskIds = new ArrayList<Long>();
		for (Task task : subtasks) {
			taskIds.add(task.getId());
		}
		return taskIds;
	}

	@Override
	protected Collection<Long> unsafeGetRootElements() {
		return unsafeGetChildren(null);
	}

	@Override
	protected boolean unsafeIsRoot(Long taskId) {
		Task task = modelMgr.getTask(taskId);
		return task.getPath().length() == 0;
	}

	@Override
	protected boolean unsafeContains(Long taskId) {
		return modelMgr.getTask(taskId) != null;
	}

	@Override
	protected IView<?> unsafeGetCell(Long taskId, String propertyId)
			throws Exception {
		final Task task = modelMgr.getTask(taskId);
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), task.getName()).getView();
		}
		else if (CODE_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), task.getCode()).getView();
		}
		else if (BUDGET_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(task.getBudget())).getView();
		}
		else if (INITIAL_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(task.getInitiallyConsumed())).getView();
		}
		else if (COSUMMED_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(1234)).getView();
		}
		else if (ETC_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(task.getTodo())).getView();
		}
		else if (DELTA_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(4321)).getView();
		}
		else if (COMMENT_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), task.getComment()).getView();
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
	}
	@Override
	protected Collection<String> unsafeGetPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	protected boolean unsafeHasChildren(Long taskId) {
		return !modelMgr.isLeaf(taskId);
	}

	@Override
	protected Long unsafeGetParent(Long taskId) {
		Task parentTask = modelMgr.getParentTask(modelMgr.getTask(taskId));
		return parentTask != null ? parentTask.getId() : null;
	}

}