package org.activitymgr.ui.web.viewng.impl.internal.vaadin;

import org.activitymgr.ui.web.viewng.impl.internal.ActivityManagerUI;

import com.vaadin.server.Constants;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class OSGiUIProvider extends UIProvider implements Constants {

	@Override
	public synchronized Class<? extends UI> getUIClass(
			UIClassSelectionEvent event) {
		return ActivityManagerUI.class.asSubclass(UI.class);
	}

	@Override
	public UI createInstance(UICreateEvent event) {
		UI instance = super.createInstance(event);
		Activator.getDefault().getInjector().injectMembers(instance);
		return instance;
	}
}
