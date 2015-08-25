package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.event.ContributionChangeEvent;

public class ContributionsTabLogicImpl extends AbstractContributionTabLogicImpl implements IEventListener {
	
	private ContributionsListTableCellProvider contributionsProvider;

	public ContributionsTabLogicImpl(AbstractLogicImpl<?> parent) {
		super(parent);

		// Contributions provider
		contributionsProvider = new ContributionsListTableCellProvider(this);
		// Set default day
		try {
			contributionsProvider.changeFirstDayOfWeek(new GregorianCalendar());
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}
		getView().setContributionsProvider(buildTransactionalWrapper(contributionsProvider, ITableCellProviderCallback.class));
		
		// Collaborators provider
		CollaboratorsListTableCellProvider collaboratorsProvider = new CollaboratorsListTableCellProvider(
				this, false, true) {
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
		getView().setCollaboratorsProvider(buildTransactionalWrapper(collaboratorsProvider, ITableCellProviderCallback.class));
		getView().selectCollaborator(contributionsProvider.getContributor().getId());
		
		// Set the date in the view
		getView().setDate(contributionsProvider.getFirstDayOfWeek());
		
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
	public void onToday() {
		onDateChange(new GregorianCalendar());
	}
	
	@Override
	public void onSelectMe() {
		getView().selectCollaborator(getContext().getConnectedCollaborator().getId());
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
	public void addTasks(long... taskIds) {
		try {
			for (long taskId : taskIds) {
				if (!contributionsProvider.getTaskIds().contains(taskId)) {
					contributionsProvider.addEmptyWeekContribution(taskId);
				}
			}
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
	public Collaborator getContributor() {
		return contributionsProvider.getContributor();
	}

	@Override
	public Collection<Long> getTaskIds() {
		return contributionsProvider.getTaskIds();
	}

}

