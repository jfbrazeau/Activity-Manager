package org.activitymgr.ui.web.logic;


public interface IEventListener<TYPE extends AbstractEvent> {
	
	void handle(TYPE event);

}
