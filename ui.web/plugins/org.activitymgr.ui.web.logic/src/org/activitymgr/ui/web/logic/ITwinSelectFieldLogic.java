package org.activitymgr.ui.web.logic;

import java.util.Collection;

public interface ITwinSelectFieldLogic extends
		IFieldLogic<Collection<String>, ITwinSelectFieldLogic.View> {

	public interface View extends
			IFieldLogic.View<Collection<String>, ITwinSelectFieldLogic> {

		void showOrderButton();

		void addAvailableEntry(String id, String label);

		void selectAll();

	}

}
