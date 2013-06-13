package org.activitymgr.ui.web.logic.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activitymgr.core.DbException;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.ModelMgr;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IActionLogic;
import org.activitymgr.ui.web.logic.IContributionCellLogicProviderExtension;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class ContributionsLogicImpl extends AbstractLogicImpl<IContributionsLogic.View> implements IContributionsLogic {
	
	private static final List<String> DEFAULT_COLUMN_IDENTIFIERS = Collections
			.unmodifiableList(Arrays.asList(new String[] {
					IContributionCellLogicProviderExtension.PATH_COLUMN_ID,
					IContributionCellLogicProviderExtension.NAME_COLUMN_ID,
					IContributionCellLogicProviderExtension.MON_COLUMN_ID,
					IContributionCellLogicProviderExtension.TUE_COLUMN_ID,
					IContributionCellLogicProviderExtension.WED_COLUMN_ID,
					IContributionCellLogicProviderExtension.THU_COLUMN_ID,
					IContributionCellLogicProviderExtension.FRI_COLUMN_ID,
					IContributionCellLogicProviderExtension.SAT_COLUMN_ID,
					IContributionCellLogicProviderExtension.SUN_COLUMN_ID,
					IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID }));
	
	public static final IContributionCellLogicProviderExtension DEFAULT_CONTRIBUTION_CELL_LOGIC_PROVIDER = new DefaultContributionCellLogicProvider();
	
	private Calendar date;
	private List<TaskContributions> weekContributions = new ArrayList<TaskContributions>();
	private List<String> columnIdentifiers;
	private IContributionCellLogicProviderExtension cellLogicProvider;
	private Map<TaskContributions, Map<String, ILogic<?>>> cellLogics = new HashMap<TaskContributions, Map<String, ILogic<?>>>();

	public ContributionsLogicImpl(ILogic<?> parent) {
		super(parent);

		// TODO put in an extension point
		columnIdentifiers = getColumnIdentifiers();
		getView().setColumnIdentifiers(columnIdentifiers);
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.contributionCellLogicProvider");
		if (cfgs.length == 0) {
			cellLogicProvider = DEFAULT_CONTRIBUTION_CELL_LOGIC_PROVIDER;
		}
		else {
			try {
				cellLogicProvider = ((IContributionCellLogicProviderExtension) cfgs[0].createExecutableExtension("class"));
			} catch (CoreException e) {
				throw new IllegalStateException("Unable to load cell logic provider '" + cfgs[0].getAttribute("class") + "'", e);
			}
		}
			
		// Add actions
		IActionLogic newTaskActionLogic = new AbstractActionLogicImpl(this, "New task") {
			@Override
			public void onActionInvoked() {
				List<Long> selectedTaskIds = new ArrayList<Long>();
				for (TaskContributions tc : weekContributions) {
					selectedTaskIds.add(tc.getTask().getId());
				}
				new TaskChooserLogicImpl(ContributionsLogicImpl.this, selectedTaskIds);
			}
		};
		getView().addAction(newTaskActionLogic.getView());

		// Initialization
		date = new GregorianCalendar();
		// Fake change : add 0 year and update date in the view
		changeFirstDayOfWeekAndUpdateView(Calendar.YEAR, 0);
	}

	private List<String> getColumnIdentifiers() {
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.contributionColumns");
		if (cfgs.length == 0) {
			return DEFAULT_COLUMN_IDENTIFIERS;
		}
		else {
			Set<String> set = new LinkedHashSet<String>();
			for (IConfigurationElement cfg : cfgs) {
				String column = cfg.getAttribute("id");
System.out.println("Register contribution column : " + column);
				set.add(column);
			}
			System.out.println("Result : " + set);
			return new ArrayList<String>(set);
		}
	}
	
	@Override
	public void onPreviousYear() {
		changeFirstDayOfWeekAndUpdateView(Calendar.YEAR, -1);
	}

	@Override
	public void onPreviousMonth() {
		changeFirstDayOfWeekAndUpdateView(Calendar.MONTH, -1);
	}

	@Override
	public void onPreviousWeek() {
		changeFirstDayOfWeekAndUpdateView(Calendar.WEEK_OF_YEAR, -1);
	}

	@Override
	public void onNextWeek() {
		changeFirstDayOfWeekAndUpdateView(Calendar.WEEK_OF_YEAR, 1);
	}

	@Override
	public void onNextMonth() {
		changeFirstDayOfWeekAndUpdateView(Calendar.MONTH, 1);
	}

	@Override
	public void onNextYear() {
		changeFirstDayOfWeekAndUpdateView(Calendar.YEAR, 1);
	}

	@Override
	public void onDateChange(Calendar value) {
		date = value;
		changeFirstDayOfWeekAndUpdateView(Calendar.DATE, 0);
	}

	private void updateTotals() {
		long total = 0;
		for (int dayOfWeek=0; dayOfWeek<7; dayOfWeek++) {
			long dayTotal = 0;
			for (TaskContributions tc : weekContributions) {
				Contribution c = tc.getContributions()[dayOfWeek];
				if (c != null) {
					dayTotal += c.getDurationId();
					total += c.getDurationId();
				}
			}
			getView().setColumnFooter(IContributionCellLogicProviderExtension.DAY_COLUMNS_IDENTIFIERS.get(dayOfWeek), StringHelper
					.hundredthToEntry(dayTotal));
		}
		getView().setColumnFooter(IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID, StringHelper
				.hundredthToEntry(total));
		// Update the week contributions total
		for (TaskContributions tc : weekContributions) {
			long taskTotal = 0;
			for (int dayOfWeek=0; dayOfWeek<7; dayOfWeek++) {
				Contribution c = tc.getContributions()[dayOfWeek];
				if (c != null) {
					taskTotal += c.getDurationId();
				}
			}
			Map<String, ILogic<?>> rowLogics = cellLogics.get(tc);
			ILabelLogic totalLogic = (ILabelLogic) rowLogics.get(IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID);
			if (totalLogic != null) {
				totalLogic.getView().setLabel(taskTotal != 0 ? 
						StringHelper.hundredthToEntry(taskTotal) : "");
			}
		}
		
	}

	private void changeFirstDayOfWeekAndUpdateView(int amountType, int amount) {
		// Update date
		date.add(amountType, amount);
		date = getMondayBefore(date);
		getView().setDate(date);

		// Recherche des taches déclarées pour cet utilisateur
		// pour la semaine courante (et la semaine passée pour
		// réafficher automatiquement les taches de la semaine
		// passée)
		Calendar fromDate = (Calendar) date.clone();
		fromDate.add(Calendar.DATE, -7);
		Calendar toDate = (Calendar) date.clone();
		toDate.add(Calendar.DATE, 6);
		weekContributions.clear();
		try {
			IntervalContributions intervalContributions = ModelMgr
					.getIntervalContributions(getContext().getConnectedCollaborator(),
							null, fromDate, toDate);
			TaskContributions[] weekContributionsArray = intervalContributions.getTaskContributions();
			weekContributions.addAll(Arrays.asList(weekContributionsArray));
			
		} catch (DbException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}

	
		// The result contains the contributions of the previous
		// week. We truncate it before proceeding.
		getView().removeAllWeekContributions();
		cellLogics.clear();
		// TODO comparator as constant
		Collections.sort(weekContributions, new Comparator<TaskContributions>() {
			@Override
			public int compare(TaskContributions tc1, TaskContributions tc2) {
				return tc1.getTask().getFullPath().compareTo(tc2.getTask().getFullPath());
			}
		});
		for (TaskContributions tc : weekContributions) {
			Contribution[] newContribs = new Contribution[7];
			System.arraycopy(tc.getContributions(), 7,
					newContribs, 0, 7);
			tc.setContributions(newContribs);
			addWeekContributions(tc);
		}
		
		// Update totals
		updateTotals();
	}

	private void addWeekContributions(TaskContributions tc) {
		Map<String, ILogic<?>> rowLogics = new HashMap<String, ILogic<?>>();
		cellLogics.put(tc, rowLogics);
		List<ILogic.IView<?>> cellViews = new ArrayList<ILogic.IView<?>>();
		for (String columnId : columnIdentifiers) {
			ILogic<?> cellLogic = cellLogicProvider.getCellLogic(this, columnId, tc);
			rowLogics.put(columnId, cellLogic);
			cellViews.add(cellLogic.getView());
		}
		getView().addWeekContribution(tc.getTask().getId(), cellViews);
	}

	private static Calendar getMondayBefore(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

	protected void onDurationChanged(TaskContributions weekContributions, int dayOfWeek, String duration, ITextFieldLogic textFieldLogic) {
		System.out.println("onDurationChanged(" + weekContributions + ", " + dayOfWeek + ", " + duration + ", " + textFieldLogic + ")");
		try {
			long durationId = 0;
			if (duration != null && !"".equals(duration.trim())) {
				durationId = StringHelper.entryToHundredth(duration.replaceAll(",", "."));
			}

			Contribution contribution = weekContributions.getContributions()[dayOfWeek];
			// First case : the contribution must be created
			if (contribution == null) {
				if (durationId != 0) {
					// Let's create the new contribution
					contribution = new Contribution();
					contribution.setContributorId(getContext().getConnectedCollaborator().getId());
					contribution.setDurationId(durationId);
					contribution.setTaskId(weekContributions.getTask().getId());
					Calendar clone = (Calendar) date.clone();
					clone.add(Calendar.DATE, dayOfWeek);
					contribution.setDate(clone);
					ModelMgr.createContribution(contribution, true);
					weekContributions.getContributions()[dayOfWeek] = contribution;
				}
			}
			// Second case : the contribution must be removed
			else if (durationId == 0) {
				// Let's remove the duration
				ModelMgr.removeContribution(contribution, true);
				weekContributions.getContributions()[dayOfWeek] = null;
			}
			// Third case : the contribution must be updated
			else {
				// contribution update
				contribution.setDurationId(durationId);
				ModelMgr.updateContribution(contribution, true);
			}
			
			// Update the view
			duration = durationId == 0 ? "" : StringHelper.hundredthToEntry(durationId);
			textFieldLogic.getView().setValue(duration);

			// Update totals
			updateTotals();
		}
		catch (DbException e) {
			handleError(e);
		}
		catch (ModelException e) {
			getRoot().getView().showNotification(e.getMessage());
			textFieldLogic.getView().focus();
		}
		catch (StringFormatException e) {
			getRoot().getView().showNotification(e.getMessage());
			textFieldLogic.getView().focus();
		}
	}

	public void addTask(long taskId) {
		try {
			Task task = ModelMgr.getTask(taskId);
			TaskContributions tc = new TaskContributions();
			tc.setTaskCodePath(ModelMgr.getTaskCodePath(task));
			tc.setTask(task);
			tc.setContributions(new Contribution[14]);
			weekContributions.add(tc);
			addWeekContributions(tc);
			// No need to update totals, the new line has no contribution
			//updateTotals();
		}
		catch (DbException e) {
			handleError(e);
		}
		catch (ModelException e) {
			handleError(e);
		}
	}

}

class DefaultContributionCellLogicProvider implements IContributionCellLogicProviderExtension {
	
	@Override
	public ILogic<?> getCellLogic(final IContributionsLogic parent, final String columnId,
			final TaskContributions weekContributions) {
		if (DAY_COLUMNS_IDENTIFIERS.contains(columnId)) {
			final int dayOfWeek = DAY_COLUMNS_IDENTIFIERS.indexOf(columnId);
			Contribution c = weekContributions.getContributions()[dayOfWeek];
			String duration = (c == null) ? "" : StringHelper.hundredthToEntry(c.getDurationId());
			ITextFieldLogic textFieldLogic = new AbstractTextFieldLogicImpl(parent, duration) {
				@Override
				public void onValueChanged(String newValue) {
					((ContributionsLogicImpl) parent).onDurationChanged(weekContributions, dayOfWeek, newValue, this);
				}
			};
			return textFieldLogic;
		}
		else if (IContributionCellLogicProviderExtension.PATH_COLUMN_ID.equals(columnId)) {
			return new LabelLogicImpl(parent, weekContributions.getTaskCodePath());
		}
		else if (IContributionCellLogicProviderExtension.NAME_COLUMN_ID.equals(columnId)) {
			return new LabelLogicImpl(parent, weekContributions.getTask().getName());
		}
		else if (IContributionCellLogicProviderExtension.TOTAL_COLUMN_ID.equals(columnId)) {
			return new LabelLogicImpl(parent, "");
		}
		else {
			throw new IllegalArgumentException("Unexpected column identifier '" + columnId + "'");
		}
	}

}

