package org.activitymgr.ui.web.view.impl.dialogs;

import org.activitymgr.ui.web.logic.IExternalContentDialogLogic;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ExternalContentDialog extends AbstractDialog implements
		IExternalContentDialogLogic.View, ClickListener {

	@SuppressWarnings("unused")
	private IExternalContentDialogLogic logic;
	private BrowserFrame frame;

	public ExternalContentDialog() {
		super("External content");
		setModal(true);
		setImmediate(true);

		// Global layout
		VerticalLayout contentLayout = new VerticalLayout();
		contentLayout.setSizeFull();
		contentLayout.setMargin(new MarginInfo(true, true, true, true));
		contentLayout.setSpacing(true);
		setContent(contentLayout);

		// External content
		frame = new BrowserFrame();
		frame.setImmediate(true);
		frame.setSizeFull();
		contentLayout.addComponent(frame);

		// Ok button
		Button closeButton = new Button("Close", this);
		closeButton.setImmediate(true);
		contentLayout.addComponent(closeButton);
		contentLayout.setComponentAlignment(closeButton, Alignment.TOP_CENTER);

		// Set expand ratios
		contentLayout.setExpandRatio(frame, 95);
		contentLayout.setExpandRatio(closeButton, 5);

		addAttachListener(new AttachListener() {
			@Override
			public void attach(AttachEvent event) {
				UI ui = getUI();
				WebBrowser webBrowser = ui.getPage().getWebBrowser();
				setWidth((int) (webBrowser.getScreenWidth() * 0.9), Unit.PIXELS);
				setHeight((int) (webBrowser.getScreenHeight() * 0.7),
						Unit.PIXELS);
				ExternalContentDialog.this.removeAttachListener(this);
			}
		});
	}

	@Override
	public void setTitle(String title) {
		setCaption(title);
	}

	@Override
	public void registerLogic(IExternalContentDialogLogic logic) {
		this.logic = logic;
	}

	@Override
	public void setContentUrl(String url) {
		frame.setSource(new ExternalResource(url));
	}

	@Override
	public void buttonClick(ClickEvent event) {
		close();
	}

}
