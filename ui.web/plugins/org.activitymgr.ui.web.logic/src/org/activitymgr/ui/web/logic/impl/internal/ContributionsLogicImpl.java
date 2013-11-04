package org.activitymgr.ui.web.logic.impl.internal;

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
import org.activitymgr.core.IModelMgr;
import org.activitymgr.core.ModelException;
import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.IntervalContributions;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.beans.TaskContributions;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.AbstractContributionLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractWeekContributionsProviderExtension;
import org.activitymgr.ui.web.logic.impl.DefaultContributionCellLogicProvider;
import org.activitymgr.ui.web.logic.impl.IContributionCellLogicProviderExtension;
import org.activitymgr.ui.web.logic.impl.IContributionsActionHandler;
import org.activitymgr.ui.web.logic.impl.event.DurationChangedEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class ContributionsLogicImpl extends AbstractContributionLogicImpl implements IContributionsLogic, IEventListener {
	
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
	public static final AbstractWeekContributionsProviderExtension DEFAULT_WEEK_CONTRIBUTIONS_PROVIDER = new DefaultWeekContributionsProvider();

	private Calendar firstDayOfWeek;
	private List<TaskContributions> weekContributions = new ArrayList<TaskContributions>();
	private List<String> columnIdentifiers;
	private IContributionCellLogicProviderExtension cellLogicProvider;
	private AbstractWeekContributionsProviderExtension weekContributionsProvider;
	private Map<TaskContributions, Map<String, ILogic<?>>> cellLogics = new HashMap<TaskContributions, Map<String, ILogic<?>>>();
	private Collaborator selectedCollaborator;
	private Map<String, IContributionsActionHandler> actionHandlers = new HashMap<String, IContributionsActionHandler>();

	public ContributionsLogicImpl(RootLogicImpl parent) {
		super(parent);

		// Retrieve collaborators list
		try {
			Collaborator[] activeCollaborators = getModelMgr().getActiveCollaborators(Collaborator.FIRST_NAME_FIELD_IDX, true);
			List<ICollaborator> wrappers = new ArrayList<IContributionsLogic.ICollaborator>();
			for (Collaborator collaborator : activeCollaborators) {
				wrappers.add(new CollaboratorWrapper(collaborator));
			}
			getView().setCollaborators(wrappers);
		}
		catch (DbException e) {
			throw new IllegalStateException("Unable to retrieve collaborators list", e);
		}
		
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
		
		// REtrieve week contributions provider
		// TODO factorize all extension points with class attribute
		cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.weekContributionsProvider");
		if (cfgs.length == 0) {
			weekContributionsProvider = DEFAULT_WEEK_CONTRIBUTIONS_PROVIDER;
		}
		else {
			try {
				weekContributionsProvider = ((AbstractWeekContributionsProviderExtension) cfgs[0].createExecutableExtension("class"));
			} catch (CoreException e) {
				throw new IllegalStateException("Unable to load week contribution provider '" + cfgs[0].getAttribute("class") + "'", e);
			}
		}
			
		// Create actions
		cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.contributionAction");
		for (IConfigurationElement cfg : cfgs) {
			try {
				String iconId = cfg.getAttribute("iconId");
				String label = cfg.getAttribute("label");
				KeyBinding kb = new KeyBinding(cfg.getAttribute("shortcutKey"));
				final IContributionsActionHandler handler = ((IContributionsActionHandler) cfg.createExecutableExtension("handler"));
				// Register the handler
				String id = handler.getClass().getName();
				actionHandlers.put(id, handler);
				// Add the action to the view
				getView().addAction(id, label, kb.toString(), iconId, kb.getKey(), kb.isCtrl(), kb.isShift(), kb.isAlt()); 
			} catch (CoreException e) {
				throw new IllegalStateException("Unable to load action handler '" + cfg.getAttribute("class") + "'", e);
			}
		}

		// Initialization
		firstDayOfWeek = new GregorianCalendar();
		// Fake change : add 0 year and update date in the view
		changeFirstDayOfWeekAndUpdateView(Calendar.YEAR, 0);
		
		// Connected collaborator selection
		getView().selectCollaborator(getContext().getConnectedCollaborator().getLogin());
		
		// Eregister event listener
		getEventBus().register(DurationChangedEvent.class, this);
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
		firstDayOfWeek = value;
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
		firstDayOfWeek.add(amountType, amount);
		firstDayOfWeek = moveToFirstDayOfWeek(firstDayOfWeek);
		getView().setDate(firstDayOfWeek);

		loadContributions();
	}

	private void loadContributions() {
		// Load contributions
		weekContributions.clear();
		if (selectedCollaborator != null) {
			TaskContributions[] tcs = weekContributionsProvider.getWeekContributions(getModelMgr(), selectedCollaborator, firstDayOfWeek);
			weekContributions.addAll(Arrays.asList(tcs));
		}
	
		// TODO comparator as constant
		Collections.sort(weekContributions, new Comparator<TaskContributions>() {
			@Override
			public int compare(TaskContributions tc1, TaskContributions tc2) {
				return tc1.getTask().getFullPath().compareTo(tc2.getTask().getFullPath());
			}
		});
		// Update the view
		getView().removeAllWeekContributions();
		cellLogics.clear();
		for (TaskContributions tc : weekContributions) {
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
			ILogic<?> cellLogic = cellLogicProvider.getCellLogic(this,
					selectedCollaborator, columnId, tc);
			rowLogics.put(columnId, cellLogic);
			cellViews.add(cellLogic.getView());
		}
		getView().addWeekContribution(tc.getTask().getId(), cellViews);
	}

	private static Calendar moveToFirstDayOfWeek(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

	private void onDurationChanged(TaskContributions weekContributions, int dayOfWeek, String duration, ITextFieldLogic textFieldLogic) {
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
					Calendar clone = (Calendar) firstDayOfWeek.clone();
					clone.add(Calendar.DATE, dayOfWeek);
					contribution.setDate(clone);
					getModelMgr().createContribution(contribution, true);
					weekContributions.getContributions()[dayOfWeek] = contribution;
				}
			}
			// Second case : the contribution must be removed
			else if (durationId == 0) {
				// Let's remove the duration
				getModelMgr().removeContribution(contribution, true);
				weekContributions.getContributions()[dayOfWeek] = null;
			}
			// Third case : the contribution must be updated
			else {
				// contribution update
				contribution.setDurationId(durationId);
				getModelMgr().updateContribution(contribution, true);
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

	@Override
	public void addTask(long taskId) {
		try {
			addTask(getModelMgr().getTask(taskId));
		}
		catch (DbException e) {
			handleError(e);
		}
	}

	@Override
	public void addTask(Task task) {
		try {
			addTask(task, getModelMgr().getTaskCodePath(task));
		}
		catch (DbException e) {
			handleError(e);
		}
		catch (ModelException e) {
			handleError(e);
		}
	}

	@Override
	public void addTask(Task task, String taskCodePath) {
		// If the task is already present, no need to add it
		if (getTasksIds().contains(task.getId())) {
			return;
		}
		TaskContributions tc = weekContributionsProvider.newTaskContributions(task, taskCodePath);
		if (tc.getContributions() == null) {
			tc.setContributions(new Contribution[7]);
		}
		if (tc.getTask() == null) {
			throw new IllegalStateException("Week contribution is not associated to any task");
		}
		if (tc.getTaskCodePath() == null) {
			tc.setTaskCodePath(taskCodePath);
		}
		weekContributions.add(tc);
		addWeekContributions(tc);
		// No need to update totals, the new line has no contribution
		//updateTotals();
	}

	private Set<Long> getTasksIds() {
		Set<Long> result = new LinkedHashSet<Long>();
		for (TaskContributions tc : getWeekContributions()) {
			result.add(tc.getTask().getId());
		}
		return result;
	}
	
	@Override
	public Calendar getFirstDayOfWeek() {
		return firstDayOfWeek;
	}

	@Override
	public List<TaskContributions> getWeekContributions() {
		return weekContributions;
	}

	@Override
	public void onSelectedCollaboratorChanged(String login) {
		try {
			if (selectedCollaborator != null && selectedCollaborator.getLogin().equals(login)) {
				return;
			}
			else {
				selectedCollaborator = null;
				if (login != null) {
					selectedCollaborator = getModelMgr().getCollaborator(login);
				}
				// Update contributions
				loadContributions();
			}
		}
		catch (DbException e) {
			handleError(e);
		}
	}

	@Override
	public void handle(AbstractEvent event) {
		if (event instanceof DurationChangedEvent) {
			DurationChangedEvent durationChangedEvent = (DurationChangedEvent) event;
			onDurationChanged(durationChangedEvent.getWeekContributions(), durationChangedEvent.getDayOfWeek(), durationChangedEvent.getDuration(), durationChangedEvent.getTextFieldLogic());
		}
		else {
			throw new IllegalArgumentException("Received an unexpected event");
		}
	}

	@Override
	public void onAction(String actionId) {
		actionHandlers.get(actionId).handle(this);
	}

}

class DefaultWeekContributionsProvider extends AbstractWeekContributionsProviderExtension {
	
	@Override
	public TaskContributions[] getWeekContributions(IModelMgr modelMgr,
			Collaborator contributor, Calendar firstDayOfWeek) {
		// Recherche des taches déclarées pour cet utilisateur
		// pour la semaine courante (et la semaine passée pour
		// réafficher automatiquement les taches de la semaine
		// passée)
		Calendar fromDate = (Calendar) firstDayOfWeek.clone();
		fromDate.add(Calendar.DATE, -7);
		Calendar toDate = (Calendar) firstDayOfWeek.clone();
		toDate.add(Calendar.DATE, 6);
		try {
			IntervalContributions intervalContributions = modelMgr
					.getIntervalContributions(contributor, null, null, fromDate,
							toDate);
			TaskContributions[] weekContributions = intervalContributions.getTaskContributions();
			
			// The result contains the contributions of the previous
			// week. We truncate it before proceeding.
			for (TaskContributions tc : weekContributions) {
				Contribution[] newContribs = new Contribution[7];
				System.arraycopy(tc.getContributions(), 7,
						newContribs, 0, 7);
				tc.setContributions(newContribs);
			}
			return weekContributions;
		} catch (DbException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}
	}

}

class CollaboratorWrapper implements IContributionsLogic.ICollaborator {

	private Collaborator collaborator;

	CollaboratorWrapper(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	@Override
	public String getLogin() {
		return collaborator.getLogin();
	}

	@Override
	public String getFirstName() {
		return collaborator.getFirstName();
	}

	@Override
	public String getLastName() {
		return collaborator.getLastName();
	}
	
}