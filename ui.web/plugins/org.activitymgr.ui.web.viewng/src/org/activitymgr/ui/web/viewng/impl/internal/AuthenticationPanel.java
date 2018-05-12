package org.activitymgr.ui.web.viewng.impl.internal;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;

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

	private CheckBox rememberMeCheckBox;

	private PasswordField passwordField;

	private TextField userField;
	
	public AuthenticationPanel() {
		super(1, 1);

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
		userField = new TextField();
		formPanel.addComponent(userField);
		formPanel.addComponent(new Label(""));
		
		/* Line 2 */

		// Password field
		Label passwordLabel = new Label("Password");
		formPanel.addComponent(passwordLabel);
		formPanel.setComponentAlignment(passwordLabel, Alignment.MIDDLE_RIGHT);
		passwordField = new PasswordField();
		formPanel.addComponent(passwordField);

		// Button
		Button validateButton = new Button("Login");
		formPanel.addComponent(validateButton);
		
		/* Line 3 */

		// Remember me
		formPanel.addComponent(new Label(""));
		rememberMeCheckBox = new CheckBox("Remember me");
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
		userField.focus();
		
		// Register the attach listener
		addAttachListener(new AttachListener() {
			@Override
			public void attach(AttachEvent event) {
				logic.onViewAttached();
			}
		});
	}
	
	@Override
	public void registerLogic(IAuthenticationLogic logic) {
		this.logic = logic;
	}

}
