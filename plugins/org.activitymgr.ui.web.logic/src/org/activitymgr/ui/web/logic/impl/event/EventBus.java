package org.activitymgr.ui.web.logic.impl.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EventBus {

	private Map<Class<? extends Event>, Collection<IEventListener>> listeners = new HashMap<Class<? extends Event>, Collection<IEventListener>>();

	public void register(Class<? extends Event> eventType,
			IEventListener listener) {
		Collection<IEventListener> eventTypeListeners = listeners
				.get(eventType);
		if (eventTypeListeners == null) {
			eventTypeListeners = new ArrayList<IEventListener>();
			listeners.put(eventType, eventTypeListeners);
		}
		eventTypeListeners.add(listener);
	}

	public void fire(Event event) {
		System.out.println("Event : " + event);
		if (event != null) {
			Collection<IEventListener> eventTypeListeners = listeners.get(event
					.getClass());
			if (eventTypeListeners != null) {
				for (IEventListener eventTypeListener : eventTypeListeners) {
					eventTypeListener.handle(event);
				}
			}

		}
	}

}
