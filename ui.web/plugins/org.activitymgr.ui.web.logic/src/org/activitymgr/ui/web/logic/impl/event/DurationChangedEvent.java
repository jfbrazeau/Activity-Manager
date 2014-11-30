package org.activitymgr.ui.web.logic.impl.event;

import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;

public class DurationChangedEvent extends AbstractEvent {

	private TaskContributions weekContributions;
	private int dayOfWeek;
	private String duration;
	private ITextFieldLogic textFieldLogic;

	public DurationChangedEvent(ILogic<?> source,
			TaskContributions weekContributions, int dayOfWeek,
			String duration, ITextFieldLogic textFieldLogic) {
		super(source);
		this.weekContributions = weekContributions;
		this.dayOfWeek = dayOfWeek;
		this.duration = duration;
		this.textFieldLogic = textFieldLogic;
	}
	
	public TaskContributions getWeekContributions() {
		return weekContributions;
	}
	
	public int getDayOfWeek() {
		return dayOfWeek;
	}
	
	public String getDuration() {
		return duration;
	}
	
	public ITextFieldLogic getTextFieldLogic() {
		return textFieldLogic;
	}

}
