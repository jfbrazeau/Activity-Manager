package org.activitymgr.ui.web.logic.impl;

import org.activitymgr.ui.web.logic.IEventListener;
import org.activitymgr.ui.web.logic.ITabLogic;
import org.activitymgr.ui.web.logic.impl.event.ContributionsTabWeekChangedEvent;

import com.google.inject.Inject;

public abstract class AbstractSafeContributionTabStandardButtonLogicImpl extends
	AbstractSafeStandardButtonLogicImpl {

	private IEventListener<ContributionsTabWeekChangedEvent> listener;
	
	@Inject
	public AbstractSafeContributionTabStandardButtonLogicImpl(ITabLogic<?> parent, String label, String iconId, String shortcutKey) {
		super(parent, label, iconId, shortcutKey);
		listener = new IEventListener<ContributionsTabWeekChangedEvent>() {
			@Override
			public void handle(ContributionsTabWeekChangedEvent event) {
				boolean self = (event.getContributor().getId() == getContext().getConnectedCollaborator().getId());
				getView().setEnabled(self && mustBeEnabled(event));
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
