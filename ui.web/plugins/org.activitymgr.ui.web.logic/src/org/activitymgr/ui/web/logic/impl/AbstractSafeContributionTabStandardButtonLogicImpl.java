package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.impl.event.ContributionsTabWeekChangedEvent;

import com.google.inject.Inject;

public abstract class AbstractSafeContributionTabStandardButtonLogicImpl extends
	AbstractSafeStandardButtonLogicImpl {

	private IEventListener<ContributionsTabWeekChangedEvent> listener;
	
	@Inject
	public AbstractSafeContributionTabStandardButtonLogicImpl(IContributionsTabLogic parent, String label, String iconId, String shortcutKey) {
		super(parent, label, iconId, shortcutKey);
		listener = new IEventListener<ContributionsTabWeekChangedEvent>() {
			@Override
			public void handle(ContributionsTabWeekChangedEvent event) {
				getView().setEnabled(mustBeEnabled(event));
			}
		};
		getEventBus().register(ContributionsTabWeekChangedEvent.class, listener);
	}

	protected boolean mustBeEnabled(ContributionsTabWeekChangedEvent event) {
		return true;
	}

	@Override
	public void dispose() {
		getEventBus().unregister(listener);
	}

}
