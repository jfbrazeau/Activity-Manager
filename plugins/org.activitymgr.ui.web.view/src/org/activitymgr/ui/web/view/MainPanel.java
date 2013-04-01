package org.activitymgr.ui.web.view;

import org.activitymgr.ui.web.logic.IGenericCallback;
import org.activitymgr.ui.web.logic.IRootLogic;
import org.activitymgr.ui.web.view.dialogs.YesNoDialog;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

@SuppressWarnings("serial")
public class MainPanel extends VerticalLayout implements IRootLogic.View {

	@SuppressWarnings("unused")
	private IRootLogic logic;
	
	@SuppressWarnings("unused")
	private ResourceCache resourceCache;

	public MainPanel(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		VerticalSplitPanel vl = new VerticalSplitPanel();
		addComponent(vl);

		Button button = new Button("Click me !");
		vl.addComponent(button);
		button.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				showNotification("You clicked !");
			}
		});
		
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

}
