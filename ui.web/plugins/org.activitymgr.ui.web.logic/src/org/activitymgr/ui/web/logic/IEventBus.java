package org.activitymgr.ui.web.logic;

public interface IEventBus {

	public <EVENT extends AbstractEvent> void register(Class<EVENT> eventType,
			IEventListener<EVENT> listener);

	public void unregister(IEventListener<? extends AbstractEvent> listener);

	public void fire(AbstractEvent event);

}