package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTreeContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.LogicContext;

class TaskTreeContentProvider extends AbstractSafeTreeContentProviderCallback<Task> {

	private IModelMgr modelMgr;

	private HashMap<Long, List<Task>> childrenMap = new HashMap<Long, List<Task>>();
	
	private HashMap<Long, Task> parentsMap = new HashMap<Long, Task>();

	public TaskTreeContentProvider(ILogic<?> source, LogicContext context, IModelMgr modelMgr) {
		super(source, context.getEventBus());
		this.modelMgr = modelMgr;
	}

	@Override
	protected Collection<Task> unsafeGetChildren(Task task) {
		Long key = task != null ? task.getId() : null;
		List<Task> children = childrenMap.get(key);
		if (children == null) {
			children = Arrays.asList(modelMgr.getSubTasks(task));
			System.out.println("loadChildren(" + task + ")=>" + children);
			childrenMap.put(key, children);
			// Populate parents map
			for (Task child : children) {
				parentsMap.put(child.getId(), task);
			}
		}
		return children;
	}

	@Override
	protected Collection<Task> unsafeGetRootElements() {
		return unsafeGetChildren(null);
	}

	@Override
	protected boolean unsafeIsRoot(Task task) {
		return task.getPath().length() == 0;
	}

	@Override
	public String unsafeGetText(Task task, String propertyId)
			throws Exception {
		return task.getName();
	}
	
	@Override
	public Collection<String> getPropertyIds() {
		return DEFAULT_PROPERTY_IDS;
	}

	@Override
	protected boolean unsafeHasChildren(Task element) {
		// Root case
		if (element == null) {
			return true;
		}
		else {
			// If the cache is filled, let's use it
			List<Task> children = childrenMap.get(element.getId());
			if (children != null) {
				return (children.size() > 0);
			}
			// Else, let's ask to the model manager
			else {
				return modelMgr.getSubTasksCount(element.getId()) > 0;
			}

		}
	}

	@Override
	protected Task unsafeGetParent(Task element) {
		return parentsMap.get(element.getId());
	}

}