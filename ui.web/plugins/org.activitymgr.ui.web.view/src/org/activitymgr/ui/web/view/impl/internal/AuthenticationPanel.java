package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.event.ShortcutListener;
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

	private IAuthenticationLogic logic;

	@SuppressWarnings("unused")
	private IResourceCache resourceCache;

	public AuthenticationPanel(IResourceCache resourceCache, String defaultUser) {
		super(1, 1);
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);

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
		formPanel.addComponent(userField);
		if (defaultUser != null) {
			userField.setValue(defaultUser);
		}
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
				// Authentication
				logic.onAuthenticate(userField.getValue(),
						passwordField.getValue(), rememberMeCheckBox.getValue());
			}
		});
		passwordField.addShortcutListener(new ShortcutListener("Authenticate", 13, null) {
			@Override
			public void handleAction(Object sender, Object target) {
				logic.onAuthenticate(userField.getValue(),
						passwordField.getValue(), rememberMeCheckBox.getValue());
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

}
