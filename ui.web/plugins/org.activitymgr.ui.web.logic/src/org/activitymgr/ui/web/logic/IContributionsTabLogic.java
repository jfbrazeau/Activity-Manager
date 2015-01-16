package org.activitymgr.ui.web.logic;

import java.util.Calendar;

// TODO clear
public interface IContributionsTabLogic extends ITabLogic<IContributionsTabLogic.View> {

	void onPreviousYear();

	void onPreviousMonth();

	void onPreviousWeek();

	void onNextWeek();

	void onNextMonth();

	void onNextYear();

	void onDateChange(Calendar value);

	void onSelectedCollaboratorChanged(long collaboratorId);

	public interface View extends ITabLogic.View<IContributionsTabLogic> {
		
		void setCollaboratorsProvider(ITableCellProviderCallback<Long> collaboratorsProvider);
		
		void selectCollaborator(long collaboratorId);

		void setContributionsProvider(ITableCellProviderCallback<Long> contributionsProvider);

		void setDate(Calendar lastMonday);

		void reloadContributionTableItems();

		void reloadContributionTableFooter();

	}

}
