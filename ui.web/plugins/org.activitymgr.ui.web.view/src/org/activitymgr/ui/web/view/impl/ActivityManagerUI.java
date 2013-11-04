package org.activitymgr.ui.web.view.impl;

import java.io.IOException;

import javax.servlet.http.Cookie;

import org.activitymgr.ui.web.logic.ActivityManagerLogic;
import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.logic.IGenericCallback;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.IViewFactory;
import org.activitymgr.ui.web.view.impl.dialogs.TaskChooserDialog;
import org.activitymgr.ui.web.view.impl.dialogs.YesNoDialog;
import org.activitymgr.ui.web.view.util.LabelView;
import org.activitymgr.ui.web.view.util.ResourceCache;
import org.activitymgr.ui.web.view.util.TextFieldView;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

@Theme("activitymgr")
@SuppressWarnings("serial")
public class ActivityManagerUI extends UI implements IViewFactory,
		IRootLogic.View {

	private ResourceCache resourceCache;
	@SuppressWarnings("unused")
	private IRootLogic logic;
	private Cookie[] cookies;

	@Override
	protected void init(VaadinRequest request) {
		// Create the resource cache
		resourceCache = new ResourceCache();
		// Fetch all cookies from the request
		cookies = request.getCookies();
		// Create the logic
		new ActivityManagerLogic(this);
	}

	@Override
	public IView<?> createView(Class<?> logicType, Object... parameters) {
		if (IRootLogic.class.isAssignableFrom(logicType)) {
			return this;
		} else if (IAuthenticationLogic.class.isAssignableFrom(logicType)) {
			String defaultUser = (String) (parameters.length > 0 ? parameters[0] : null);
			return new AuthenticationPanel(resourceCache, defaultUser);
		} else if (IContributionsLogic.class.isAssignableFrom(logicType)) {
			return new ContributionsPanel(resourceCache);
		} else if (ITaskChooserLogic.class.isAssignableFrom(logicType)) {
			TaskChooserDialog dialog = new TaskChooserDialog(resourceCache);
			getUI().addWindow(dialog);
			return dialog;
		} else if (ILabelLogic.class.isAssignableFrom(logicType)) {
			return new LabelView();
		} else if (ITextFieldLogic.class.isAssignableFrom(logicType)) {
			return new TextFieldView();
		} else {
			throw new IllegalStateException("Unexpected logic type '"
					+ logicType + "'");
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
		Notification.show(message, description != null ? "<br>" + description
				: null, Type.ERROR_MESSAGE);
	}

	@Override
	public void showNotification(String message) {
		Notification.show(message);
	}

	@Override
	public String getCookie(String name) {
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	@Override
	public void show(IView<?> view) {
		setContent((Component) view);
	}

	@Override
	public void setCookie(String name, String value) {
		// Cookie management
		Cookie cookie = new Cookie(name, value);
		String contextPath = VaadinService.getCurrentRequest().getContextPath();
		if (contextPath == null || "".equals(contextPath)) {
			contextPath = "/";
		}
		cookie.setPath(contextPath);
		cookie.setMaxAge(value != null ? Integer.MAX_VALUE : 0);
		VaadinService.getCurrentResponse().addCookie(cookie);
	}

	@Override
	public void removeCookie(String name) {
		setCookie(name, null);
	}

}
