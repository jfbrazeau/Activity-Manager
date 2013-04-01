package org.activitymgr.ui.web.view;

import org.activitymgr.ui.web.logic.ActivityManagerLogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@Theme("runo")
@SuppressWarnings("serial")
public class ActivityManagerUI extends UI implements IViewFactory {

	private ResourceCache resourceCache;
	private MainPanel content;

	@Override
	protected void init(VaadinRequest request) {
		resourceCache = new ResourceCache();

		// Create UI
		content = new MainPanel(resourceCache);
		setContent(content);

		// Create the logic
		new ActivityManagerLogic(this);
	}

	@Override
	public IView<?> createView(Class<?> logicType, Object... parameters) {
		if (IRootLogic.class.isAssignableFrom(logicType)) {
			return content;
		}
		else {
			throw new IllegalStateException("Unexpected logic type '" + logicType + "'");
		}
	}

}

