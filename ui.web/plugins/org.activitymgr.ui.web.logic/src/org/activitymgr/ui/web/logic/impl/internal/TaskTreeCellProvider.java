package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.event.TaskUpdatedEvent;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

class TaskTreeCellProvider extends AbstractSafeTreeTableCellProviderCallback<Long> implements IEventListener<TaskUpdatedEvent> {

	@Inject
	private IModelMgr modelMgr;

	@Inject
	private ITasksCellLogicFactory cellLogicFactory;

	@Inject
	private ILogicContext context;

	private String filter;
	private Map<Long, Long> parentTaskCache = new HashMap<Long, Long>();
	private Map<Long, List<Long>> taskChildrenCache = new HashMap<Long, List<Long>>();
	private Map<Long, TaskSums> taskSumsCache = new HashMap<Long, TaskSums>();
	private boolean readOnly;
	
	private LoadingCache<Long, LoadingCache<String, ILogic<?>>> cellLogics = CacheBuilder.newBuilder().build(new CacheLoader<Long, LoadingCache<String, ILogic<?>>>() {
		@Override
		public LoadingCache<String, ILogic<?>> load(final Long taskId) throws Exception {
			return CacheBuilder.newBuilder().build(new CacheLoader<String, ILogic<?>>() {
				@Override
				public ILogic<?> load(String propertyId) throws Exception {
					TaskSums taskSums = taskSumsCache.get(taskId);
					return cellLogicFactory.createCellLogic((AbstractLogicImpl<?>) getSource(), context, filter, taskSums, propertyId, readOnly);
				}
			});
		}
	});

	public TaskTreeCellProvider(AbstractLogicImpl<?> source, String filter,
			boolean readOnly) {
		super(source);
		if (filter != null) {
			this.filter = filter.trim();
			if (this.filter.equals("")) {
				this.filter = null;
			}
		}
		this.readOnly = readOnly;
		context.getEventBus().register(TaskUpdatedEvent.class, this);
	}

	@Override
	protected List<Long> unsafeGetChildren(Long parentTaskId) throws ModelException {
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
		return cellLogicFactory.getPropertyIds();
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
		try {
			return cellLogics.get(taskId).get(propertyId).getView();
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected Integer unsafeGetColumnWidth(String propertyId) {
		return cellLogicFactory.getColumnWidth(propertyId);
	}
	
	@Override
	protected Align unsafeGetColumnAlign(String propertyId) {
		return cellLogicFactory.getColumnAlign(propertyId);
	}

	@Override
	public void handle(TaskUpdatedEvent event) {
		Task task = event.getTask();
		long updateAmount = event.getNewValue() - event.getOldValue();
		long updateAmountForDelta = ITasksCellLogicFactory.BUDGET_PROPERTY_ID.equals(event.getProperty()) ? updateAmount : -updateAmount;
		try {
			updateTaskLabelPropertyAmount(task.getId(), ITasksCellLogicFactory.DELTA_PROPERTY_ID, updateAmountForDelta);
			
			Long cursor = event.getTask().getId();
			while ((cursor = parentTaskCache.get(cursor)) != null) {
				updateTaskLabelPropertyAmount(cursor, event.getProperty(), updateAmount);
				updateTaskLabelPropertyAmount(cursor, ITasksCellLogicFactory.DELTA_PROPERTY_ID, updateAmountForDelta);
			}
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
		catch (StringFormatException e) {
			throw new IllegalStateException(e);
		}
		
	}

	private void updateTaskLabelPropertyAmount(long taskId, String property, long amount) throws ExecutionException, StringFormatException {
		ILabelLogic.View view = ((ILabelLogic) cellLogics.get(taskId).get(property)).getView();
		long actualDelta = StringHelper.entryToHundredth(view.getLabel());
		view.setLabel(StringHelper.hundredthToEntry(actualDelta + amount));
	}

	@Override
	public void dispose() {
		getContext().getEventBus().unregister(this);
	}

}