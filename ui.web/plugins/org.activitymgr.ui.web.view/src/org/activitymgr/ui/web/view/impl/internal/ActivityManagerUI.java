package org.activitymgr.ui.web.view.impl.internal;

import javax.servlet.http.Cookie;

import org.activitymgr.ui.web.logic.ActivityManagerLogic;
import org.activitymgr.ui.web.logic.IGenericCallback;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.view.impl.dialogs.YesNoDialog;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

@Theme("activitymgr")
@SuppressWarnings("serial")
public class ActivityManagerUI extends UI implements IRootLogic.View {

	@SuppressWarnings("unused")
	private IRootLogic logic;
	private Cookie[] cookies;

	@Inject
	private Injector injector;
	
	@Override
	protected void init(VaadinRequest request) {
		// Fetch all cookies from the request
		cookies = request.getCookies();
		// Create the logic
		new ActivityManagerLogic(this, injector);
	}

	@Override
	public void registerLogic(IRootLogic logic) {
		this.logic = logic;
	}

	@Override
	public void showConfirm(String message, IGenericCallback<Boolean> callback) {
		YesNoDialog dialog = new YesNoDialog("Confirmation", message, callback);
		injector.injectMembers(dialog);
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
	public void setContentView(IView<?> view) {
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

	@Override
	public void openWindow(IView<?> view) {
		addWindow((Window) view);

	}

	@Override
	public void openExternalUrl(String url) {
		getUI().getPage().open(url, "_blank");
	}
}
