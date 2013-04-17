package org.activitymgr.ui.web.view;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.activitymgr.ui.web.logic.IContributionsLogic;
import org.activitymgr.ui.web.view.util.ResourceCache;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ContributionsPanel extends VerticalLayout implements IContributionsLogic.View, Button.ClickListener {

	private IContributionsLogic logic;
	
	@SuppressWarnings("unused")
	private ResourceCache resourceCache;

	private DateField dateField;

	private Button previousYearButton;

	private Button previousMonthButton;

	private Button previousWeekButton;

	private Button nextWeekButton;

	private Button nextMonthButton;

	private Button nextYearButton;

	private Label[] dayLabels;

	private GridLayout contributionsContainer;

	public ContributionsPanel(ResourceCache resourceCache) {
		this.resourceCache = resourceCache;

		setSpacing(true);
		setMargin(true);
		
		/*
		 * Controls
		 */
		GridLayout controlsContainer = new GridLayout(8, 1);
		addComponent(controlsContainer);
		Label emptyLabel = new Label();
		emptyLabel.setWidth(250, Unit.PIXELS);
		controlsContainer.addComponent(emptyLabel);
		previousYearButton = new Button("<<< Year");
		controlsContainer.addComponent(previousYearButton);
		previousMonthButton = new Button("<< Month");
		controlsContainer.addComponent(previousMonthButton);
		previousWeekButton = new Button("< Week");
		controlsContainer.addComponent(previousWeekButton);
		
		dateField = new DateField();
		dateField.setDateFormat("EEE dd/MM/yyyy");
		dateField.setStyleName("monday-date-field");
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
		contributionsContainer = new GridLayout(11, 2);
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
			dayLabels[i] = dayLabel;
		}
		Label totalColumnLabel = new Label("Total");
		totalColumnLabel.addStyleName("day-labels");
		contributionsContainer.addComponent(totalColumnLabel);
		
		// TODO externalize ? (last column)
		Label previsionnalColumnLabel = new Label("Prev");
		previsionnalColumnLabel.addStyleName("day-labels");
		contributionsContainer.addComponent(previsionnalColumnLabel);

		// Last line
		Button addTaskButton = new Button("Add");
		contributionsContainer.addComponent(addTaskButton, 0, 1, 1, 1);
		for (int i=0; i<7; i++) {
			Label totalLabel = newAmountLabel("T" + (i+1));
			contributionsContainer.addComponent(totalLabel);
		}
		Label totalLabel = newAmountLabel("TT");
		contributionsContainer.addComponent(totalLabel);

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
		addTaskButton.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				System.out.println("addTaskButton");
				logic.onTaskButtonClicked();
			}
		});
	}

	private static Label newAmountLabel(String caption) {
		Label amountLabel = new Label(caption);
		amountLabel.setStyleName("amount");
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
	public void setDayLabels(String[] dayLabels) {
		for (int i=0; i<dayLabels.length ; i++) {
			this.dayLabels[i].setValue(dayLabels[i]);
		}
	}

	@Override
	public void addWeekContribution(final String taskCodePath, String name,
			String[] durations) {
		int row = contributionsContainer.getRows() - 1;
		contributionsContainer.insertRow(row);

		// Task path & name
		contributionsContainer.addComponent(new Label(taskCodePath), 0, row);
		contributionsContainer.addComponent(new Label(name), 1, row);
		
		// Duration forms
		for (int i=0; i<7; i++) {
			final TextField durationTextField = new TextField();
			durationTextField.addStyleName("amount");
			durationTextField.setWidth(60, Unit.PIXELS);
			durationTextField.setImmediate(true);
			durationTextField.setValue(durations != null ? durations[i] : "");
			contributionsContainer.addComponent(durationTextField, 2+i, row);
			// Register listener
			final int dayOfWeek = i;
			durationTextField.addValueChangeListener(new Property.ValueChangeListener() {
				@Override
				public void valueChange(ValueChangeEvent event) {
					System.out.println("** valueChanged");
					logic.onDurationChanged(
							taskCodePath,
							dayOfWeek,
							durationTextField.getValue());
				}
			});
		}
		Label totalLabel = newAmountLabel("TT");
		contributionsContainer.addComponent(totalLabel, 9, row);
		
		// TODO externalize ? 
		Label prevLabel = newAmountLabel("");
		contributionsContainer.addComponent(prevLabel, 10, row);
	}

	@Override
	public void removeAllWeekContributions() {
		while (contributionsContainer.getRows() != 2) {
			contributionsContainer.removeRow(1);
		}
	}

	@Override
	public void updateDuration(String taskCodePath, int dayOfWeek,
			String duration) {
		int row = findRow(taskCodePath);
		// Retrieve clickable images container
		TextField durationTextField = (TextField) contributionsContainer.getComponent(2+dayOfWeek, row);
		durationTextField.setValue(duration);
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
	public void focusOnCell(String taskCodePath, int dayOfWeek) {
		int row = findRow(taskCodePath);
		TextField durationTextField = (TextField) contributionsContainer.getComponent(2+dayOfWeek, row);
		durationTextField.focus();
		durationTextField.selectAll();
	}

	@Override
	public void setTaskWeekPrevision(String taskCodePath, String previsionalWeekDuration) {
		int row = findRow(taskCodePath);
		Label label = (Label) contributionsContainer.getComponent(10, row);
		label.setValue(previsionalWeekDuration);
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
