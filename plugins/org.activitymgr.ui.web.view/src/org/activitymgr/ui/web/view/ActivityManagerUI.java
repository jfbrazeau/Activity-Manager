package org.activitymgr.ui.web.view;

import org.activitymgr.ui.web.logic.ActivityManagerLogic;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.IGenericCallback;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.view.dialogs.YesNoDialog;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

@Theme("activitymgr")
@SuppressWarnings("serial")
public class ActivityManagerUI extends UI implements IViewFactory, IRootLogic.View {

	private ResourceCache resourceCache;
	@SuppressWarnings("unused")
	private IRootLogic logic;
	private AuthenticationPanel authenticationPanel;
	private ContributionsPanel contributionsPanel;

	@Override
	protected void init(VaadinRequest request) {
		resourceCache = new ResourceCache();

		// Default panels
		authenticationPanel = new AuthenticationPanel(resourceCache);
		contributionsPanel = new ContributionsPanel(resourceCache);
		
		// Create the logic
		new ActivityManagerLogic(this);
	}

	@Override
	public IView<?> createView(Class<?> logicType, Object... parameters) {
		if (IRootLogic.class.isAssignableFrom(logicType)) {
			return this;
		}
		else if (IAuthenticationLogic.class.isAssignableFrom(logicType)) {
			return authenticationPanel;
		}
		else if (IContributionsLogic.class.isAssignableFrom(logicType)) {
			return contributionsPanel;
		}
		else {
			throw new IllegalStateException("Unexpected logic type '" + logicType + "'");
		}
	}

	@Override
	public void registerLogic(IRootLogic logic) {
		this.logic = logic;
	}

	@Override
	public void showConfirm(String message, IGenericCallback<Boolean> callback) {
		YesNoDialog dialog = new YesNoDialog("Confirmation", message, callback);
		getUI().addWindow(dialog);
	}

	@Override
	public void showErrorNotification(String message, String description) {
		Notification.show(message, description != null ? "<br>" + description : null, Type.ERROR_MESSAGE);
	}

	@Override
	public void showNotification(String message) {
		Notification.show(message);
	}

	@Override
	public void showAuthenticationForm() {
		setContent(authenticationPanel);
	}

	@Override
	public void showContributionsForm() {
		setContent(contributionsPanel);
	}

}

