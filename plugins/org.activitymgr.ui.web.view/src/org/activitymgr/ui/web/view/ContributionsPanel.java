package org.activitymgr.ui.web.view;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.MouseEvents;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsLogic.View, Button.ClickListener {

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

		//setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		/*
		 * Controls
		 */
		GridLayout controlsContainer = new GridLayout(8, 1);
		addComponent(controlsContainer);
		Label emptyLabel = new Label();
		emptyLabel.setWidth(330, Unit.PIXELS);
		controlsContainer.addComponent(emptyLabel);
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
		contributionsContainer.addStyleName("contribution-table");
		contributionsContainer.setSpacing(true);
		addComponent(contributionsContainer);

		// First line
		Label pathLabel = new Label("Path");
		pathLabel.setWidth(150, Unit.PIXELS);
		contributionsContainer.addComponent(pathLabel);
		Label taskNameLabel = new Label("Task");
		taskNameLabel.setWidth(150, Unit.PIXELS);
		contributionsContainer.addComponent(taskNameLabel);
		dayLabels = new Label[7];
		for (int i=0; i<7; i++) {
			Label dayLabel = new Label("D" + (i+1));
			dayLabel.addStyleName("day-labels");
			contributionsContainer.addComponent(dayLabel);
			contributionsContainer.setComponentAlignment(dayLabel, Alignment.MIDDLE_RIGHT);
			dayLabel.setWidth(80, Unit.PIXELS);
			dayLabels[i] = dayLabel;
		}
		Label totalColumnLabel = new Label("Total");
		totalColumnLabel.addStyleName("day-labels");
		contributionsContainer.addComponent(totalColumnLabel);
		contributionsContainer.setComponentAlignment(totalColumnLabel, Alignment.MIDDLE_RIGHT);

		// Last line
		Button addTaskButton = new Button("Add");
		contributionsContainer.addComponent(addTaskButton, 0, 1, 1, 1);
		//contributionsContainer.addComponent(new Label(""), 0, 1, 1, 1);
		for (int i=0; i<7; i++) {
			Label totalLabel = newAmountLabel("T" + (i+1));
			contributionsContainer.addComponent(totalLabel);
			contributionsContainer.setComponentAlignment(totalLabel, Alignment.MIDDLE_RIGHT);
		}
		Label totalLabel = newAmountLabel("TT");
		contributionsContainer.addComponent(totalLabel);
		contributionsContainer.setComponentAlignment(totalLabel, Alignment.MIDDLE_RIGHT);
		
		// Register listeners
		previousYearButton.addClickListener(this);
		previousMonthButton.addClickListener(this);
		previousWeekButton.addClickListener(this);
		nextWeekButton.addClickListener(this);
		nextMonthButton.addClickListener(this);
		nextYearButton.addClickListener(this);
		dateField.setImmediate(true);
		dateField.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				Calendar cal = new GregorianCalendar();
				cal.setTime(dateField.getValue());
				logic.onDateChange(cal);
			}
		});
	}

	private static Label newAmountLabel(String caption) {
		Label amountLabel = new Label(caption);
		amountLabel.setStyleName("amount");
		amountLabel.setWidth(40, Unit.PIXELS);
		return amountLabel;
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
		contributionsContainer.insertRow(row);

		// Task path & name
		contributionsContainer.addComponent(new Label(taskCodePath), 0, row);
		contributionsContainer.addComponent(new Label(name), 1, row);
		
		// Duration forms
		for (int i=0; i<durationIndexes.length; i++) {
			int durationIndex = durationIndexes[i];
			HorizontalLayout hl = new HorizontalLayout();
			contributionsContainer.addComponent(hl, i+2, row);
			contributionsContainer.setComponentAlignment(hl, Alignment.MIDDLE_RIGHT);
			// Add clickable images
			for (int j=0; j<durationLabels.length; j++) {
				final Image image = new Image();
				image.setSource(resourceCache.getResource(ResourceCache.ONE_PIXEL_ICON));
				image.setHeight("16px");
				image.setWidth("7px");
				image.setStyleName(durationIndex >= j ? "duration-on" : "duration-off");
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
			hl.addComponent(newAmountLabel(durationIndex >= 0 ? durationLabels[durationIndex] : ""));
		}
		Label totalLabel = newAmountLabel("TT");
		contributionsContainer.addComponent(totalLabel, 9, row);
		contributionsContainer.setComponentAlignment(totalLabel, Alignment.MIDDLE_RIGHT);
	}

	@Override
	public void removeAllWeekContributions() {
		while (contributionsContainer.getRows() != 2) {
			contributionsContainer.removeRow(1);
		}
	}

	@Override
	public void updateDurationIndex(String taskCodePath, int dayOfWeek,
			int durationIdx) {
		int row = findRow(taskCodePath);
		// Retrieve clickable images container
		HorizontalLayout hl = (HorizontalLayout) contributionsContainer.getComponent(2+dayOfWeek, row);
		for (int j=0; j<durationLabels.length; j++) {
			Image image = (Image) hl.getComponent(j*2);
			image.setStyleName(durationIdx >= j ? "duration-on" : "duration-off");
		}
		// TODO factoriser
		Label label = (Label) hl.getComponent(hl.getComponentCount()-1);
		label.setValue(durationIdx >= 0 ? durationLabels[durationIdx] : "");
	}

	@Override
	public void setDayTotal(int dayOfWeek, String total) {
		Label label = (Label) contributionsContainer.getComponent(2 + dayOfWeek, contributionsContainer.getRows() - 1);
		label.setValue(total);
	}

	@Override
	public void setTotal(String total) {
		Label label = (Label) contributionsContainer.getComponent(9, contributionsContainer.getRows() - 1);
		label.setValue(total);
	}

	@Override
	public void setTaskTotal(String taskCodePath, String total) {
		int row = findRow(taskCodePath);
		Label label = (Label) contributionsContainer.getComponent(9, row);
		label.setValue(total);
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

	private int findRow(String taskCodePath) {
		int row = 1;
		// Retrieve row index
		while (row < contributionsContainer.getRows() - 1) {
			Label pathLabel = (Label) contributionsContainer.getComponent(0, row);
			if (taskCodePath.equals(pathLabel.getValue())) {
				break;
			}
			row ++;
		}
		return row;
	}

}
