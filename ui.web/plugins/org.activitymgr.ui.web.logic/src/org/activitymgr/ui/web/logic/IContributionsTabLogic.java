package org.activitymgr.ui.web.logic;

import java.util.Calendar;

// TODO clear
public interface IContributionsTabLogic extends ILogic<IContributionsTabLogic.View> {

	void onPreviousYear();

	void onPreviousMonth();

	void onPreviousWeek();

	void onNextWeek();

	void onNextMonth();

	void onNextYear();

	void onDateChange(Calendar value);

	void onSelectedCollaboratorChanged(long collaboratorId);

	void onAction(String actionId);

	public interface View extends ILogic.IView<IContributionsTabLogic> {
		
		void setCollaboratorsProvider(ITableCellProviderCallback<Long> collaboratorsProvider);
		
		void selectCollaborator(long collaboratorId);

		void setContributionsProvider(ITableCellProviderCallback<Long> contributionsProvider);

		void setDate(Calendar lastMonday);

		void addAction(String actionId, String label, String keyBindingDescription, String iconId, char key, boolean ctrl, boolean shift, boolean alt);

		void reloadContributionTableItems();
		
		void reloadContributionTableFooter();
	}

}
