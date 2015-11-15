package org.activitymgr.ui.web.logic.impl.event;

import java.util.Calendar;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;

public class ContributionsTabWeekChangedEvent extends AbstractEvent {

	private Collaborator contributor;
	private Calendar firstDayOfWeek;
	
	/**
	 * Default constructor.
	 */
	public ContributionsTabWeekChangedEvent(ILogic<?> source, Collaborator contributor, Calendar firstDayOfWeek) {
		super(source);
		this.contributor = contributor;
		this.firstDayOfWeek = firstDayOfWeek;
	}

	public Collaborator getContributor() {
		return contributor;
	}
	
	public Calendar getFirstDayOfWeek() {
		return firstDayOfWeek;
	}
	
}
