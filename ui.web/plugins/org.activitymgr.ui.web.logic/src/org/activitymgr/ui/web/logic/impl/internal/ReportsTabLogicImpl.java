package org.activitymgr.ui.web.logic.impl.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic.ISelectedTaskCallback;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class ReportsTabLogicImpl extends AbstractTabLogicImpl<IReportsTabLogic.View> implements IReportsTabLogic {

	static enum ReportIntervalBoundsMode {
		AUTOMATIC, LOWER_BOUND, BOTH_BOUNDS
	}
	
	@Inject(optional = true)
	private Set<ITabButtonFactory<IReportsTabLogic>> buttonFactories;
	
	private ReportIntervalType intervalType = ReportIntervalType.MONTH;

	private ReportIntervalBoundsMode intervalBoundsMode = ReportIntervalBoundsMode.AUTOMATIC;
	
	private Calendar start = Calendar.getInstance();
	
	private Calendar end = Calendar.getInstance();
	
	private boolean limitTaskScope;

	private String taskScopePath;

	public ReportsTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		// Add buttons
		registerButtons(buttonFactories);
		for (ReportIntervalType type : ReportIntervalType.values()) {
			getView().addIntervalTypeRadioButton(type, StringHelper.toLowerFirst(type.name().toLowerCase()));
		}
		for (ReportIntervalBoundsMode mode : ReportIntervalBoundsMode.values()) {
			getView().addIntervalBoundsModeRadioButton(mode, StringHelper.toLowerFirst(mode.name().replace('_',  ' ').toLowerCase()));
		}
		updateUI();
	}

	@Override
	public String getLabel() {
		return "Reports";
	}

	@Override
	public void onIntervalTypeChanged(Object newValue) {
		intervalType = (ReportIntervalType) newValue;
		updateUI();
	}
	
	@Override
	public void onIntervalBoundsModeChanged(Object newValue) {
		intervalBoundsMode = (ReportIntervalBoundsMode) newValue;
		updateUI();
	}

	@Override
	public void onIntervalBoundsChanged(Date startDate, Date endDate) {
		start.setTime(startDate);
		end.setTime(endDate);
		updateUI();
	}

	@Override
	public void onLimitTaskScopeCheckboxClicked(boolean value) {
		limitTaskScope = value;
		updateUI();
	}

	@Override
	public void onBrowseTaskButtonCLicked() {
		Long selectedTask = null;
		try {
			if (taskScopePath != null) {
				Task task = getModelMgr().getTaskByCodePath(taskScopePath);
				selectedTask = task.getId();
			}
		} catch (ModelException e) {
			// Simply ignore, and consider that no task is selected
		}
		new TaskChooserLogic(this, selectedTask, new ISelectedTaskCallback() {
			@Override
			public void taskSelected(long taskId) {
				Task task = getModelMgr().getTask(taskId);
				try {
					getView().setTaskScopePath(
							getModelMgr().getTaskCodePath(task));
				} catch (ModelException e) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					e.printStackTrace(new PrintWriter(out));
					getRoot().getView().showErrorNotification(
							"Unexpeced error while retrieving task path",
							new String(out.toByteArray()));
				}
			}
		});
	}

	@Override
	public void onTaskScopePathChanged(String value) {
		taskScopePath = value;
	}

	void updateUI() {
		// Update interval type & bounds
		getView().selectIntervalTypeRadioButton(intervalType);
		getView().selectIntervalBoundsModeButton(intervalBoundsMode);

		// Update date fields enablement
		switch (intervalBoundsMode) {
		case AUTOMATIC:
			getView().setIntervalBoundsModeEnablement(false, false);
			break;
		case LOWER_BOUND:
			getView().setIntervalBoundsModeEnablement(true, false);
			break;
		case BOTH_BOUNDS:
			getView().setIntervalBoundsModeEnablement(true, true);
		}

		// Update dates
		if (!ReportIntervalType.DAY.equals(intervalType)) {
			switch (intervalType) {
			case YEAR:
				// Goto start of year
				start.set(Calendar.MONTH, 0);
				start.set(Calendar.DATE, 1);
				
				// Goto start of following year
				end.set(Calendar.MONTH, 0);
				end.set(Calendar.DATE, 1);
				end.add(Calendar.YEAR, 1);
				break;
			case MONTH:
				// Goto start of month
				start.set(Calendar.DATE, 1);
				
				// Goto start of following month
				end.set(Calendar.DATE, 1);
				end.add(Calendar.MONTH, 1);
				break;
			case WEEK:
				// Goto start of week
				start = DateHelper.moveToFirstDayOfWeek(start);
				
				// Goto start of following month
				end = DateHelper.moveToFirstDayOfWeek(end);
				end.add(Calendar.WEEK_OF_YEAR, 1);
				break;
			case DAY:
				// Do nothing
			}
			end.add(Calendar.DATE, -1);
			getView().setIntervalBounds(start.getTime(), end.getTime());
		}

		// Task scope management
		getView().setLimitRootTaskFieldEnabled(limitTaskScope);

		// taskDepthSpinner.setEnabled(includeTasksButton.getSelection());
		// filterByTaskText.setEnabled(filterByTaskCheckbox.getSelection());
		// filterByTaskSelectButton
		// .setEnabled(filterByTaskCheckbox.getSelection());
	}

}
