package org.activitymgr.ui.web.view;

import java.util.Calendar;
import java.util.Date;

import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.event.MouseEvents;
import com.vaadin.ui.Button;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsLogic.View, Button.ClickListener {

	@SuppressWarnings("unused")
	private IContributionsLogic logic;
	
	private ResourceCache resourceCache;

	private DateField dateField;

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	public ContributionsPanel(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		/*
		 * Controls
		 */
		GridLayout controlsLayout = new GridLayout(7, 1);
		addComponent(controlsLayout);
		previousYearButton = new Button("<<< Year");
		controlsLayout.addComponent(previousYearButton);
		previousMonthButton = new Button("<< Month");
		controlsLayout.addComponent(previousMonthButton);
		previousWeekButton = new Button("< Week");
		controlsLayout.addComponent(previousWeekButton);
		
		dateField = new DateField();
		dateField.setDateFormat("EEE dd/MM/yyyy");
		dateField.setStyleName("mondayDateField");
		controlsLayout.addComponent(dateField);
		
		nextWeekButton = new Button("Week >");
		controlsLayout.addComponent(nextWeekButton);
		nextMonthButton = new Button("Month >>");
		controlsLayout.addComponent(nextMonthButton);
		nextYearButton = new Button("Year >>>");
		controlsLayout.addComponent(nextYearButton);

		/*
		 * Table
		 */
		GridLayout contributionsLayout = new GridLayout(10, 2);
		addComponent(contributionsLayout);
		
		// First line
		contributionsLayout.addComponent(new Label("Path"));
		contributionsLayout.addComponent(new Label("Task"));
		Label day1Label = new Label("D1");
		contributionsLayout.addComponent(day1Label);
		Label day2Label = new Label("D2");
		contributionsLayout.addComponent(day2Label);
		Label day3Label = new Label("D3");
		contributionsLayout.addComponent(day3Label);
		Label day4Label = new Label("D4");
		contributionsLayout.addComponent(day4Label);
		Label day5Label = new Label("D5");
		contributionsLayout.addComponent(day5Label);
		Label day6Label = new Label("D6");
		contributionsLayout.addComponent(day6Label);
		Label day7Label = new Label("D7");
		contributionsLayout.addComponent(day7Label);
		contributionsLayout.addComponent(new Label("Total"));
		
		// Last line
		Button addTaskButton = new Button("Add");
		contributionsLayout.addComponent(addTaskButton, 0, 1, 1, 1);
		Label day1TotalLabel = new Label("T1");
		contributionsLayout.addComponent(day1TotalLabel);
		Label day2TotalLabel = new Label("T2");
		contributionsLayout.addComponent(day2TotalLabel);
		Label day3TotalLabel = new Label("T3");
		contributionsLayout.addComponent(day3TotalLabel);
		Label day4TotalLabel = new Label("T4");
		contributionsLayout.addComponent(day4TotalLabel);
		Label day5TotalLabel = new Label("T5");
		contributionsLayout.addComponent(day5TotalLabel);
		Label day6TotalLabel = new Label("T6");
		contributionsLayout.addComponent(day6TotalLabel);
		Label day7TotalLabel = new Label("T7");
		contributionsLayout.addComponent(day7TotalLabel);
		Label totalLabel = new Label("TT");
		contributionsLayout.addComponent(totalLabel);
		
		// Register listeners
		previousYearButton.addClickListener(this);
		previousMonthButton.addClickListener(this);
		previousWeekButton.addClickListener(this);
		nextWeekButton.addClickListener(this);
		nextMonthButton.addClickListener(this);
		nextYearButton.addClickListener(this);
		/* TODO to remove */
		
//		VerticalSplitPanel vl = new VerticalSplitPanel();
//		addComponent(vl);

		
		contributionsLayout.insertRow(1);
		contributionsLayout.addComponent(new Label("Hoho"), 0, 1);
		
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

	@Override
	public void setDate(Calendar monday) {
		dateField.setValue(monday.getTime());
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == previousYearButton) {
			logic.onPreviousYear();
		} else if (event.getSource() == previousMonthButton) {
			logic.onPreviousMonth();
		} else if (event.getSource() == previousWeekButton) {
			logic.onPreviousWeek();
		} else if (event.getSource() == nextWeekButton) {
			logic.onNextWeek();
		} else if (event.getSource() == nextMonthButton) {
			logic.onNextMonth();
		} else if (event.getSource() == nextYearButton) {
			logic.onNextYear();
		}
	}

}
