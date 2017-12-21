package org.activitymgr.ui.web.view.impl.internal.vaadin;

import org.activitymgr.ui.web.view.impl.internal.ActivityManagerUI;

import com.vaadin.server.Constants;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

@SuppressWarnings("serial")
public class OSGiUIProvider extends UIProvider implements Constants {

	@Override
	public synchronized Class<? extends UI> getUIClass(
			UIClassSelectionEvent event) {
		CurrentInstance.clearAll();
		return ActivityManagerUI.class.asSubclass(UI.class);
	}

	@Override
	public UI createInstance(UICreateEvent event) {
		UI instance = super.createInstance(event);
		Activator.getDefault().getInjector().injectMembers(instance);
		return instance;
	}
}
