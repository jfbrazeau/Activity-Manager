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
import org.activitymgr.ui.web.logic.Align;
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
	public static final String CONSUMMED_PROPERTY_ID = "CONSUMMED";
	public static final String ETC_PROPERTY_ID = "ETC";
	public static final String DELTA_PROPERTY_ID = "DELTA";
	public static final String COMMENT_PROPERTY_ID = "COMMENT";
	public static final List<String> PROPERTY_IDS = Arrays.asList(new String[] { NAME_PROPERTY_ID, CODE_PROPERTY_ID, BUDGET_PROPERTY_ID, 
			INITIAL_PROPERTY_ID, CONSUMMED_PROPERTY_ID, ETC_PROPERTY_ID, DELTA_PROPERTY_ID, COMMENT_PROPERTY_ID } );

	@Inject
	private IModelMgr modelMgr;
	private String filter;
	private Map<Long, Long> parentTaskCache = new HashMap<Long, Long>();
	private Map<Long, List<Long>> taskChildrenCache = new HashMap<Long, List<Long>>();
	private Map<Long, TaskSums> taskSumsCache = new HashMap<Long, TaskSums>();

	public TaskTreeContentProvider(ILogic<?> source, String filter) {
		super(source);
		if (filter != null) {
			this.filter = filter.trim();
			if (this.filter.equals("")) {
				this.filter = null;
			}
		}
	}

	@Override
	protected List<Long> unsafeGetChildren(Long parentTaskId) throws ModelException {
		System.out.println("unsafeGetChildren(" + parentTaskId + ")");
		// Clear cache
		List<Long> subTaskIds = taskChildrenCache.get(parentTaskId);
		if (subTaskIds != null) {
			for (Long subTaskId : subTaskIds) {
				taskSumsCache.remove(subTaskId);
				parentTaskCache.remove(subTaskId);
			}
		}
		taskChildrenCache.remove(parentTaskId);
		
		// Update cache
		Task parentTask = parentTaskId == null ? null : taskSumsCache.get(parentTaskId).getTask();
		List<TaskSums> subTasksSums = modelMgr.getSubTasksSums(parentTask, null, null);
		if (filter != null) {
			Task[] filteredTasks = modelMgr.getSubTasks(parentTaskId, filter);
			List<Long> filteredTaskIds = new ArrayList<Long>();
			for (Task filteredTask : filteredTasks) {
				filteredTaskIds.add(filteredTask.getId());
			}
			List<TaskSums> filteredTaskSums = new ArrayList<TaskSums>();
			for (TaskSums sums : subTasksSums) {
				if (filteredTaskIds.contains(sums.getTask().getId())) {
					filteredTaskSums.add(sums);
				}
			}
			subTasksSums = filteredTaskSums;
		}
		
		subTaskIds = new ArrayList<Long>();
		for (TaskSums subTaskSums : subTasksSums) {
			long subTaskId = subTaskSums.getTask().getId();
			subTaskIds.add(subTaskId);
			// Update task cache
			taskSumsCache.put(subTaskId, subTaskSums);
			parentTaskCache.put(subTaskId, parentTaskId);
		}
		// Register children
		taskChildrenCache.put(parentTaskId, subTaskIds);
		return subTaskIds;
	}

	@Override
	protected final List<Long> unsafeGetRootElements() throws ModelException {
		return unsafeGetChildren(null);
	}

	@Override
	protected final boolean unsafeIsRoot(Long taskId) {
		return parentTaskCache.get(taskId) == null;
	}

	@Override
	protected final boolean unsafeContains(Long taskId) {
		return taskSumsCache.containsKey(taskId) || 
				// The task may not have been yet loaded in cache (if we want to reveal a deep task for example)
				modelMgr.getTask(taskId) != null;
	}

	@Override
	protected Collection<String> unsafeGetPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	protected boolean unsafeHasChildren(Long taskId) {
		if (taskSumsCache.containsKey(taskId)) {
			return !taskSumsCache.get(taskId).isLeaf();
		}
		else {
			return !modelMgr.isLeaf(taskId);
		}
	}

	@Override
	protected final Long unsafeGetParent(Long taskId) {
		Long parentTaskId = parentTaskCache.get(taskId);
		if (parentTaskId != null) {
			return parentTaskId;
		}
		else {
			// The task may not have been yet loaded in cache (if we want to reveal a deep task for example)
			Task parentTask = modelMgr.getParentTask(modelMgr.getTask(taskId));
			return parentTask != null ? parentTask.getId() : null;
		}
	}

	@Override
	protected IView<?> unsafeGetCell(Long taskId, String propertyId)
			throws Exception {
		TaskSums taskSums = taskSumsCache.get(taskId);
		Task task = taskSums.getTask();
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			String name = highlightFilter(task.getName());
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), name).getView();
		}
		else if (CODE_PROPERTY_ID.equals(propertyId)) {
			String code = highlightFilter(task.getCode());
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), code).getView();
		}
		else if (BUDGET_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getBudgetSum())).getView();
		}
		else if (INITIAL_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getInitiallyConsumedSum())).getView();
		}
		else if (CONSUMMED_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getContributionsSums().getConsumedSum())).getView();
		}
		else if (ETC_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getTodoSum())).getView();
		}
		else if (DELTA_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), StringHelper.hundredthToEntry(taskSums.getBudgetSum()-taskSums.getInitiallyConsumedSum()-taskSums.getContributionsSums().getConsumedSum()-taskSums.getTodoSum())).getView();
		}
		else if (COMMENT_PROPERTY_ID.equals(propertyId)) {
			return new LabelLogicImpl((AbstractLogicImpl<?>)getSource(), task.getComment()).getView();
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
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
		else if (CONSUMMED_PROPERTY_ID.equals(propertyId)) {
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
	
	@Override
	protected Align unsafeGetColumnAlign(String propertyId) {
		if (BUDGET_PROPERTY_ID.equals(propertyId)
				|| INITIAL_PROPERTY_ID.equals(propertyId)
				|| CONSUMMED_PROPERTY_ID.equals(propertyId)
				|| ETC_PROPERTY_ID.equals(propertyId)
				|| DELTA_PROPERTY_ID.equals(propertyId)) {
			return Align.RIGHT;
		} else {
			return Align.LEFT;
		}
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
					sw.append("<b><i>");
					sw.append(text.substring(indexOf, indexOf + filterLength));
					sw.append("</i></b>");
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
	
}