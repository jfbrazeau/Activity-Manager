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

	private String[] durationLabels;

	private Label[] dayLabels;

	private GridLayout contributionsContainer;

	public ContributionsPanel(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;

		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		/*
		 * Controls
		 */
		GridLayout controlsContainer = new GridLayout(7, 1);
		addComponent(controlsContainer);
		previousYearButton = new Button("<<< Year");
		controlsContainer.addComponent(previousYearButton);
		previousMonthButton = new Button("<< Month");
		controlsContainer.addComponent(previousMonthButton);
		previousWeekButton = new Button("< Week");
		controlsContainer.addComponent(previousWeekButton);
		
		dateField = new DateField();
		dateField.setDateFormat("EEE dd/MM/yyyy");
		dateField.setStyleName("mondayDateField");
		controlsContainer.addComponent(dateField);
		
		nextWeekButton = new Button("Week >");
		controlsContainer.addComponent(nextWeekButton);
		nextMonthButton = new Button("Month >>");
		controlsContainer.addComponent(nextMonthButton);
		nextYearButton = new Button("Year >>>");
		controlsContainer.addComponent(nextYearButton);

		/*
		 * Contributions table
		 */
		contributionsContainer = new GridLayout(10, 2);
		addComponent(contributionsContainer);

		// First line
		Label pathLabel = new Label("Path");
		pathLabel.setWidth(200, Unit.PIXELS);
		contributionsContainer.addComponent(pathLabel);
		Label taskNameLabel = new Label("Task");
		taskNameLabel.setWidth(200, Unit.PIXELS);
		contributionsContainer.addComponent(taskNameLabel);
		Label day1Label = new Label("D1");
		day1Label.setWidth(80, Unit.PIXELS);
		contributionsContainer.addComponent(day1Label);
		Label day2Label = new Label("D2");
		contributionsContainer.addComponent(day2Label);
		Label day3Label = new Label("D3");
		contributionsContainer.addComponent(day3Label);
		Label day4Label = new Label("D4");
		contributionsContainer.addComponent(day4Label);
		Label day5Label = new Label("D5");
		contributionsContainer.addComponent(day5Label);
		Label day6Label = new Label("D6");
		contributionsContainer.addComponent(day6Label);
		Label day7Label = new Label("D7");
		contributionsContainer.addComponent(day7Label);
		contributionsContainer.addComponent(new Label("Total"));
		dayLabels = new Label[] { day1Label, day2Label, day3Label,
				day4Label, day5Label, day6Label, day7Label };
		for (Label dayLabel : dayLabels) {
			dayLabel.setWidth(80, Unit.PIXELS);
		}
		
		// Last line
		//Button addTaskButton = new Button("Add");
		//contributionsContainer.addComponent(addTaskButton, 0, 1, 1, 1);
		contributionsContainer.addComponent(new Label(""), 0, 1, 1, 1);

		Label day1TotalLabel = new Label("T1");
		contributionsContainer.addComponent(day1TotalLabel);
		Label day2TotalLabel = new Label("T2");
		contributionsContainer.addComponent(day2TotalLabel);
		Label day3TotalLabel = new Label("T3");
		contributionsContainer.addComponent(day3TotalLabel);
		Label day4TotalLabel = new Label("T4");
		contributionsContainer.addComponent(day4TotalLabel);
		Label day5TotalLabel = new Label("T5");
		contributionsContainer.addComponent(day5TotalLabel);
		Label day6TotalLabel = new Label("T6");
		contributionsContainer.addComponent(day6TotalLabel);
		Label day7TotalLabel = new Label("T7");
		contributionsContainer.addComponent(day7TotalLabel);
		Label totalLabel = new Label("TT");
		contributionsContainer.addComponent(totalLabel);
		
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
	public void setDate(Calendar date) {
		dateField.setValue(date.getTime());
	}

	@Override
	public void setDurationLabels(String[] durationLabels) {
		this.durationLabels = durationLabels;
	}

	@Override
	public void setDayLabels(String[] dayLabels) {
		for (int i=0; i<dayLabels.length ; i++) {
			this.dayLabels[i].setValue(dayLabels[i]);
		}
	}

	@Override
	public void addWeekContribution(final String taskCodePath, String name,
			int[] durationIndexes) {
		int row = contributionsContainer.getRows() - 1;
		System.out.println("Total rows = " + contributionsContainer.getRows());
		contributionsContainer.insertRow(row);

		// Task path & name
		contributionsContainer.addComponent(new Label(taskCodePath), 0, row);
		contributionsContainer.addComponent(new Label(name), 1, row);
		
		// Duration forms
		for (int i=0; i<durationIndexes.length; i++) {
			int durationIndex = durationIndexes[i];
			HorizontalLayout hl = new HorizontalLayout();
			contributionsContainer.addComponent(hl, i+2, row);
			// Add clickable images
			for (int j=0; j<durationLabels.length; j++) {
				final Image image = new Image();
				image.setSource(resourceCache.getResource(ResourceCache.ONE_PIXEL_ICON));
				image.setHeight("16px");
				image.setWidth("7px");
				image.setStyleName(durationIndex >= j ? "green" : "red");
				image.setDescription(durationLabels[j]);
				hl.addComponent(image);
				// Separator
				Image sep = new Image();
				sep.setSource(resourceCache.getResource(ResourceCache.ONE_PIXEL_ICON));
				hl.addComponent(sep);
				// Register listener
				final int durationIdx = j;
				final int dayOfWeek = i;
				image.addClickListener(new MouseEvents.ClickListener() {
					@Override
					public void click(com.vaadin.event.MouseEvents.ClickEvent event) {
						logic.onDurationClicked(taskCodePath, dayOfWeek, durationIdx);
					}
				});
			}
			// TODO factoriser
			hl.addComponent(new Label(durationIndex >= 0 ? durationLabels[durationIndex] : ""));
		}
	}

	@Override
	public void updateDurationIndex(String taskCodePath, int dayOfWeek,
			int durationIdx) {
		int row = 1;
		// Retrieve row index
		while (row < contributionsContainer.getRows() - 1) {
			Label pathLabel = (Label) contributionsContainer.getComponent(0, row);
			if (taskCodePath.equals(pathLabel.getValue())) {
				break;
			}
			row ++;
		}
		// Retrieve clickable images container
		HorizontalLayout hl = (HorizontalLayout) contributionsContainer.getComponent(2+dayOfWeek, row);
		for (int j=0; j<durationLabels.length; j++) {
			Image image = (Image) hl.getComponent(j*2);
			image.setStyleName(durationIdx >= j ? "green" : "red");
		}
		// TODO factoriser
		Label label = (Label) hl.getComponent(hl.getComponentCount()-1);
		label.setValue(durationIdx >= 0 ? durationLabels[durationIdx] : "");
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
		else {
			throw new IllegalArgumentException("Unexpected button click");
		}
	}

}
