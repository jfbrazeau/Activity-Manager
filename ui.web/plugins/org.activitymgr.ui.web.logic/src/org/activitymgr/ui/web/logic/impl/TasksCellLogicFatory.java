package org.activitymgr.ui.web.logic.impl;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.event.TaskUpdatedEvent;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;

public class TasksCellLogicFatory implements ITasksCellLogicFactory {

	private static final String NAME_ATTRIBUTE_NAME = "name";

	private static final String CODE_ATTRIBUTE_NAME = "code";

	private static final String ETC_ATTRIBUTE_NAME = "todo";

	private static final String INITIALLY_CONSUMED_ATTRIBUTE_NAME = "initiallyConsumed";

	private static final String BUDGET_ATTRIBUTE_NAME = "budget";
	
	static final Map<String, String> PROPERTY_ID_TO_ATTRIBUTE_NAME = new HashMap<String, String>();
	static {
		PROPERTY_ID_TO_ATTRIBUTE_NAME.put(NAME_PROPERTY_ID, NAME_ATTRIBUTE_NAME);
		PROPERTY_ID_TO_ATTRIBUTE_NAME.put(CODE_PROPERTY_ID, CODE_ATTRIBUTE_NAME);
		PROPERTY_ID_TO_ATTRIBUTE_NAME.put(BUDGET_PROPERTY_ID, BUDGET_ATTRIBUTE_NAME);
		PROPERTY_ID_TO_ATTRIBUTE_NAME.put(INITIAL_PROPERTY_ID, INITIALLY_CONSUMED_ATTRIBUTE_NAME);
		PROPERTY_ID_TO_ATTRIBUTE_NAME.put(ETC_PROPERTY_ID, ETC_ATTRIBUTE_NAME);
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory#createCellLogic(org.activitymgr.ui.web.logic.impl.AbstractLogicImpl, org.activitymgr.ui.web.logic.ILogicContext, org.activitymgr.core.dto.Task, java.lang.String, boolean)
	 */
	@Override
	public ILogic<?> createCellLogic(final AbstractLogicImpl<?> parentLogic, final IUILogicContext context, final String filter, final TaskSums taskSums, final String propertyId, boolean readOnly) {
		ILogic<?> logic = null;
		final Task task = taskSums.getTask();
		if (NAME_PROPERTY_ID.equals(propertyId)) {
			if (readOnly) {
				String name = highlightFilter(filter, task.getName());
				logic = new LabelLogicImpl(parentLogic, name, true);
			} else {
				logic = new TaskPropertyTextFieldLogic(parentLogic, task.getName(), propertyId, task);
			}
		}
		else if (CODE_PROPERTY_ID.equals(propertyId)) {
			if (readOnly) {
				String code = highlightFilter(filter, task.getCode());
				logic = new LabelLogicImpl(parentLogic, code, true);
			} else {
				logic = new TaskPropertyTextFieldLogic(parentLogic, task.getCode(), propertyId, task);
			}
		}
		else if (BUDGET_PROPERTY_ID.equals(propertyId)) {
			if (readOnly || !taskSums.isLeaf()) {
				logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getBudgetSum()));
			} else {
				logic = new TaskNumericPropertyTextFieldLogic(parentLogic, taskSums.getBudgetSum(), propertyId, task);
			}
		}
		else if (INITIAL_PROPERTY_ID.equals(propertyId)) {
			if (readOnly || !taskSums.isLeaf()) {
				logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getInitiallyConsumedSum()));
			} else {
				logic = new TaskNumericPropertyTextFieldLogic(parentLogic, taskSums.getInitiallyConsumedSum(), propertyId, task);
			}
		}
		else if (CONSUMMED_PROPERTY_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getContributionsSums().getConsumedSum()));
		}
		else if (ETC_PROPERTY_ID.equals(propertyId)) {
			if (readOnly || !taskSums.isLeaf()) {
				logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getTodoSum()));
			} else {
				logic = new TaskNumericPropertyTextFieldLogic(parentLogic, taskSums.getTodoSum(), propertyId, task);
			}
		}
		else if (DELTA_PROPERTY_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, StringHelper.hundredthToEntry(taskSums.getBudgetSum()-taskSums.getInitiallyConsumedSum()-taskSums.getContributionsSums().getConsumedSum()-taskSums.getTodoSum())); 
		}
		else if (COMMENT_PROPERTY_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, task.getComment());
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
		return logic;
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ICollaboratorsCellLogicFactory#getPropertyIds()
	 */
	@Override
	public Collection<String> getPropertyIds() {
		return PROPERTY_IDS;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ICollaboratorsCellLogicFactory#getColumnWidth(java.lang.String)
	 */
	@Override
	public Integer getColumnWidth(String propertyId) {
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
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.spi.ICellLogicFactory#getColumnAlign(java.lang.String)
	 */
	@Override
	public Align getColumnAlign(String propertyId) {
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

	private String highlightFilter(final String filter, String text) {
		text = text.replaceAll("<", "&lt;");
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

class TaskNumericPropertyTextFieldLogic extends TaskPropertyTextFieldLogic {
	
	public TaskNumericPropertyTextFieldLogic(AbstractLogicImpl<?> parent, long value, String property, Task task) {
		super(parent, StringHelper.hundredthToEntry(value), property, task);
		getView().setNumericFieldStyle();
	}

	@Override
	protected void unsafeOnValueChanged(String newValue)
			throws ModelException, IllegalAccessException, InvocationTargetException, StringFormatException, NumberFormatException, NoSuchMethodException {
		BeanUtilsBean beanUtils = BeanUtilsBean2.getInstance();
		long oldValue = Long.parseLong(beanUtils.getProperty(getTask(), TasksCellLogicFatory.PROPERTY_ID_TO_ATTRIBUTE_NAME.get(getProperty())));
		long newValueAsLong = StringHelper.entryToHundredth(newValue);
		super.unsafeOnValueChanged(String.valueOf(newValueAsLong));
		getView().setValue(StringHelper.hundredthToEntry(newValueAsLong));
		getEventBus().fire(new TaskUpdatedEvent(this, getTask(), getProperty(), oldValue, newValueAsLong));
	}

};

class TaskPropertyTextFieldLogic extends AbstractSafeTextFieldLogicImpl {
	
	private String property;
	private Task task;

	public TaskPropertyTextFieldLogic(AbstractLogicImpl<?> parent, String value, String property, Task task) {
		super(parent, value, true);
		this.property = property;
		this.task = task;
	}

	@Override
	protected void unsafeOnValueChanged(String newValue)
			throws ModelException, IllegalAccessException, InvocationTargetException, StringFormatException, NumberFormatException, NoSuchMethodException {
		BeanUtilsBean beanUtils = BeanUtilsBean2.getInstance();
		beanUtils.setProperty(task, TasksCellLogicFatory.PROPERTY_ID_TO_ATTRIBUTE_NAME.get(property), newValue);
		getModelMgr().updateTask(task);
	}
	
	public Task getTask() {
		return task;
	}
	
	public String getProperty() {
		return property;
	}
	
}
