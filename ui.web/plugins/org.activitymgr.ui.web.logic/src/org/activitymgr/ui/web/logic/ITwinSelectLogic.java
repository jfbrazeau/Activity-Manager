package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface ITwinSelectLogic extends ILogic<ITwinSelectLogic.View> {

	void onValueChangedChanged(Collection<String> itemIds);

	public interface View extends ILogic.IView<ITwinSelectLogic> {

		void showOrderButton();

		void addAvailableEntry(String id, String label);

		void selectAll();

		void select(String id);

	}

}
