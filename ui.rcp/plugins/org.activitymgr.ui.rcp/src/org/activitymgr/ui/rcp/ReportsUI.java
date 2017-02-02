/*
 * Copyright (c) 2004-2017, Jean-Francois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.ui.rcp;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.ui.rcp.dialogs.ErrorDialog;
import org.activitymgr.ui.rcp.dialogs.TaskChooserTreeWithHistoryDialog;
import org.activitymgr.ui.rcp.dialogs.TasksChooserDialog;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class ReportsUI {

	private static final String BUDGET_ATTRIBUTE = "budget";

	private static final String NAME_ATTRIBUTE = "name";

	private static final String CODE_ATTRIBUTE = "code";

	private static final String PATH_ATTRIBUTE = "path";

	private static final String IS_ACTIVE_ATTRIBUTE = "is active";

	private static final String LAST_NAME_ATTRIBUTE = "last name";

	private static final String FIRST_NAME_ATTRIBUTE = "first name";

	private static final String LOGIN_ATTRIBUTE = "login";

	/** Logger */
	private static Logger log = Logger.getLogger(ReportsUI.class);

	/** Model manager */
	private IModelMgr modelMgr;
	
	/** Composant parent */
	private Composite parent;

	/** Bean factory */
	private IDTOFactory factory;

	private Button dayButton;

	private Button weekButton;

	private Button monthButton;

	private Button yearButton;

	private Button automaticIntervalBoundsTypeRadio;

	private Button lowerBoundIntervalBoundsTypeRadio;

	private Button bothBoundsIntervalBoundsTypeRadio;

	private DateTime startDateTime;

	private DateTime endDateTime;

	private Map<String, Button> collaboratorFieldButtonsMap;

	private Map<String, Button> taskFieldButtonsMap;

	private Button includeTasksButton;

	private Button filterByTaskCheckbox;

	private Button includeCollaboratorsButton;

	private Button orderByTasksButton;

	private Button orderByCollaboratorsButton;

	private Spinner taskDepthSpinner;

	private Text filterByTaskText;

	private Button filterByTaskSelectButton;

	private TaskChooserTreeWithHistoryDialog taskChooserDialog;
	
	/**
	 * Default constructor intended to be used from within a tab.
	 * 
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 *            @param factory the {@link IDTOFactory DTO factory}.
	 */
	public ReportsUI(TabItem tabItem, IModelMgr modelMgr, IDTOFactory factory) {
		this(tabItem.getParent(), modelMgr, factory);
		tabItem.setControl(parent);
	}

	/**
	 * Default constructor.
	 * 
	 * @param parentComposite
	 *            the parent composite.
	 * @param modelMgr
	 *            the model manager instance.
	 * @param factory
	 *            bean factory.
	 */
	public ReportsUI(Composite parentComposite, final IModelMgr modelMgr, IDTOFactory factory) {
		this.modelMgr = modelMgr;
		this.factory = factory;

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		
		// Report configuration form creation
		Composite cfgParent = new Composite(parent, SWT.NONE);
		cfgParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cfgParent.setLayout(new GridLayout(1, false));
		
		// Interval grop
		Group intervalGroup = new Group(cfgParent, SWT.SHADOW_OUT);
		intervalGroup.setLayout(new GridLayout(2, false));
		intervalGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		intervalGroup.setText("Interval configuration");
		
		new Label(intervalGroup, SWT.NONE).setText("Interval unit :");
		Composite intervalTypeGroup = new Composite(intervalGroup, SWT.NONE);
		intervalTypeGroup.setLayout(new RowLayout());
		dayButton = new Button(intervalTypeGroup, SWT.RADIO);
		dayButton.setText("Day");
		weekButton = new Button(intervalTypeGroup, SWT.RADIO);
		weekButton.setText("Week");
		monthButton = new Button(intervalTypeGroup, SWT.RADIO);
		monthButton.setText("Month");
		yearButton = new Button(intervalTypeGroup, SWT.RADIO);
		yearButton.setText("Year");

		new Label(intervalGroup, SWT.NONE).setText("Interval bounds mode :");
		Composite intervalBoundsTypeGroup = new Composite(intervalGroup, SWT.NONE);
		intervalBoundsTypeGroup.setLayout(new RowLayout());
		automaticIntervalBoundsTypeRadio = new Button(intervalBoundsTypeGroup, SWT.RADIO);
		automaticIntervalBoundsTypeRadio.setText("Automatic");
		lowerBoundIntervalBoundsTypeRadio = new Button(intervalBoundsTypeGroup, SWT.RADIO);
		lowerBoundIntervalBoundsTypeRadio.setText("Lower bound");
		bothBoundsIntervalBoundsTypeRadio = new Button(intervalBoundsTypeGroup, SWT.RADIO);
		bothBoundsIntervalBoundsTypeRadio.setText("Both bounds");

		new Label(intervalGroup, SWT.NONE).setText("Interval bounds :");
		Composite intervalDatesGroup = new Composite(intervalGroup, SWT.NONE);
		intervalDatesGroup.setLayout(new GridLayout(4, false));
		startDateTime = new DateTime(intervalDatesGroup, SWT.DATE | SWT.CALENDAR | SWT.DROP_DOWN);
		endDateTime = new DateTime(intervalDatesGroup, SWT.DATE | SWT.CALENDAR | SWT.DROP_DOWN);

		// Task group
		Group taskGroup = new Group(cfgParent, SWT.SHADOW_OUT);
		taskGroup.setLayout(new GridLayout(3, false));
		taskGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		taskGroup.setText("Task management");
		filterByTaskCheckbox = new Button(taskGroup, SWT.CHECK);
		filterByTaskCheckbox.setText("Limit task scope to root path :");
		filterByTaskText = new Text(taskGroup, SWT.NONE);
		filterByTaskText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterByTaskSelectButton = new Button(taskGroup, SWT.NONE);
		filterByTaskSelectButton.setText("...");
		
		includeTasksButton = new Button(taskGroup, SWT.CHECK);
		includeTasksButton.setText("Include tasks until depth :");
		taskDepthSpinner = new Spinner(taskGroup, SWT.NONE);
		GridData taskDepthSpinnerGridData = new GridData();
		taskDepthSpinnerGridData.horizontalSpan = 2;
		taskDepthSpinner.setLayoutData(taskDepthSpinnerGridData);
		taskDepthSpinner.setMinimum(1);

		new Label(taskGroup, SWT.NONE).setText("Columns to include :");
		Composite taskColumnsGroup = new Composite(taskGroup, SWT.SHADOW_OUT);
		RowLayout taskColumnsGroupLayout = new RowLayout();
		taskColumnsGroupLayout.marginLeft = 0;
		taskColumnsGroup.setLayout(taskColumnsGroupLayout);
		GridData taskColumnsGroupGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		taskColumnsGroupGridData.horizontalSpan = 2;
		taskColumnsGroup.setLayoutData(taskColumnsGroupGridData);
		taskColumnsGroup.setLayout(new GridLayout(4, false));
		taskFieldButtonsMap = buildCheckboxes(taskColumnsGroup, PATH_ATTRIBUTE, CODE_ATTRIBUTE, NAME_ATTRIBUTE, BUDGET_ATTRIBUTE);
		
		// Collaborator group
		Group collaboratorGroup = new Group(cfgParent, SWT.SHADOW_OUT);
		collaboratorGroup.setLayout(new GridLayout(2, false));
		collaboratorGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		collaboratorGroup.setText("Collaborator management");
		includeCollaboratorsButton = new Button(collaboratorGroup, SWT.CHECK);
		includeCollaboratorsButton.setText("Include collaborators");
		new Label(collaboratorGroup, SWT.NONE); // Fake label just to use the grid layout cell

		new Label(collaboratorGroup, SWT.NONE).setText("Columns to include : ");
		Composite collaboratorColumnsGroup = new Composite(collaboratorGroup, SWT.SHADOW_OUT);
		collaboratorColumnsGroup.setLayout(new GridLayout(4, false));
		collaboratorFieldButtonsMap = buildCheckboxes(collaboratorColumnsGroup, LOGIN_ATTRIBUTE, FIRST_NAME_ATTRIBUTE, LAST_NAME_ATTRIBUTE, IS_ACTIVE_ATTRIBUTE);

		Composite orderByAndButtonsPanel = new Composite(cfgParent, SWT.NONE);
		orderByAndButtonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		orderByAndButtonsPanel.setLayout(new GridLayout(2, false));
		
		// Order by group
		Group orderByGroup = new Group(orderByAndButtonsPanel, SWT.NONE);
		orderByGroup.setLayout(new RowLayout());
		orderByGroup.setText("Order by");
		orderByTasksButton = new Button(orderByGroup, SWT.RADIO);
		orderByTasksButton.setText("tasks");
		orderByCollaboratorsButton = new Button(orderByGroup, SWT.RADIO);
		orderByCollaboratorsButton.setText("collaborators");

		// Button
		Button buildReportButton = new Button(orderByAndButtonsPanel, SWT.NONE);
		GridData buildReportButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
		buildReportButtonGridData.horizontalAlignment = SWT.END;
		buildReportButtonGridData.verticalAlignment = SWT.END;
		buildReportButton.setLayoutData(buildReportButtonGridData);
		buildReportButton.setText("Build report");
		
		// Default values
		monthButton.setSelection(true);
		automaticIntervalBoundsTypeRadio.setSelection(true);
		includeTasksButton.setSelection(true);
		includeCollaboratorsButton.setSelection(true);
		orderByTasksButton.setSelection(true);
		endDateTime.setMonth(endDateTime.getMonth()+1);
		taskFieldButtonsMap.get(PATH_ATTRIBUTE).setSelection(true);
		taskFieldButtonsMap.get(CODE_ATTRIBUTE).setSelection(true);
		taskFieldButtonsMap.get(NAME_ATTRIBUTE).setSelection(true);
		collaboratorFieldButtonsMap.get(LOGIN_ATTRIBUTE).setSelection(true);
		
		// Register listeners
		SelectionAdapter buttonListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFieldsEnablement();
			}
		};
		dayButton.addSelectionListener(buttonListener);
		weekButton.addSelectionListener(buttonListener);
		monthButton.addSelectionListener(buttonListener);
		yearButton.addSelectionListener(buttonListener);
		automaticIntervalBoundsTypeRadio.addSelectionListener(buttonListener);
		lowerBoundIntervalBoundsTypeRadio.addSelectionListener(buttonListener);
		bothBoundsIntervalBoundsTypeRadio.addSelectionListener(buttonListener);
		filterByTaskCheckbox.addSelectionListener(buttonListener);
		includeTasksButton.addSelectionListener(buttonListener);
		includeCollaboratorsButton.addSelectionListener(buttonListener);
		
		taskChooserDialog = new TaskChooserTreeWithHistoryDialog(filterByTaskSelectButton.getShell(), modelMgr);
		filterByTaskSelectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				if (taskChooserDialog.open() == Dialog.OK) {
					Task selectedTask = (Task) taskChooserDialog.getValue();
					try {
						String taskCodePath = modelMgr
								.getTaskCodePath(selectedTask);
						filterByTaskText.setText(taskCodePath);
					} catch (ModelException e) {
						new ErrorDialog(parent.getShell(), e.getMessage(), e);
					}
				}
			}
		});
		
		buildReportButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildReport();
			}
		});
		
		// Update fields enablement
		updateFieldsEnablement();
		
	}
	
	private void buildReport() {
		Calendar start = null;
		if (startDateTime.isEnabled()) {
			start = toCalendar(startDateTime);
		}
		ReportIntervalType intervalType = getReportIntervalType();
		Integer intervalCount = null;
		if (endDateTime.isEnabled()) {
			Calendar end = toCalendar(endDateTime);
			switch (intervalType) {
			case YEAR:
				intervalCount = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
				break;
			case MONTH:
				intervalCount = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR))*12 + (end.get(Calendar.MONTH) - start.get(Calendar.MONTH));
				break;
			case WEEK:
				intervalCount = DateHelper.countDaysBetween(start, end)/7;
				break;
			case DAY:
				intervalCount = DateHelper.countDaysBetween(start, end);
			}
		}
		
		try {
			Long rootTaskId = null;
			int taskDepth = 0;
			if (includeTasksButton.getSelection()) {
				taskDepth = taskDepthSpinner.getSelection();
			}
			if (filterByTaskCheckbox.getSelection()) {
				Task selectedTask = modelMgr.getTaskByCodePath(filterByTaskText.getText());
				rootTaskId = selectedTask.getId();
			}
			// Fix included properties
			Collection<String> c = new ArrayList<String>();
			c.add("ee");
			Workbook report = modelMgr.buildReport(start, intervalType, intervalCount, rootTaskId, taskDepth, includeCollaboratorsButton.getSelection(), orderByCollaboratorsButton.getSelection(), c, c);
			File file = File.createTempFile("am-report-", ".xls");
			System.out.println(file);
			FileOutputStream out = new FileOutputStream(file);
			report.write(out);
			out.close();
			Desktop.getDesktop().open(file);
		}
		catch (ModelException e) {
			new ErrorDialog(parent.getShell(), e.getMessage(), e);
		} catch (IOException e) {
			new ErrorDialog(parent.getShell(), e.getMessage(), e);
		}
	}

	private Calendar toCalendar(DateTime dateTime) {
		Calendar start;
		start = Calendar.getInstance();
		start.set(Calendar.YEAR, dateTime.getYear());
		start.set(Calendar.MONTH, dateTime.getMonth());
		start.set(Calendar.DATE, dateTime.getDay());
		start.set(Calendar.HOUR, 12);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		return start;
	}
	
	
	private void updateFieldsEnablement() {
		System.out.println("inchaneg");
		// Update date fields enablement
		if (automaticIntervalBoundsTypeRadio.getSelection()) {
			setEnabled(startDateTime, false);
			setEnabled(endDateTime, false);
		}
		else if (lowerBoundIntervalBoundsTypeRadio.getSelection()) {
			setEnabled(startDateTime, true);
			setEnabled(endDateTime, false);
		}
		else {
			setEnabled(startDateTime, true);
			setEnabled(endDateTime, true);
		}
		
		// Update task group fields enablement
		for (Button b : taskFieldButtonsMap.values()) {
			b.setEnabled(includeTasksButton.getSelection());
		}
		taskDepthSpinner.setEnabled(includeTasksButton.getSelection());
		filterByTaskText.setEnabled(filterByTaskCheckbox.getSelection());
		filterByTaskSelectButton.setEnabled(filterByTaskCheckbox.getSelection());
		
		// Update collaborators group field enablement
		for (Button b : collaboratorFieldButtonsMap.values()) {
			b.setEnabled(includeCollaboratorsButton.getSelection());
		}
		
		// Update order by fields enablement
		boolean orderByEnabled = (includeCollaboratorsButton.getSelection() && includeTasksButton.getSelection());
		orderByTasksButton.setEnabled(orderByEnabled);
		orderByCollaboratorsButton.setEnabled(orderByEnabled);

	}
	
	private void setEnabled(DateTime field, boolean enabled) {
		field.setEnabled(enabled);
		Display display = field.getShell().getDisplay();
		field.setBackground(enabled ? parent.getBackground() : display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
	}
	
	private ReportIntervalType getReportIntervalType() {
		if (dayButton.getSelection()) {
			return ReportIntervalType.DAY;
		}
		else if (weekButton.getSelection()) {
			return ReportIntervalType.WEEK;
		}
		else if (monthButton.getSelection()) {
			return ReportIntervalType.MONTH;
		}
		else {
			return ReportIntervalType.YEAR;
		}
	}
	
	private Map<String, Button> buildCheckboxes(Composite composite, String... names) {
		Map<String, Button> map = new HashMap<String, Button>();
		for (String name : names) {
			Button button = new Button(composite, SWT.CHECK);
			map.put(name, button);
			button.setText(name);
		}
		return map;
	}
	
}
