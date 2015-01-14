package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.IContributionsActionHandler;
import org.activitymgr.ui.web.logic.impl.event.ContributionChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class ContributionsTabLogicImpl extends AbstractContributionTabLogicImpl implements IEventListener {
	
	private Map<String, IContributionsActionHandler> actionHandlers = new HashMap<String, IContributionsActionHandler>();
	private ContributionsListTableCellProvider contributionsProvider;

	public ContributionsTabLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);

		// Contributions provider
		contributionsProvider = new ContributionsListTableCellProvider(this, getContext());
		// Set default day
		try {
			contributionsProvider.changeFirstDayOfWeek(new GregorianCalendar());
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}
		getView().setContributionsProvider(getContext().buildTransactionalWrapper(contributionsProvider, ITableCellProviderCallback.class));
		
		// Collaborators provider
		CollaboratorsListTableCellProvider collaboratorsProvider = new CollaboratorsListTableCellProvider(
				this, getContext(), false, true) {
			@Override
			protected Collection<String> unsafeGetPropertyIds() {
				return Arrays.asList(new String[] {
						CollaboratorsCellLogicFatory.FIRST_PROPERTY_NAME_ID,
						CollaboratorsCellLogicFatory.LAST_PROPERTY_NAME_ID });
			}
			@Override
			protected Integer unsafeGetColumnWidth(String propertyId) {
				return 100;
			}
		};
		getView().setCollaboratorsProvider(getContext().buildTransactionalWrapper(collaboratorsProvider, ITableCellProviderCallback.class));
		getView().selectCollaborator(contributionsProvider.getContributor().getId());
		
		// Set the date in the view
		getView().setDate(contributionsProvider.getFirstDayOfWeek());
		
		// Create actions
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor("org.activitymgr.ui.web.logic.contributionAction");
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
		
		// Register the contribution change event
		getEventBus().register(ContributionChangeEvent.class, this);
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
		try {
			contributionsProvider.changeFirstDayOfWeek(value);
			getView().setDate(contributionsProvider.getFirstDayOfWeek());
			getView().reloadContributionTableItems();
		} catch (ModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private void changeFirstDayOfWeekAndUpdateView(int amountType, int amount) {
		try {
			contributionsProvider.changeFirstDayOfWeek(amountType, amount);
			getView().setDate(contributionsProvider.getFirstDayOfWeek());
			getView().reloadContributionTableItems();
		} catch (ModelException e) {
			throw new IllegalStateException(e);
		}
	}


	@Override
	public void addTask(long taskId) {
		try {
			contributionsProvider.addEmptyWeekContribution(taskId);
			getView().reloadContributionTableItems();
		}
		catch (ModelException e) {
			handleError(e);
		}
	}

	@Override
	public Calendar getFirstDayOfWeek() {
		return contributionsProvider.getFirstDayOfWeek();
	}

	@Override
	public void onSelectedCollaboratorChanged(long collaboratorId) {
		try {
			contributionsProvider.changeContributor(getModelMgr().getCollaborator(collaboratorId));
			getView().reloadContributionTableItems();
		}
		catch (ModelException e) {
			handleError(e);
		}
	}

	@Override
	public void handle(AbstractEvent event) {
		ContributionChangeEvent ccEvent = (ContributionChangeEvent) event;
		System.out.println("handle(" + ccEvent.getPropertyId() + ", " + ccEvent.getOldDuration() + ", " + ccEvent.getNewDuration() + ")");
		contributionsProvider.updateTaskTotal(ccEvent.getTaskId());
		getView().reloadContributionTableFooter();
	}


	@Override
	public void onAction(String actionId) {
		actionHandlers.get(actionId).handle(this);
	}

	@Override
	public Collaborator getContributor() {
		return contributionsProvider.getContributor();
	}

	@Override
	public Collection<Long> getTaskIds() {
		return contributionsProvider.getTaskIds();
	}

}

