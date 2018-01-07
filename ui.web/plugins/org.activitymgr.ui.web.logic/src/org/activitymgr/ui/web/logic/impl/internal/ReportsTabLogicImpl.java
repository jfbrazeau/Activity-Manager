package org.activitymgr.ui.web.logic.impl.internal;

import java.util.Set;

import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;

import com.google.inject.Inject;

public class ReportsTabLogicImpl extends
		AbstractTabLogicImpl<IReportsTabLogic.View> implements IReportsTabLogic {

	@Inject(optional = true)
	private Set<ITabButtonFactory<IReportsTabLogic>> buttonFactories;
	
	private String tabLabel;

	public ReportsTabLogicImpl(ITabFolderLogic parent,
			final boolean advancedMode) {
		super(parent);

		tabLabel = advancedMode ? "Adv. reports" : "My reports";

		// Add buttons
		registerButtons(buttonFactories);

		// Add a report logic
		getView().setReportsView(
				new ReportsLogicImpl(this, advancedMode).getView());
	}

	@Override
	public String getLabel() {
		return tabLabel;
	}

}

