package org.activitymgr.ui.web.view.impl.internal;

import java.util.Date;

import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.view.AbstractTabPanel;
import org.activitymgr.ui.web.view.impl.dialogs.PopupDateFieldWithParser;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class ReportsPanel extends AbstractTabPanel<IReportsTabLogic> implements IReportsTabLogic.View {

	private OptionGroup intervalUnitGroup;
	private OptionGroup intervalBoundsModeGroup;
	private PopupDateFieldWithParser startDateField;
	private PopupDateFieldWithParser endDateField;

	@Override
	protected Component createBodyComponent() {
		GridLayout v = new GridLayout(2, 16);
		//v.setMargin(true);
		v.setSpacing(true);
		v.setWidth("850px");
		
		createIntervalConfigurationPanel(v);
		createTaskManagementPanel(v);
		createCollaboratorsManagementPanel(v);
		createOrderManagementPanel(v);
		return v;
	}

	private void createIntervalConfigurationPanel(GridLayout gl) {
		addTitle(gl, "Interval configuration", 0);

		gl.addComponent(new Label("Interval unit :"));
		intervalUnitGroup = new OptionGroup();
		intervalUnitGroup.setImmediate(true);
		intervalUnitGroup.setStyleName("horizontal");
		gl.addComponent(intervalUnitGroup);
		
		gl.addComponent(new Label("Interval bounds mode :"));
		intervalBoundsModeGroup = new OptionGroup();
		intervalBoundsModeGroup.setImmediate(true);
		intervalBoundsModeGroup.setStyleName("horizontal");
		gl.addComponent(intervalBoundsModeGroup);

		gl.addComponent(new Label("Interval bounds :"));
		HorizontalLayout intervalBoundsPanel = new HorizontalLayout();
		gl.addComponent(intervalBoundsPanel);
		startDateField = newDateField();
		intervalBoundsPanel.addComponent(startDateField);
		endDateField = newDateField();
		intervalBoundsPanel.addComponent(endDateField);
		intervalBoundsPanel.addComponent(new Label("(ignored if automatic mode is selected)"));

		intervalUnitGroup.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onIntervalTypeChanged(event.getProperty().getValue());
			}
		});
		intervalBoundsModeGroup.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onIntervalBoundsModeChanged(event.getProperty().getValue());
			}
		});
		ValueChangeListener dateBoundsChangeListener = new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				getLogic().onIntervalBoundsChanged(startDateField.getValue(), endDateField.getValue());
			}
		};
		startDateField.addValueChangeListener(dateBoundsChangeListener);
		endDateField.addValueChangeListener(dateBoundsChangeListener);
	}

	private void createTaskManagementPanel(GridLayout gl) {
		addTitle(gl, "Task management", 4);
		
		CheckBox limitTaskScopeCheckbox = new CheckBox("Limit task scope to :");
		gl.addComponent(limitTaskScopeCheckbox);
		HorizontalLayout taskScopePanel = new HorizontalLayout();
		gl.addComponent(taskScopePanel);
		TextField taskPathScopeTextField = new TextField();
		taskPathScopeTextField.setWidth("300px");
		taskScopePanel.addComponent(taskPathScopeTextField);
		Button browseTaskButton = new Button("...");
		taskScopePanel.addComponent(browseTaskButton);
		
		CheckBox declineResultsetByTaskCheckbox = new CheckBox("Decline resultset by task");
		gl.addComponent(declineResultsetByTaskCheckbox, 0, 6, 1, 6);
		
		gl.addComponent(new Label("Task tree depth :"));
		HorizontalLayout taskDepthLayout = new HorizontalLayout();
		gl.addComponent(taskDepthLayout);
		Button decreaseTaskDepthButton = new Button("-");
		taskDepthLayout.addComponent(decreaseTaskDepthButton);
		TextField taskDepthTextField = new TextField();
		taskDepthTextField.setWidth("40px");
		taskDepthLayout.addComponent(taskDepthTextField);
		Button increaseTaskDepthButton = new Button("+");
		taskDepthLayout.addComponent(increaseTaskDepthButton);
		taskDepthLayout.addComponent(new Label("(deeper contributions will be aggregated)"));
		
		gl.addComponent(new Label("Columns to include :"));
		ListSelect taskColumnstoInclude = new ListSelect();
		taskColumnstoInclude.setImmediate(true);
		taskColumnstoInclude.setMultiSelect(true);
		taskColumnstoInclude.setWidth("300px");
		taskColumnstoInclude.addItems("path", "code", "name", "comment", "budget", "initially consumed", "etc");
		taskColumnstoInclude.setHeight("70px");
		gl.addComponent(taskColumnstoInclude);
		
	}

	private void createCollaboratorsManagementPanel(GridLayout gl) {
		addTitle(gl, "Collaborators management", 9);
	
		CheckBox declineResultsetByContributorCheckbox = new CheckBox("Decline resultset by contributor");
		gl.addComponent(declineResultsetByContributorCheckbox, 0, 10, 1, 10);

		gl.addComponent(new Label("Columns to include :"));
		ListSelect contributorColumnstoInclude = new ListSelect();
		contributorColumnstoInclude.setImmediate(true);
		contributorColumnstoInclude.setMultiSelect(true);
		contributorColumnstoInclude.addItems("login", "first name", "last name", "active");
		contributorColumnstoInclude.setWidth("300px");
		contributorColumnstoInclude.setHeight("70px");
		gl.addComponent(contributorColumnstoInclude);

	}

	private void createOrderManagementPanel(GridLayout gl) {
		addTitle(gl, "Order management", 12);

		gl.addComponent(new Label("Report mode :"));
		OptionGroup reportModeUnitGroup = new OptionGroup();
		reportModeUnitGroup.setImmediate(true);
		reportModeUnitGroup.addItems("Task centric", "Collaborator centric");
		reportModeUnitGroup.setStyleName("horizontal");
		gl.addComponent(reportModeUnitGroup);

		gl.addComponent(new Label("Columns order mode :"));
		OptionGroup columnsOrderModeUnitGroup = new OptionGroup();
		columnsOrderModeUnitGroup.setImmediate(true);
		columnsOrderModeUnitGroup.addItems("Automatic", "Manual");
		columnsOrderModeUnitGroup.setStyleName("horizontal");
		gl.addComponent(columnsOrderModeUnitGroup);
		
		gl.addComponent(new Label("Columns order :"));
		ListSelect columnsOrderInclude = new ListSelect();
		columnsOrderInclude.setImmediate(true);
		columnsOrderInclude.addItems("login (collaborator)", "first name (collaborator)", "last name (collaborator)", "active (collaborator)");
		columnsOrderInclude.setNullSelectionAllowed(false);
		columnsOrderInclude.setWidth("300px");
		columnsOrderInclude.setHeight("100px");
		gl.addComponent(columnsOrderInclude);

	}

	private void addTitle(GridLayout gl, String caption, int row) {
		Label label = new Label("<b>" + caption + "</b><hr>", ContentMode.HTML);
		label.setWidth("100%");
		gl.addComponent(label, 0, row, 1, row);
		
	}

	private PopupDateFieldWithParser newDateField() {
		PopupDateFieldWithParser startDateField = new PopupDateFieldWithParser();
		startDateField.setImmediate(true);
		startDateField.setDateFormat("E dd/MM/yyyy");
		startDateField.setShowISOWeekNumbers(true);
		startDateField.setStyleName("monday-date-field");
		startDateField.setValue(new Date());
		return startDateField;
	}

	@Override
	public void addIntervalTypeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalUnitGroup, id, label);
	}
	
	@Override
	public void selectIntervalTypeRadioButton(Object id) {
		intervalUnitGroup.setValue(id);
	}

	@Override
	public void addIntervalBoundsModeRadioButton(Object id, String label) {
		addCheckboxToGroup(intervalBoundsModeGroup, id, label);
	}

	@Override
	public void selectIntervalBoundsModeButton(Object id) {
		intervalBoundsModeGroup.setValue(id);
	}

	private static void addCheckboxToGroup(OptionGroup group, Object id, String label) {
		group.addItem(id);
		group.setItemCaption(id, label);
	}

	@Override
	public void setIntervalBoundsModeEnablement(boolean startDateEnablement, boolean endDateEnablement) {
		startDateField.setEnabled(startDateEnablement);
		endDateField.setEnabled(endDateEnablement);
	}
 
	@Override
	public void setIntervalBounds(Date startDate, Date endDate) {
		startDateField.setValue(startDate);
		endDateField.setValue(endDate);
	}

}
