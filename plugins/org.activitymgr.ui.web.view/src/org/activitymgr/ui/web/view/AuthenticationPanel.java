package org.activitymgr.ui.web.view;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class AuthenticationPanel extends GridLayout implements IAuthenticationLogic.View {

	private IAuthenticationLogic logic;
	
	@SuppressWarnings("unused")
	private ResourceCache resourceCache;

	public AuthenticationPanel(ResourceCache resourceCache) {
		super(1, 1);
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);

		GridLayout formPanel = new GridLayout(2, 3);
		formPanel.setMargin(true);
		formPanel.setSpacing(true);
		formPanel.addComponent(new Label("User"));
		final TextField userField = new TextField();
		formPanel.addComponent(userField);
		formPanel.addComponent(new Label("Password"));
		final PasswordField passwordField = new PasswordField();
		formPanel.addComponent(passwordField);
		formPanel.addComponent(new Label(""));
		Button validateButton = new Button("Login");
		formPanel.addComponent(validateButton);
		addComponent(formPanel);
		setComponentAlignment(formPanel, Alignment.MIDDLE_CENTER);
		
		validateButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				System.out.println("Validate");
				logic.onAuthenticate(userField.getValue(), passwordField.getValue());
			}
		});
		userField.focus();
	}

	@Override
	public void registerLogic(IAuthenticationLogic logic) {
		System.out.println(logic);
		this.logic = logic;
	}
	
}
