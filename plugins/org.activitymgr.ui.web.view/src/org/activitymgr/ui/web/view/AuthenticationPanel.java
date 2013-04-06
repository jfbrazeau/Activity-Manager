package org.activitymgr.ui.web.view;

import javax.servlet.http.Cookie;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class AuthenticationPanel extends GridLayout implements
		IAuthenticationLogic.View {

	private static final String NAME_COOKIE = "name";

	private IAuthenticationLogic logic;

	@SuppressWarnings("unused")
	private ResourceCache resourceCache;

	public AuthenticationPanel(ResourceCache resourceCache, VaadinRequest request) {
		super(1, 1);
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);

		// Cookie retrieval
		String defaultUser = null;
		Cookie nameCookie = getCookieByName(request, NAME_COOKIE);
		if (nameCookie != null) {
			defaultUser = nameCookie.getValue();
		}
		
		
		// Form panel
		GridLayout formPanel = new GridLayout(3, 4);
		formPanel.setMargin(true);
		formPanel.setSpacing(true);
		addComponent(formPanel);
		setComponentAlignment(formPanel, Alignment.MIDDLE_CENTER);
		
		/* Line 1 */

		// User field
		Label userLabel = new Label("User");
		formPanel.addComponent(userLabel);
		formPanel.setComponentAlignment(userLabel, Alignment.MIDDLE_RIGHT);
		final TextField userField = new TextField();
		if (defaultUser != null) {
			userField.setValue(defaultUser);
		}
		formPanel.addComponent(userField);
		formPanel.addComponent(new Label(""));
		
		/* Line 2 */

		// Password field
		Label passwordLabel = new Label("Password");
		formPanel.addComponent(passwordLabel);
		formPanel.setComponentAlignment(passwordLabel, Alignment.MIDDLE_RIGHT);
		final PasswordField passwordField = new PasswordField();
		formPanel.addComponent(passwordField);

		// Button
		Button validateButton = new Button("Login");
		formPanel.addComponent(validateButton);
		
		/* Line 3 */

		// Remember me
		formPanel.addComponent(new Label(""));
		final CheckBox rememberMeCheckBox = new CheckBox("Remember me");
		rememberMeCheckBox.setValue(defaultUser != null);
		formPanel.addComponent(rememberMeCheckBox);

		// Register listeners
		validateButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				// Cookie management
				Cookie cookie = new Cookie(NAME_COOKIE, userField.getValue());
				String contextPath = VaadinService.getCurrentRequest().getContextPath();
				if (contextPath == null || "".equals(contextPath)) {
					contextPath = "/";
				}
				cookie.setPath(contextPath);
				cookie.setMaxAge(rememberMeCheckBox.getValue() ? Integer.MAX_VALUE : 0);
				VaadinService.getCurrentResponse().addCookie(cookie);
				
				// Authentication
				logic.onAuthenticate(userField.getValue(),
						passwordField.getValue());
			}
		});
		
		// Default focus management
		if (defaultUser == null) {
			userField.focus();
		}
		else {
			passwordField.focus();
		}
	}
	
	@Override
	public void registerLogic(IAuthenticationLogic logic) {
		this.logic = logic;
	}

	private Cookie getCookieByName(VaadinRequest request, String name) {
		System.out.println("getCookieByName");
		// Fetch all cookies from the request
		Cookie[] cookies = request.getCookies();

		// Iterate to find cookie by its name
		for (Cookie cookie : cookies) {
			System.out.println("** cookie : " + cookie.getName() + "-" + cookie.getValue() + "-" + cookie.getPath());
			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}

		return null;
	}
}
