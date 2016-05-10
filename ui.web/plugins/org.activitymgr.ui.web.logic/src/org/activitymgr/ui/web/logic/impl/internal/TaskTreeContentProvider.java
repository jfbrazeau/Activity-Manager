package org.activitymgr.ui.web.logic.impl.internal;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.ILabelLogic.View.Align;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.LabelLogicImpl;

import com.google.inject.Inject;

class TaskTreeContentProvider extends AbstractSafeTreeTableCellProviderCallback<Long> {

	public static final String NAME_PROPERTY_ID = "NAME";
	public static final String CODE_PROPERTY_ID = "CODE";
	public static final String BUDGET_PROPERTY_ID = "BUDGET";
	public static final String INITIAL_PROPERTY_ID = "INITIAL";
	public static final String COSUMMED_PROPERTY_ID = "CONSUMMED";
	public static final String ETC_PROPERTY_ID = "ETC";
	public static final String DELTA_PROPERTY_ID = "DELTA";
	public static final String COMMENT_PROPERTY_ID = "COMMENT";
	public static final List<String> PROPERTY_IDS = Arrays.asList(new String[] { NAME_PROPERTY_ID, CODE_PROPERTY_ID, BUDGET_PROPERTY_ID, 
			INITIAL_PROPERTY_ID, COSUMMED_PROPERTY_ID, ETC_PROPERTY_ID, DELTA_PROPERTY_ID, COMMENT_PROPERTY_ID } );

	@Inject
	private IModelMgr modelMgr;
	private String filter;
	private Map<Long, Task> tasksCache = new HashMap<Long, Task>();
	private Map<Long, TaskSums> taskSumsCache = new HashMap<Long, TaskSums>();

	public TaskTreeContentProvider(ILogic<?> source, String filter) {
		super(source);
		if (filter != null) {
			this.filter = filter.trim();
			if (this.filter.equals("")) {
				filter = null;
			}
		}
	}

	@Override
	protected Collection<Long> unsafeGetChildren(Long taskId) {
		Task[] subtasks = filter != null ? modelMgr.getSubTasks(taskId, filter) : modelMgr.getSubTasks(taskId);
		Collection<Long> taskIds = new ArrayList<Long>();
		for (Task task : subtasks) {
			taskIds.add(task.getId());
			// Update task cache
			tasksCache.put(task.getId(), task);
			// Invalidate tasks sum cache
			taskSumsCache.remove(task.getId());
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

	private String highlightFilter(String text) {
		if (filter == null || filter.length() == 0) {
			return text;
		}
		else {
			String filterLC = filter.toLowerCase();
			String textToLC = text.toLowerCase();
			int filterLength = filter.length();
			StringWriter sw = new StringWriter();
			if (filterLength > 0) {
				int lastIndexOf = 0;
				int indexOf = 0;
				while ((indexOf = textToLC.indexOf(filterLC, lastIndexOf)) >= 0) {
					sw.append(text.substring(lastIndexOf, indexOf));
					sw.append("*");
					sw.append(text.substring(indexOf, indexOf + filterLength));
					sw.append("*");
					lastIndexOf = indexOf + filterLength;
				}
				int textLength = text.length();
				if (lastIndexOf < textLength) {
					sw.append(text.substring(lastIndexOf, textLength));
				}
			}
			return sw.toString();
		}
	}
	
	@Override
	protected IView<?> unsafeGetCell(Long taskId, String propertyId)
			throws Exception {
		final Task task = tasksCache.get(taskId);
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			String name = highlightFilter(task.getName());
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), name).getView();
		}
		else if (CODE_PROPERTY_ID.equals(propertyId)) {
			String code = highlightFilter(task.getCode());
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), code).getView();
		}
		else if (BUDGET_PROPERTY_ID.equals(propertyId)) {
			TaskSums taskSums = getTaskSums(taskId);
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getBudgetSum()), Align.RIGHT).getView();
		}
		else if (INITIAL_PROPERTY_ID.equals(propertyId)) {
			TaskSums taskSums = getTaskSums(taskId);
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getInitiallyConsumedSum()), Align.RIGHT).getView();
		}
		else if (COSUMMED_PROPERTY_ID.equals(propertyId)) {
			TaskSums taskSums = getTaskSums(taskId);
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getContributionsSums().getConsumedSum()), Align.RIGHT).getView();
		}
		else if (ETC_PROPERTY_ID.equals(propertyId)) {
			TaskSums taskSums = getTaskSums(taskId);
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getTodoSum()), Align.RIGHT).getView();
		}
		else if (DELTA_PROPERTY_ID.equals(propertyId)) {
			TaskSums taskSums = getTaskSums(taskId);
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getBudgetSum()-taskSums.getInitiallyConsumedSum()-taskSums.getContributionsSums().getConsumedSum()-taskSums.getTodoSum()), Align.RIGHT).getView();
		}
		else if (COMMENT_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), task.getComment(), Align.RIGHT).getView();
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
	}

	private TaskSums getTaskSums(long taskId) throws ModelException {
		TaskSums taskSums = taskSumsCache.get(taskId);
		if (taskSums == null) {
			taskSums = modelMgr.getTaskSums(taskId, null, null);
			taskSumsCache.put(taskId, taskSums);
		}
		return taskSums;
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

	@Override
	protected Integer unsafeGetColumnWidth(String propertyId) {
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			return 200;
		}
		else if (CODE_PROPERTY_ID.equals(propertyId)) {
			return 60;
		}
		else if (BUDGET_PROPERTY_ID.equals(propertyId)) {
			return 60;
		}
		else if (INITIAL_PROPERTY_ID.equals(propertyId)) {
			return 60;
		}
		else if (COSUMMED_PROPERTY_ID.equals(propertyId)) {
			return 60;
		}
		else if (ETC_PROPERTY_ID.equals(propertyId)) {
			return 60;
		}
		else if (DELTA_PROPERTY_ID.equals(propertyId)) {
			return 60;
		}
		else if (COMMENT_PROPERTY_ID.equals(propertyId)) {
			return 300;
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
	}
}