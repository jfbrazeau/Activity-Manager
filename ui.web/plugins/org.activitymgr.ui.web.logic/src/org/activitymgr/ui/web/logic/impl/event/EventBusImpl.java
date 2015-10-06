package org.activitymgr.ui.web.logic.impl.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.IEventListener;

public class EventBusImpl implements IEventBus {

	private Map<Class<? extends AbstractEvent>, Collection<IEventListener<?>>> listeners = new HashMap<Class<? extends AbstractEvent>, Collection<IEventListener<?>>>();

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.event.IEventBus#register(java.lang.Class, org.activitymgr.ui.web.logic.impl.event.IEventListener)
	 */
	@Override
	public <EVENT extends AbstractEvent> void register(Class<EVENT> eventType,
			IEventListener<EVENT> listener) {
		Collection<IEventListener<?>> eventTypeListeners = listeners
				.get(eventType);
		if (eventTypeListeners == null) {
			eventTypeListeners = new ArrayList<IEventListener<?>>();
			listeners.put(eventType, eventTypeListeners);
		}
		eventTypeListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.event.IEventBus#fire(org.activitymgr.ui.web.logic.impl.event.Event)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void fire(AbstractEvent event) {
		if (event != null) {
			Collection<IEventListener<?>> eventTypeListeners = listeners.get(event
					.getClass());
			if (eventTypeListeners != null) {
				for (IEventListener eventTypeListener : eventTypeListeners) {
					eventTypeListener.handle(event);
				}
			}

		}
	}

}
