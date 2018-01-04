package org.activitymgr.ui.web.logic.impl.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic.ISelectedTaskCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeDownloadButtonLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeStandardButtonLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTwinSelectFieldLogic;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTwinSelectFieldLogic.IDTOInfosProvider;
import org.activitymgr.ui.web.logic.impl.AbstractTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.ExternalContentDialogLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.services.AbstractReportServiceLogic;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.inject.Inject;

public class ReportsTabLogicImpl extends
		AbstractTabLogicImpl<IReportsTabLogic.View> implements IReportsTabLogic {

	private static final String SERVICE_LOAD_URI = "/service/load";

	private static final String SERVICE_REPORT_HTML_URI = "/service/report/html";

	private static final String TASK = Task.class.getSimpleName().toLowerCase();
	
	private static final Collection<String> DTO_ATTRIBUTE_IDS_BLACKLIST = Arrays
			.asList(new String[] { "collaborator.id", "task.id",
					"task.fullPath", "task.number", "task.numberAsHex" });

	private static final String COLLABORATOR = Collaborator.class
			.getSimpleName().toLowerCase();

	static enum ReportIntervalBoundsMode {
		AUTOMATIC, LOWER_BOUND, BOTH_BOUNDS
	}
	
	static enum ReportCollaboratorsSelectionMode {
		ME, ALL_COLLABORATORS, SELECT_COLLABORATORS
	}

	private static final IDTOInfosProvider<Collaborator> COLLABORATORS_INFOS_PROVIDER = new IDTOInfosProvider<Collaborator>() {
		@Override
		public String getId(Collaborator dto) {
			return String.valueOf(dto.getId());
		}

		@Override
		public String getLabel(Collaborator dto) {
			return dto.getFirstName() + " " + dto.getLastName();
		}

	};

	private static final IDTOInfosProvider<DTOAttribute> DTO_ATTRIBUTE_INFOS_PROVIDER = new IDTOInfosProvider<DTOAttribute>() {
		@Override
		public String getId(DTOAttribute att) {
			return att.getId();
		}

		@Override
		public String getLabel(DTOAttribute att) {
			return att.getLabel();
		}

	};

	@Inject(optional = true)
	private Set<ITabButtonFactory<IReportsTabLogic>> buttonFactories;
	
	@Inject
	private IDTOFactory dtoFactory;

	private ReportIntervalType intervalType = ReportIntervalType.MONTH;

	private ReportIntervalBoundsMode intervalBoundsMode = ReportIntervalBoundsMode.AUTOMATIC;
	
	private Calendar start = Calendar.getInstance();
	
	private Calendar end = Calendar.getInstance();
	
	private int intervalCount;

	private String taskScopePath = "";

	private ReportCollaboratorsSelectionMode collaboratorsSelectionMode;

	private int taskTreeDepth = 1;

	private AbstractSafeTwinSelectFieldLogic<Collaborator> collaboratorsSelectionLogic;

	private AbstractSafeTwinSelectFieldLogic<DTOAttribute> columnsSelectionLogic;

	private Collaborator[] collaborators;
	
	private boolean onlyKeepTaskWithContributions;

	private String tabLabel;

	private AbstractSafeDownloadButtonLogicImpl downloadReportButtonLogic;

	private boolean advancedMode;

	private AbstractSafeStandardButtonLogicImpl showPreviewDialogButtonLogic;

	private AbstractSafeStandardButtonLogicImpl showPreviewFullscreenButtonLogic;

	private List<DTOAttribute> attributes;

	public ReportsTabLogicImpl(ITabFolderLogic parent,
			final boolean advancedMode) {
		super(parent);
		this.advancedMode = advancedMode;

		tabLabel = advancedMode ? "Adv. reports" : "My reports";

		// Initialize view
		getView().initialize(advancedMode);

		// Add buttons
		registerButtons(buttonFactories);
		for (ReportIntervalType type : ReportIntervalType.values()) {
			getView().addIntervalTypeRadioButton(type,
					StringHelper.toUpperFirst(type.name().toLowerCase()));
		}
		for (ReportIntervalBoundsMode mode : ReportIntervalBoundsMode.values()) {
			getView().addIntervalBoundsModeRadioButton(
					mode,
					StringHelper.toUpperFirst(mode.name().replace('_', ' ')
							.toLowerCase()));
		}
		if (advancedMode) {
			for (ReportCollaboratorsSelectionMode mode : ReportCollaboratorsSelectionMode.values()) {
				getView().addCollaboratorsSelectionModeRadioButton(
						mode,
						StringHelper.toUpperFirst(mode.name().replace('_', ' ')
								.toLowerCase()));
			}
		}
		/*
		 * Collaborators twin select
		 */
		collaborators = getModelMgr().getCollaborators();
		collaboratorsSelectionLogic = new AbstractSafeTwinSelectFieldLogic<Collaborator>(
				this, false, COLLABORATORS_INFOS_PROVIDER, collaborators) {
			@Override
			protected void unsafeOnValueChanged(Collection<String> newValue)
					throws Exception {
				updateUI();
			}
		};
		if (advancedMode) {
			getView().setCollaboratorsSelectionView(
					collaboratorsSelectionLogic.getView());
		}
		try {
			/*
			 * Attributes twin select
			 */
			attributes = new ArrayList<DTOAttribute>();
			appendDTOAttributes(attributes, dtoFactory.newTask(), TASK);
			appendDTOAttributes(attributes, dtoFactory.newCollaborator(),
					COLLABORATOR);
			Collections.sort(attributes, new Comparator<DTOAttribute>() {
				@Override
				public int compare(DTOAttribute o1, DTOAttribute o2) {
					return o1.getLabel().compareTo(o2.getLabel());
				}
			});
			columnsSelectionLogic = new AbstractSafeTwinSelectFieldLogic<DTOAttribute>(
					this, true, DTO_ATTRIBUTE_INFOS_PROVIDER,
					attributes.toArray(new DTOAttribute[attributes.size()])) {
				@Override
				protected void unsafeOnValueChanged(Collection<String> newValue)
						throws Exception {
					updateUI();
				}
			};
			if (advancedMode) {
				getView().setColumnSelectionView(
						columnsSelectionLogic.getView());
			}

		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		getView().addReportButton(
				new AbstractSafeStandardButtonLogicImpl(this, "Defaults", null,
						null) {
					@Override
					protected void unsafeOnClick() throws Exception {
						restoreDefaultValues();
					}

				}.getView());
		showPreviewFullscreenButtonLogic = new AbstractSafeStandardButtonLogicImpl(
				this, "Preview (page)", null, null) {
			@Override
			protected void unsafeOnClick() throws Exception {
				onShowPreviewFullscreenButtonClicked();
			}
		};
		getView().addReportButton(showPreviewFullscreenButtonLogic.getView());
		showPreviewDialogButtonLogic = new AbstractSafeStandardButtonLogicImpl(
				this, "Preview (dialog)", null, null) {
			@Override
			protected void unsafeOnClick() throws Exception {
				onShowPreviewDialogButtonClicked();
			}
		};
		getView().addReportButton(
				showPreviewDialogButtonLogic.getView());
		downloadReportButtonLogic = new AbstractSafeDownloadButtonLogicImpl(
				this, "Build report",
				null, null) {
			@Override
			protected byte[] unsafeGetContent() throws Exception {
				Workbook report = buildReport(false);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				report.write(out);
				return out.toByteArray();
			}

			@Override
			protected String unsafeGetFileName() throws Exception {
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyyMMdd-HHmmss-SSS");
				return "am-report-" + sdf.format(new Date()) + ".xls";
			}

		};
		getView().addReportButton(
				downloadReportButtonLogic.getView());
		restoreDefaultValues();
	}

	private void restoreDefaultValues() {
		intervalType = ReportIntervalType.MONTH;
		intervalBoundsMode = ReportIntervalBoundsMode.AUTOMATIC;
		start = Calendar.getInstance();
		end = Calendar.getInstance();
		intervalCount = 0;
		taskScopePath = "";
		getView().setTaskScopePath(taskScopePath);
		collaboratorsSelectionMode = advancedMode ? ReportCollaboratorsSelectionMode.ALL_COLLABORATORS
				: ReportCollaboratorsSelectionMode.ME;
		taskTreeDepth = 1;
		getView().setTaskTreeDepth(taskTreeDepth);
		collaboratorsSelectionLogic.select(new Collaborator[0]);
		Map<String, DTOAttribute> map = new HashMap<String, DTOAttribute>();
		for (DTOAttribute att : attributes) {
			map.put(att.getId(), att);
		}
		if (advancedMode) {
			columnsSelectionLogic.select(map.get("task.path"),
					map.get("task.name"), map.get("collaborator.login"));
		} else {
			columnsSelectionLogic.select(map.get("task.path"),
					map.get("task.name"));
		}
		// in basic mode, only keep non empty tasks by default
		onlyKeepTaskWithContributions = !advancedMode;
		updateUI();
	}

	private static void appendDTOAttributes(List<DTOAttribute> attributes,
			Object dto, String dtoLabel) throws ReflectiveOperationException {
		for (Object property : BeanUtils.describe(dto).keySet()) {
			if (!"class".equals(property)) {
				DTOAttribute att = new DTOAttribute(dtoLabel + "." + property,
						StringHelper.camelCaseToPhrase(String.valueOf(property))
								+ " ("
								+ dtoLabel + ")");
				if (!DTO_ATTRIBUTE_IDS_BLACKLIST.contains(att.getId())) {
					attributes.add(att);
				}
			}
		}
	}

	@Override
	public String getLabel() {
		return tabLabel;
	}

	@Override
	public void onIntervalTypeChanged(Object newValue) {
		intervalType = (ReportIntervalType) newValue;
		updateUI();
	}
	
	@Override
	public void onIntervalBoundsModeChanged(Object newValue) {
		intervalBoundsMode = (ReportIntervalBoundsMode) newValue;
		updateUI();
	}

	@Override
	public void onIntervalBoundsChanged(Date startDate, Date endDate) {
		start.setTime(startDate);
		end.setTime(endDate);
		updateUI();
	}

	@Override
	public void onCollaboratorsSelectionModeChanged(Object newValue) {
		collaboratorsSelectionMode = (ReportCollaboratorsSelectionMode) newValue;
		updateUI();
	}

	@Override
	public void onBrowseTaskButtonCLicked() {
		Long selectedTask = null;
		try {
			if (taskScopePath != null) {
				Task task = getModelMgr().getTaskByCodePath(taskScopePath);
				selectedTask = task.getId();
			}
		} catch (ModelException e) {
			// Simply ignore, and consider that no task is selected
		}
		new TaskChooserLogic(this, selectedTask, new ISelectedTaskCallback() {
			@Override
			public void taskSelected(long taskId) {
				Task task = getModelMgr().getTask(taskId);
				try {
					String taskCodePath = getModelMgr().getTaskCodePath(task);
					ReportsTabLogicImpl.this.taskScopePath = taskCodePath;
					getView().setTaskScopePath(taskCodePath);
					updateUI();
				} catch (ModelException e) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					e.printStackTrace(new PrintWriter(out));
					getRoot().getView().showErrorNotification(
							"Unexpeced error while retrieving task path",
							new String(out.toByteArray()));
				}
			}
		});
	}

	@Override
	public void onTaskScopePathChanged(String newValue) {
		taskScopePath = newValue;
		updateUI();
	}

	@Override
	public void onTaskTreeDepthChanged(int newDepth) {
		if (newDepth < 1) {
			taskTreeDepth = 1;
			getView().setTaskTreeDepth(taskTreeDepth);
		} else {
			taskTreeDepth = newDepth;
		}
		updateUI();
	}

	@Override
	public void onOnlyKeepTaskWithContributionsCheckboxChanged(boolean newValue) {
		onlyKeepTaskWithContributions = newValue;
		updateUI();
	}

	@Override
	public void onIntervalCountChanged(int newValue) {
		intervalCount = newValue;
		end.setTime(start.getTime());
		switch (intervalType) {
		case YEAR:
			end.add(Calendar.YEAR, intervalCount);
			break;
		case MONTH:
			end.add(Calendar.MONTH, intervalCount);
			break;
		case WEEK:
			end.add(Calendar.WEEK_OF_YEAR, intervalCount);
			break;
		case DAY:
			end.add(Calendar.DATE, intervalCount);
		}
		end.add(Calendar.DATE, -1);
		updateUI();
	}

	private void onShowPreviewFullscreenButtonClicked() {
		String url = buildRestServiceURL();
		getRoot().getView().openExternalUrl(url);
	}

	private void onShowPreviewDialogButtonClicked() {
		String url = buildRestServiceURL();
		ExternalContentDialogLogicImpl popup = new ExternalContentDialogLogicImpl(
				this, "Preview", url);
		getRoot().getView().openWindow(popup.getView());
	}

	private String buildRestServiceURL() {
		String url = null;
		try {
			StringWriter sw = new StringWriter();
				sw.append(SERVICE_LOAD_URI);
				appendUrlParam(sw, true, "service", SERVICE_REPORT_HTML_URI);
			ReportParameters reportParameters = prepareReportParameters();
			appendUrlParam(sw,
					AbstractReportServiceLogic.INTERVAL_TYPE_PARAMETER,
					reportParameters.intervalType.toString());
			if (reportParameters.start != null) {
				appendUrlParam(sw, AbstractReportServiceLogic.START_PARAMETER,
						new SimpleDateFormat("yyyyMMdd")
								.format(reportParameters.start.getTime()));
			}
			if (reportParameters.intervalCount != null) {
				appendUrlParam(sw,
					AbstractReportServiceLogic.INTERVAL_COUNT_PARAMETER,
						reportParameters.intervalCount);
			}
			if (reportParameters.rootTaskId != null) {
				appendUrlParam(sw,
						AbstractReportServiceLogic.ROOT_TASK_PARAMETER,
						taskScopePath);
			}
			appendUrlParam(sw,
					AbstractReportServiceLogic.TASK_DEPTH_PARAMETER,
					reportParameters.taskDepth);
			if (reportParameters.contributorIds != null) {
				Object[] array = new Object[reportParameters.contributorIds.length];
				for (int i = 0; i < reportParameters.contributorIds.length; i++) {
					array[i] = reportParameters.contributorIds[i];
				}
				appendUrlParam(sw,
						AbstractReportServiceLogic.CONTRIBUTOR_IDS_PARAMETERS,
						array);
			}
			if (advancedMode) {
				appendUrlParam(sw,
						AbstractReportServiceLogic.BY_CONTRIBUTOR_PARAMETER,
						reportParameters.byContributor);
				appendUrlParam(
						sw,
						AbstractReportServiceLogic.CONTRIBUTOR_CENTRIC_MODE_PARAMETER,
						reportParameters.contributorCentricMode);
				appendUrlParam(sw,
						AbstractReportServiceLogic.COLUMN_IDS_PARAMETER,
						reportParameters.columnIds);
				appendUrlParam(
						sw,
						AbstractReportServiceLogic.ONLY_KEEP_TASKS_WITH_CONTRIBUTIONS_PARAMETER,
						onlyKeepTaskWithContributions);
			}
			url = sw.toString();
		} catch (ModelException e) {
			// Shouldn't occur (if a ModelException is raised, the button
			// should be disabled)
			throw new IllegalStateException(e);
		}
		return url;
	}

	private static <O> void appendUrlParam(StringWriter sw, String param,
			O... values) {
		appendUrlParam(sw, false, param, values);
	}

	private static <O> void appendUrlParam(StringWriter sw, boolean first,
			String param, O... values) {
		try {
			sw.append(first ? "?" : "&");
			sw.append(param);
			sw.append("=");
			boolean firstValue = true;
			for (Object value : values) {
				if (!firstValue) {
					sw.append(",");
				}
				sw.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
				firstValue = false;
			}
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	void updateUI() {
		try {
			getView().setErrorMessage("");
			// Update interval type & bounds
			getView().selectIntervalTypeRadioButton(intervalType);
			getView().selectIntervalBoundsModeButton(intervalBoundsMode);

			// Update date fields enablement
			switch (intervalBoundsMode) {
			case AUTOMATIC:
				getView().setIntervalBoundsModeEnablement(false, false);
				break;
			case LOWER_BOUND:
				getView().setIntervalBoundsModeEnablement(true, false);
				break;
			case BOTH_BOUNDS:
				getView().setIntervalBoundsModeEnablement(true, true);
			}

			// In automatic mode, auto select dates
			if (intervalBoundsMode == ReportIntervalBoundsMode.AUTOMATIC
					|| intervalBoundsMode == ReportIntervalBoundsMode.LOWER_BOUND) {
				Long rootTaskId = null;
				if (taskScopePath != null && taskScopePath.trim().length() > 0) {
					Task selectedTask = getModelMgr().getTaskByCodePath(
							taskScopePath);
					rootTaskId = selectedTask != null ? selectedTask.getId()
							: null;
				}
				Calendar[] interval = getModelMgr().getContributionsInterval(rootTaskId);
				if (interval != null) {
					// In automatic mode auto set the start date
					if (intervalBoundsMode == ReportIntervalBoundsMode.AUTOMATIC) {
						start = interval[0];
					}
					// In both modes auto set the end date
					end = interval[1];
				} else {
					// If the task has no contribution, no interval will be
					// returned : align end on start in such case
					end = start;
				}
			}

			// Update dates
			if (!ReportIntervalType.DAY.equals(intervalType)) {
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
				end.add(Calendar.DATE, -1);
			}
			getView().setIntervalBounds(start.getTime(),
					ReportsTabLogicImpl.this.end.getTime());

			// Compute interval count
			Calendar endClone = (Calendar) end.clone();
			switch (intervalType) {
			case YEAR:
				intervalCount = endClone.get(Calendar.YEAR)
						- start.get(Calendar.YEAR);
				break;
			case MONTH:
				intervalCount = (endClone.get(Calendar.YEAR) - start
						.get(Calendar.YEAR))
						* 12
						+ (endClone.get(Calendar.MONTH) - start
								.get(Calendar.MONTH));
				break;
			case WEEK:
				intervalCount = DateHelper.countDaysBetween(start, endClone) / 7;
				break;
			case DAY:
				intervalCount = DateHelper.countDaysBetween(start, endClone);
			}
			intervalCount++;
			getView().setIntervalCount(intervalCount);

			// Check if collaborators selection UI must be enables
			getView().selectCollaboratorsSelectionModeRadioButton(
					collaboratorsSelectionMode);
			getView()
					.setCollaboratorsSelectionUIEnabled(
							collaboratorsSelectionMode == ReportCollaboratorsSelectionMode.SELECT_COLLABORATORS);

			// Check if one task attribute is selected
			List<DTOAttribute> selectedColumns = columnsSelectionLogic
					.getValue();
			boolean includeTaskAttrs = false;
			for (DTOAttribute att : selectedColumns) {
				if (att.getId().startsWith(TASK)) {
					includeTaskAttrs = true;
					break;
				}
			}
			getView().setRowContentConfigurationEnabled(includeTaskAttrs);

			buildReport(true);
			setReportButtonsEnabled(true);
		} catch (ModelException e) {
			setReportButtonsEnabled(false);
			getView().setErrorMessage(e.getMessage());
		}
	}

	private void setReportButtonsEnabled(boolean enabled) {
		downloadReportButtonLogic.getView().setEnabled(enabled);
		showPreviewDialogButtonLogic.getView().setEnabled(enabled);
		showPreviewFullscreenButtonLogic.getView().setEnabled(enabled);
	}

	private Workbook buildReport(boolean dryRun) throws ModelException {
		ReportParameters reportParameters = prepareReportParameters();

		Workbook report = getModelMgr().buildReport(start, // Start date
						reportParameters.intervalType, // Interval type
				reportParameters.intervalCount, // Interval count
				reportParameters.rootTaskId, // Root task id
				reportParameters.taskDepth, // Task tree
													// depth
				reportParameters.onlyKeepTasksWithContributions, // Only keep
																	// tasks
																	// with
												// contributions
				reportParameters.byContributor, // Include collaborators
				reportParameters.contributorCentricMode, // Collaborators
															// centric mode
				reportParameters.contributorIds, // Contributor ids
				reportParameters.columnIds, // Column ids
				true,
				dryRun);
		return report;
	}

	private ReportParameters prepareReportParameters() throws ModelException {
		ReportParameters reportParameters = new ReportParameters();
		if (intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC) {
			reportParameters.start = ReportsTabLogicImpl.this.start;
		}
		reportParameters.intervalType = intervalType;
		reportParameters.intervalCount = intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC ? intervalCount
				: null;

		if (taskScopePath != null && taskScopePath.trim().length() > 0) {
			Task selectedTask = getModelMgr().getTaskByCodePath(taskScopePath);
			reportParameters.rootTaskId = selectedTask != null ? selectedTask
					.getId() : null;
		}

		List<DTOAttribute> selectedColumns = columnsSelectionLogic.getValue();
		reportParameters.columnIds = new String[selectedColumns.size()];
		for (int i = 0; i < selectedColumns.size(); i++) {
			reportParameters.columnIds[i] = selectedColumns.get(i).getId();
		}
		reportParameters.contributorCentricMode = selectedColumns.size() > 0
				&& selectedColumns.get(0).getId()
				.startsWith(COLLABORATOR);
		reportParameters.byContributor = containsDTOAttribute(selectedColumns,
					COLLABORATOR);

		boolean includeTasks = containsDTOAttribute(selectedColumns, TASK);
		reportParameters.taskDepth = includeTasks ? taskTreeDepth : 0;

		switch (collaboratorsSelectionMode) {
		case ME:
			reportParameters.contributorIds = new long[] { getContext()
					.getConnectedCollaborator().getId() };
			break;
		case ALL_COLLABORATORS:
			reportParameters.contributorIds = null;
			break;
		case SELECT_COLLABORATORS:
			List<Collaborator> selectedCollaborators = collaboratorsSelectionLogic
					.getValue();
			if (selectedCollaborators.size() == 0)
				throw new ModelException(
						"At least one collaborator must be selected");
			if (selectedCollaborators.size() != collaborators.length) {
				reportParameters.contributorIds = new long[selectedCollaborators
						.size()];
				for (int i = 0; i < selectedCollaborators.size(); i++) {
					reportParameters.contributorIds[i] = selectedCollaborators
							.get(i).getId();
				}
			}
		}
		reportParameters.onlyKeepTasksWithContributions = onlyKeepTaskWithContributions;
		return reportParameters;
	}

	private boolean containsDTOAttribute(List<DTOAttribute> attributes,
			String dtoId) {
		boolean includeCollaborators = false;
		for (DTOAttribute att : attributes) {
			if (att.getId().startsWith(dtoId)) {
				includeCollaborators = true;
			}
		}
		return includeCollaborators;
	}

}

class DTOAttribute {

	private String id;

	private String label;

	public DTOAttribute(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

}

class ReportParameters {
	Calendar start;
	ReportIntervalType intervalType;
	Integer intervalCount;
	Long rootTaskId;
	int taskDepth;
	boolean onlyKeepTasksWithContributions;
	boolean byContributor;
	boolean contributorCentricMode;
	long[] contributorIds;
	String[] columnIds;
}
