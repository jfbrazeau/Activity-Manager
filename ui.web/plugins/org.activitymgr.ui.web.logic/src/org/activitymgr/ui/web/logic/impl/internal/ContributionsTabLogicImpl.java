package org.activitymgr.ui.web.logic.impl.internal;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Set;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.event.ContributionChangeEvent;
import org.activitymgr.ui.web.logic.impl.event.ContributionsTabWeekChangedEvent;
import org.activitymgr.ui.web.logic.spi.ICollaboratorsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class ContributionsTabLogicImpl extends AbstractContributionTabLogicImpl implements IEventListener<ContributionChangeEvent> {
	
	@Inject(optional = true)
	private Set<ITabButtonFactory<IContributionsTabLogic>> buttonFactories;
	
	private ContributionsListTableCellProvider contributionsProvider;

	public ContributionsTabLogicImpl(ITabFolderLogic parent) {
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
						ICollaboratorsCellLogicFactory.FIRST_PROPERTY_NAME_ID,
						ICollaboratorsCellLogicFactory.LAST_PROPERTY_NAME_ID });
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

		// Add buttons
		registerButtons(buttonFactories);
		
		// Initialization event
		fireCollabratorOrWeekChangedEvent();
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
		changeFirstDayOfWeekAndUpdateView(Calendar.DATE, -7);
	}

	@Override
	public void onNextWeek() {
		changeFirstDayOfWeekAndUpdateView(Calendar.DATE, 7);
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
			Calendar cursor = (Calendar) contributionsProvider.getFirstDayOfWeek().clone();
			SimpleDateFormat sdf = new SimpleDateFormat("dd");
			Collection<String> propertyIds = contributionsProvider.getPropertyIds();
			for (String dayPropertyId : IContributionsCellLogicFactory.DAY_COLUMNS_IDENTIFIERS) {
				if (propertyIds.contains(dayPropertyId)) {
					getView().setColumnTitle(
							dayPropertyId,
							dayPropertyId.charAt(0)
									+ sdf.format(cursor.getTime()));
				}
				cursor.add(Calendar.DATE, 1);
			}

			getView().reloadContributionTableItems();
			fireCollabratorOrWeekChangedEvent();
		} catch (ModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private void fireCollabratorOrWeekChangedEvent() {
		getEventBus().fire(new ContributionsTabWeekChangedEvent(this, getContributor(), getFirstDayOfWeek()));
	}

	private void changeFirstDayOfWeekAndUpdateView(int amountType, int amount) {
		try {
			contributionsProvider.changeFirstDayOfWeek(amountType, amount);
			getView().setDate(contributionsProvider.getFirstDayOfWeek());
			getView().reloadContributionTableItems();
			fireCollabratorOrWeekChangedEvent();
		} catch (ModelException e) {
			throw new IllegalStateException(e);
		}
	}


	@Override
	public void addTasks(long... taskIds) {
		try {
			if (taskIds.length > 0) {
				for (long taskId : taskIds) {
					if (!contributionsProvider.getTaskIds().contains(taskId)) {
						contributionsProvider.addEmptyWeekContribution(taskId);
					}
				}
				getView().reloadContributionTableItems();
				for (String propertyId : contributionsProvider.getPropertyIds()) {
					org.activitymgr.ui.web.logic.ILogic.IView<?> cell = contributionsProvider.getCell(taskIds[0], propertyId);
					if (cell instanceof ITextFieldLogic.View && !((ITextFieldLogic.View) cell).isReadOnly()) {
						((ITextFieldLogic.View) cell).focus();
						break;
					}
				}
			}
		}
		catch (ModelException e) {
			doThrow(e);
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
			fireCollabratorOrWeekChangedEvent();
		}
		catch (ModelException e) {
			doThrow(e);
		}
	}

	@Override
	public void handle(ContributionChangeEvent event) {
		contributionsProvider.updateTaskTotal(event.getTaskId());
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

	@Override
	public String getLabel() {
		return "Contributions";
	}

}

