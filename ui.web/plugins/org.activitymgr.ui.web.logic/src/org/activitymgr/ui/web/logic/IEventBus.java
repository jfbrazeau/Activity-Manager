package org.activitymgr.ui.web.logic;

public interface IEventBus {

	public abstract void register(Class<? extends AbstractEvent> eventType,
			IEventListener listener);

	public abstract void fire(AbstractEvent event);

}