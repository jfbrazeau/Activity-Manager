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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.Report;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.Strings;
import org.activitymgr.ui.rcp.dialogs.ErrorDialog;
import org.activitymgr.ui.rcp.dialogs.TaskChooserTreeWithHistoryDialog;
import org.activitymgr.ui.rcp.util.SafeRunner;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class ReportsUI {

	private static final class ColumnElementsComparator implements
			Comparator<String> {

		private Button orderByTasksButton;
		private java.util.List<String> itemOrder;

		public ColumnElementsComparator(Button orderByTasksButton,
				java.util.List<String> itemOrder) {
			this.orderByTasksButton = orderByTasksButton;
			this.itemOrder = itemOrder;
		}

		@Override
		public int compare(String id1, String id2) {
			int idx = id1.indexOf('.');
			String obj1 = id1.substring(0, idx);

			idx = id2.indexOf('.');
			String obj2 = id2.substring(0, idx);

			// Object part is more significant
			int compare = obj1.compareTo(obj2);
			// If not significant, field name becomes significant
			if (compare == 0) {
				return new Integer(itemOrder.indexOf(id1)).compareTo(itemOrder
						.indexOf(id2));
			} else if (orderByTasksButton.getSelection()) {
				return -compare;
			} else {
				return compare;
			}
		}

	}

	private static final String PATH_ATTRIBUTE = "task.path";
	
	private static final String CODE_ATTRIBUTE = "task.code";

	private static final String NAME_ATTRIBUTE = "task.name";
	
	private static final String COMMENT_ATTRIBUTE = "task.comment";
	
	private static final String BUDGET_ATTRIBUTE = "task.budget";
	
	private static final String INITIALLY_CONSUMED_ATTRIBUTE = "task.initiallyConsumed";
	
	private static final String ETC_ATTRIBUTE = "task.etc";
	
	private static final String LOGIN_ATTRIBUTE = "collaborator.login";

	private static final String FIRST_NAME_ATTRIBUTE = "collaborator.firstName";

	private static final String LAST_NAME_ATTRIBUTE = "collaborator.lastName";

	private static final String IS_ACTIVE_ATTRIBUTE = "collaborator.isActive";

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

	private Map<String, Button> attributesCheckboxesMap = new LinkedHashMap<String, Button>();

	private Button includeTasksButton;

	private Button filterByTaskCheckbox;

	private Button includeCollaboratorsButton;

	private Button tasksCentricButton;

	private Button collaboratorsCentricButton;

	private Spinner taskDepthSpinner;

	private Text filterByTaskText;

	private Button filterByTaskSelectButton;

	private TaskChooserTreeWithHistoryDialog taskChooserDialog;

	private Button automaticColumnsOrderButton;

	private Button customColumnsOrderButton;

	private Button upButton;

	private Button downButton;

	private ListViewer columnsOrderViewer;

	private java.util.List<String> columnsOrderElements;

	private ColumnElementsComparator columnElementsComparator;

	/**
	 * Default constructor intended to be used from within a tab.
	 * 
	 * @param tabItem
	 *            item parent.
	 * @param modelMgr
	 *            the model manager instance.
	 * @param factory
	 *            the {@link IDTOFactory DTO factory}.
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
	public ReportsUI(Composite parentComposite, final IModelMgr modelMgr,
			IDTOFactory factory) {
		this.modelMgr = modelMgr;
		this.factory = factory;

		// Création du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));

		// Report configuration form creation
		Composite cfgParent = new Composite(parent, SWT.NONE);
		cfgParent.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		cfgParent.setLayout(new GridLayout(1, false));

		// Interval grop
		Group intervalGroup = new Group(cfgParent, SWT.SHADOW_OUT);
		intervalGroup.setLayout(new GridLayout(2, false));
		intervalGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
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
		Composite intervalBoundsTypeGroup = new Composite(intervalGroup,
				SWT.NONE);
		intervalBoundsTypeGroup.setLayout(new RowLayout());
		automaticIntervalBoundsTypeRadio = new Button(intervalBoundsTypeGroup,
				SWT.RADIO);
		automaticIntervalBoundsTypeRadio.setText("Automatic");
		lowerBoundIntervalBoundsTypeRadio = new Button(intervalBoundsTypeGroup,
				SWT.RADIO);
		lowerBoundIntervalBoundsTypeRadio.setText("Lower bound");
		bothBoundsIntervalBoundsTypeRadio = new Button(intervalBoundsTypeGroup,
				SWT.RADIO);
		bothBoundsIntervalBoundsTypeRadio.setText("Both bounds");

		new Label(intervalGroup, SWT.NONE).setText("Interval bounds :");
		Composite intervalDatesGroup = new Composite(intervalGroup, SWT.NONE);
		intervalDatesGroup.setLayout(new GridLayout(3, false));
		startDateTime = new DateTime(intervalDatesGroup, SWT.DATE
				| SWT.CALENDAR | SWT.DROP_DOWN);
		endDateTime = new DateTime(intervalDatesGroup, SWT.DATE | SWT.CALENDAR
				| SWT.DROP_DOWN);
		new Label(intervalDatesGroup, SWT.NONE)
				.setText("(ignored if automatic mode is selected)");

		// Task group
		Group taskGroup = new Group(cfgParent, SWT.SHADOW_OUT);
		taskGroup.setLayout(new GridLayout(3, false));
		taskGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		taskGroup.setText("Task management");
		filterByTaskCheckbox = new Button(taskGroup, SWT.CHECK);
		filterByTaskCheckbox.setText("Limit task scope to :");
		filterByTaskText = new Text(taskGroup, SWT.NONE);
		filterByTaskText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterByTaskSelectButton = new Button(taskGroup, SWT.NONE);
		filterByTaskSelectButton.setText("...");

		includeTasksButton = new Button(taskGroup, SWT.CHECK);
		GridData includeTasksButtonGridData = new GridData();
		includeTasksButtonGridData.horizontalSpan = 3;
		includeTasksButton.setLayoutData(includeTasksButtonGridData);
		includeTasksButton.setText("Decline resultset by task");

		new Label(taskGroup, SWT.NONE).setText("Task tree depth :");
		GridData taskDepthSpinnerGridData = new GridData();
		taskDepthSpinnerGridData.horizontalSpan = 2;
		Composite taskDepthSpinnerComposite = new Composite(taskGroup, SWT.NONE);
		taskDepthSpinnerComposite.setLayoutData(taskDepthSpinnerGridData);
		taskDepthSpinnerComposite.setLayout(new GridLayout(2, false));
		taskDepthSpinner = new Spinner(taskDepthSpinnerComposite, SWT.NONE);
		taskDepthSpinner.setMinimum(1);
		new Label(taskDepthSpinnerComposite, SWT.NONE)
				.setText("(contributions of deeper depth will be aggregated)");

		new Label(taskGroup, SWT.NONE).setText("Columns to include :");
		buildCheckboxes(taskGroup, 2, PATH_ATTRIBUTE, CODE_ATTRIBUTE,
				NAME_ATTRIBUTE, COMMENT_ATTRIBUTE);
		new Label(taskGroup, SWT.NONE).setText("");
		buildCheckboxes(taskGroup, 2, BUDGET_ATTRIBUTE,
				INITIALLY_CONSUMED_ATTRIBUTE, ETC_ATTRIBUTE);

		// Collaborator group
		Group collaboratorGroup = new Group(cfgParent, SWT.SHADOW_OUT);
		collaboratorGroup.setLayout(new GridLayout(2, false));
		collaboratorGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		collaboratorGroup.setText("Collaborator management");
		includeCollaboratorsButton = new Button(collaboratorGroup, SWT.CHECK);
		GridData includeCollaboratorsButtonGridData = new GridData();
		includeCollaboratorsButtonGridData.horizontalSpan = 2;
		includeCollaboratorsButton.setText("Decline resultset by contributor");
		includeCollaboratorsButton
				.setLayoutData(includeCollaboratorsButtonGridData);

		new Label(collaboratorGroup, SWT.NONE).setText("Columns to include : ");
		buildCheckboxes(collaboratorGroup, 1, LOGIN_ATTRIBUTE,
				FIRST_NAME_ATTRIBUTE, LAST_NAME_ATTRIBUTE, IS_ACTIVE_ATTRIBUTE);

		// Order by group
		Group orderManagementGroup = new Group(cfgParent, SWT.NONE);
		orderManagementGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, false));
		orderManagementGroup.setLayout(new GridLayout(2, false));
		orderManagementGroup.setText("Order management");

		new Label(orderManagementGroup, SWT.NONE)
				.setText("Report mode :");
		Composite orderByComposite = new Composite(orderManagementGroup,
				SWT.NONE);
		orderByComposite.setLayout(new RowLayout());
		tasksCentricButton = new Button(orderByComposite, SWT.RADIO);
		tasksCentricButton.setText("Task centric");
		collaboratorsCentricButton = new Button(orderByComposite, SWT.RADIO);
		collaboratorsCentricButton.setText("Collaborator centric");

		new Label(orderManagementGroup, SWT.NONE)
				.setText("Columns order mode :");
		Composite columnsOrderComposite = new Composite(orderManagementGroup,
				SWT.NONE);
		columnsOrderComposite.setLayout(new RowLayout());
		automaticColumnsOrderButton = new Button(columnsOrderComposite, SWT.RADIO);
		automaticColumnsOrderButton.setText("Automatic");
		customColumnsOrderButton = new Button(columnsOrderComposite, SWT.RADIO);
		customColumnsOrderButton.setText("Manual");

		Label columnsOrderLabel = new Label(orderManagementGroup, SWT.NONE);
		GridData columnsOrderLabelGridData = new GridData();
		columnsOrderLabelGridData.verticalAlignment = SWT.TOP;
		columnsOrderLabel.setLayoutData(columnsOrderLabelGridData);
		columnsOrderLabel.setText("Columns order :");

		Composite columnsOrderPanel = new Composite(orderManagementGroup,
				SWT.NONE);
		columnsOrderPanel.setLayout(new GridLayout(2, false));
		List columnsOrderList = new List(columnsOrderPanel, SWT.FULL_SELECTION
				| SWT.BORDER | SWT.HIDE_SELECTION | SWT.V_SCROLL);
		columnsOrderViewer = new ListViewer(columnsOrderList);
		columnsOrderViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String id = (String) element;
				String objectType = id.split("\\.")[0];
				return Strings.getString(ReportsUI.class.getSimpleName() + "."
						+ id)
						+ " (" + Strings.getString(objectType) + ")";
			}
		});
		columnsOrderViewer.setContentProvider(ArrayContentProvider
				.getInstance());
		GridData columnsOrderTableGridData = new GridData();
		columnsOrderTableGridData.heightHint = 100;
		columnsOrderTableGridData.widthHint = 200;
		columnsOrderList.setLayoutData(columnsOrderTableGridData);
		columnsOrderElements = new ArrayList<String>();
		columnsOrderViewer.setInput(columnsOrderElements);

		Composite columnsOrderButtonsPanel = new Composite(columnsOrderPanel,
				SWT.NONE);
		GridData columnsOrderButtonsPanelGridData = new GridData();
		columnsOrderButtonsPanelGridData.verticalAlignment = SWT.TOP;
		columnsOrderButtonsPanel
				.setLayoutData(columnsOrderButtonsPanelGridData);
		GridLayout columnsOrderButtonsPanelLayout = new GridLayout(1, false);
		columnsOrderButtonsPanelLayout.marginTop = 0;
		columnsOrderButtonsPanelLayout.horizontalSpacing = 0;
		columnsOrderButtonsPanel.setLayout(columnsOrderButtonsPanelLayout);
		upButton = new Button(columnsOrderButtonsPanel, SWT.NONE);
		upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		upButton.setText("Up");
		downButton = new Button(columnsOrderButtonsPanel, SWT.NONE);
		downButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		downButton.setText("Down");

		Composite orderByAndButtonsPanel = new Composite(cfgParent, SWT.NONE);
		orderByAndButtonsPanel.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));
		orderByAndButtonsPanel.setLayout(new GridLayout(2, false));

		// Button
		Button buildReportButton = new Button(orderByAndButtonsPanel, SWT.NONE);
		GridData buildReportButtonGridData = new GridData(
				GridData.FILL_HORIZONTAL);
		buildReportButtonGridData.horizontalAlignment = SWT.END;
		buildReportButtonGridData.verticalAlignment = SWT.END;
		buildReportButton.setLayoutData(buildReportButtonGridData);
		buildReportButton.setText("Build report");

		// Default values
		monthButton.setSelection(true);
		automaticIntervalBoundsTypeRadio.setSelection(true);
		includeTasksButton.setSelection(true);
		includeCollaboratorsButton.setSelection(true);
		tasksCentricButton.setSelection(true);
		endDateTime.setMonth(endDateTime.getMonth() + 1);
		attributesCheckboxesMap.get(PATH_ATTRIBUTE).setSelection(true);
		attributesCheckboxesMap.get(NAME_ATTRIBUTE).setSelection(true);
		attributesCheckboxesMap.get(LOGIN_ATTRIBUTE).setSelection(true);
		automaticColumnsOrderButton.setSelection(true);

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
		startDateTime.addSelectionListener(buttonListener);
		endDateTime.addSelectionListener(buttonListener);
		filterByTaskCheckbox.addSelectionListener(buttonListener);
		includeTasksButton.addSelectionListener(buttonListener);
		includeCollaboratorsButton.addSelectionListener(buttonListener);
		automaticColumnsOrderButton.addSelectionListener(buttonListener);
		customColumnsOrderButton.addSelectionListener(buttonListener);
		tasksCentricButton.addSelectionListener(buttonListener);
		collaboratorsCentricButton.addSelectionListener(buttonListener);
		for (String id : attributesCheckboxesMap.keySet()) {
			attributesCheckboxesMap.get(id)
					.addSelectionListener(buttonListener);
		}
		columnsOrderList.addSelectionListener(buttonListener);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveColumnUpOrDown(true);
			}

		});
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveColumnUpOrDown(false);
			}

		});

		taskChooserDialog = new TaskChooserTreeWithHistoryDialog(
				filterByTaskSelectButton.getShell(), modelMgr);
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
				new SafeRunner() {
					@Override
					protected Object runUnsafe() throws Exception {
						buildReport();
						return null;
					}
				}.run(parent.getShell());
			}
		});

		// Create column elements comparator
		columnElementsComparator = new ColumnElementsComparator(
				tasksCentricButton, new ArrayList<String>(
						attributesCheckboxesMap.keySet()));

		// Update fields enablement
		updateFieldsEnablement();

	}

	private void moveColumnUpOrDown(boolean up) {
		String item = (String) columnsOrderViewer.getStructuredSelection()
				.getFirstElement();
		int idx = columnsOrderElements.indexOf(item);
		columnsOrderElements.remove(item);
		columnsOrderElements.add(idx + (up ? -1 : 1), item);
		// columnsOrderViewer.refresh();
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
			end.add(Calendar.DATE, 1);
			switch (intervalType) {
			case YEAR:
				intervalCount = end.get(Calendar.YEAR)
						- start.get(Calendar.YEAR);
				break;
			case MONTH:
				intervalCount = (end.get(Calendar.YEAR) - start
						.get(Calendar.YEAR))
						* 12
						+ (end.get(Calendar.MONTH) - start.get(Calendar.MONTH));
				break;
			case WEEK:
				intervalCount = DateHelper.countDaysBetween(start, end) / 7;
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
				Task selectedTask = modelMgr.getTaskByCodePath(filterByTaskText
						.getText());
				rootTaskId = selectedTask.getId();
			}
			Workbook report = modelMgr.buildReport(start, intervalType,
					intervalCount, rootTaskId, taskDepth,
					includeCollaboratorsButton.getSelection(),
					collaboratorsCentricButton.getSelection(),
					null, 
					columnsOrderElements);
			File file = File.createTempFile("am-report-", ".xls");
			System.out.println(file);
			FileOutputStream out = new FileOutputStream(file);
			report.write(out);
			out.close();
			Desktop.getDesktop().open(file);
		} catch (ModelException e) {
			new ErrorDialog(parent.getShell(), e.getMessage(), e).open();
		} catch (IOException e) {
			new ErrorDialog(parent.getShell(), e.getMessage(), e).open();
		}
	}

	private Calendar toCalendar(DateTime dateTime) {
		Calendar start;
		start = Calendar.getInstance();
		start.set(Calendar.YEAR, dateTime.getYear());
		start.set(Calendar.MONTH, dateTime.getMonth());
		start.set(Calendar.DATE, dateTime.getDay());
		start.set(Calendar.HOUR_OF_DAY, 12);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		return start;
	}

	private void updateFieldsEnablement() {
		System.out.println("updateFieldsEnablement" + System.currentTimeMillis());
		// Update date fields enablement
		if (automaticIntervalBoundsTypeRadio.getSelection()) {
			setEnabled(startDateTime, false);
			setEnabled(endDateTime, false);
		} else if (lowerBoundIntervalBoundsTypeRadio.getSelection()) {
			setEnabled(startDateTime, true);
			setEnabled(endDateTime, false);
		} else {
			setEnabled(startDateTime, true);
			setEnabled(endDateTime, true);
		}

		// Update attributes fields enablement
		for (String id : attributesCheckboxesMap.keySet()) {
			Button b = attributesCheckboxesMap.get(id);
			if (id.startsWith("task.")) {
				b.setEnabled(includeTasksButton.getSelection());
			} else {
				b.setEnabled(includeCollaboratorsButton.getSelection());
			}
		}
		// Update column order list content
		for (String id : attributesCheckboxesMap.keySet()) {
			Button b = attributesCheckboxesMap.get(id);
			if (!b.isEnabled() || !b.getSelection()) {
				columnsOrderElements.remove(id);
			} else if (!columnsOrderElements.contains(id)) {
				columnsOrderElements.add(id);
			}
		}

		// If default order is selected, auto sort
		if (automaticColumnsOrderButton.getSelection()) {
			Collections.sort(columnsOrderElements, columnElementsComparator);
		}
		// And then refresh the viewer
		columnsOrderViewer.refresh();
		taskDepthSpinner.setEnabled(includeTasksButton.getSelection());
		filterByTaskText.setEnabled(filterByTaskCheckbox.getSelection());
		filterByTaskSelectButton
				.setEnabled(filterByTaskCheckbox.getSelection());

		// Update order by fields enablement
		boolean orderByEnabled = (includeCollaboratorsButton.getSelection() && includeTasksButton
				.getSelection());
		tasksCentricButton.setEnabled(orderByEnabled);
		collaboratorsCentricButton.setEnabled(orderByEnabled);
		boolean customMode = customColumnsOrderButton.getSelection();
		columnsOrderViewer.getList().setEnabled(customMode);
		IStructuredSelection columnOrderSelection = columnsOrderViewer
				.getStructuredSelection();
		boolean upAndDownButtonsEnabled = customMode
				&& !columnOrderSelection.isEmpty();
		upButton.setEnabled(upAndDownButtonsEnabled
				&& columnsOrderViewer.getList().getSelectionIndex() != 0);
		downButton
				.setEnabled(upAndDownButtonsEnabled
						&& columnsOrderViewer.getList().getSelectionIndex() != columnsOrderViewer
								.getList().getItemCount() - 1);
		
		// 2nd filter : if in collaborator mode, several task fields
		// cannot be used (because it's not possible to agregate them)
		if (collaboratorsCentricButton.isEnabled() && collaboratorsCentricButton.getSelection()) {
			attributesCheckboxesMap.get(BUDGET_ATTRIBUTE).setEnabled(false);
			attributesCheckboxesMap.get(INITIALLY_CONSUMED_ATTRIBUTE)
					.setEnabled(false);
			attributesCheckboxesMap.get(ETC_ATTRIBUTE).setEnabled(false);
		}

		// Update dates
		ReportIntervalType intervalType = getReportIntervalType();
		if (!ReportIntervalType.DAY.equals(intervalType)) {
			Calendar start = toCalendar(startDateTime);
			Calendar end = toCalendar(endDateTime);
			switch (intervalType) {
			case YEAR:
				// Goto start of year
				start.set(Calendar.MONTH, 0);
				start.set(Calendar.DATE, 1);
				
				// Goto start of following year
				end.set(Calendar.MONTH, 0);
				end.set(Calendar.DATE, 1);
				end.add(Calendar.YEAR, 1);
				break;
			case MONTH:
				// Goto start of month
				start.set(Calendar.DATE, 1);
				
				// Goto start of following month
				end.set(Calendar.DATE, 1);
				end.add(Calendar.MONTH, 1);
				break;
			case WEEK:
				// Goto start of week
				start = DateHelper.moveToFirstDayOfWeek(start);
				
				// Goto start of following month
				end = DateHelper.moveToFirstDayOfWeek(end);
				end.add(Calendar.WEEK_OF_YEAR, 1);
				break;
			case DAY:
				// Do nothing
			}
			setDateTime(startDateTime, start);
			end.add(Calendar.DATE, -1);
			setDateTime(endDateTime, end);
		}

	}

	private void setDateTime(DateTime dateTime, Calendar cal) {
		dateTime.setYear(cal.get(Calendar.YEAR));
		dateTime.setMonth(cal.get(Calendar.MONTH));
		dateTime.setDay(cal.get(Calendar.DATE));
	}

	private void setEnabled(DateTime field, boolean enabled) {
		field.setEnabled(enabled);
		Display display = field.getShell().getDisplay();
		field.setBackground(enabled ? parent.getBackground() : display
				.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
	}

	private ReportIntervalType getReportIntervalType() {
		if (dayButton.getSelection()) {
			return ReportIntervalType.DAY;
		} else if (weekButton.getSelection()) {
			return ReportIntervalType.WEEK;
		} else if (monthButton.getSelection()) {
			return ReportIntervalType.MONTH;
		} else {
			return ReportIntervalType.YEAR;
		}
	}

	private void buildCheckboxes(Composite parent, int horizontalSpan,
			String... names) {
		Composite composite = new Composite(parent, SWT.SHADOW_OUT);
		RowLayout layout = new RowLayout();
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.spacing = 0;
		composite.setLayout(layout);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.horizontalSpan = horizontalSpan;
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout(names.length, false));
		for (String name : names) {
			Button button = new Button(composite, SWT.CHECK);
			attributesCheckboxesMap.put(name, button);
			button.setText(Strings.getString(ReportsUI.class.getSimpleName()
					+ "." + name));
		}
	}

}
