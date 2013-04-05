package org.activitymgr.ui.web.view;

import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.event.MouseEvents;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsLogic.View {

	@SuppressWarnings("unused")
	private IContributionsLogic logic;
	
	private ResourceCache resourceCache;

	public ContributionsPanel(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
//		VerticalSplitPanel vl = new VerticalSplitPanel();
//		addComponent(vl);

		HorizontalLayout hl = new HorizontalLayout();
		addComponent(hl);
		
		Image image = new Image();
		configure(image);
		image.setStyleName("green");
		image.setDescription("1H");
		hl.addComponent(image);

		Image image1 = new Image();
		configure(image1);
		image1.setStyleName("red");
		image1.setDescription("2H");
		hl.addComponent(image1);
		
		final Label label = new Label();
		hl.addComponent(label);
		
		
		MouseEvents.ClickListener clickListener = new MouseEvents.ClickListener() {
			@Override
			public void click(com.vaadin.event.MouseEvents.ClickEvent event) {
				label.setValue(((Image) event.getSource()).getDescription());
			}
		};
		image.addClickListener(clickListener);
		image1.addClickListener(clickListener);
		
		DateField dateField = new DateField();
		dateField.setDateFormat("dd/MM/yyyy");
		dateField.setStyleName("mondayDateField");
		addComponent(dateField);
	}

	private void configure(Image image) {
		image.setSource(resourceCache.getResource(ResourceCache.ONE_PIXEL_ICON));
		image.setHeight("16px");
		image.setWidth("7px");
	}
	
	@Override
	public void registerLogic(IContributionsLogic logic) {
		this.logic = logic;
	}

}
