package org.activitymgr.ui.web.logic.impl.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
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

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.report.ReportIntervalType;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.DateHelper;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.IReportsLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeDownloadButtonLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeStandardButtonLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTwinSelectFieldLogic;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTwinSelectFieldLogic.IDTOInfosProvider;
import org.activitymgr.ui.web.logic.impl.ExternalContentDialogLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.services.AbstractReportServiceLogic;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;

public class ReportsLogicImpl extends AbstractLogicImpl<IReportsLogic.View>
		implements IReportsLogic {

	private static final String ONLY_KEEP_TASK_WITH_CONTRIBUTIONS = "onlyKeepTaskWithContributions";

	private static final String TASK_TREE_DEPTH = "taskTreeDepth";

	private static final String SELECTED_COLUMNS = "selectedColumns";

	private static final String SELECTED_COLLABORATORS = "selectedCollaborators";

	private static final String COLLABORATORS_SELECTION_MODE = "collaboratorsSelectionMode";

	private static final String TASK_SCOPE_PATH = "taskScopePath";

	private static final String INTERVAL_COUNT = "intervalCount";

	private static final String START = "start";

	private static final String INTERVAL_BOUNDS_MODE = "intervalBoundsMode";

	private static final String INTERVAL_TYPE = "intervalType";

	private static final String SERVICE_LOAD_URI = "/service/load";

	private static final String SERVICE_REPORT_HTML_URI = "/service/report/html";

	private static final SimpleDateFormat YYYYMMDD_SDF = new SimpleDateFormat(
			"yyyyMMdd");

	private static final String TASK = Task.class.getSimpleName().toLowerCase();
	
	protected static final String COLLABORATOR = Collaborator.class
			.getSimpleName().toLowerCase();

	private static final Collection<String> DTO_ATTRIBUTE_IDS_BLACKLIST = Arrays
			.asList(new String[] { "collaborator.id", "task.id",
					"task.fullPath", "task.number", "task.numberAsHex" });

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

	private AbstractSafeDownloadButtonLogicImpl downloadReportButtonLogic;

	private boolean advancedMode;

	private AbstractSafeStandardButtonLogicImpl showPreviewDialogButtonLogic;

	private AbstractSafeStandardButtonLogicImpl showPreviewFullscreenButtonLogic;

	private List<DTOAttribute> attributes;

	private Map<String, DTOAttribute> attributesMap;

	private String defaultConfigurationAsJson;

	public ReportsLogicImpl(AbstractLogicImpl<?> parent,
			final boolean advancedMode) {
		super(parent);
		this.advancedMode = advancedMode;

		// Initialize view
		getView().initialize(advancedMode);

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
			attributesMap = new HashMap<String, DTOAttribute>();
			for (DTOAttribute att : attributes) {
				attributesMap.put(att.getId(), att);
			}
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
				new AbstractSafeStandardButtonLogicImpl(this, "Reset", null,
						null) {
					@Override
					protected void unsafeOnClick() throws Exception {
						loadFromJson(defaultConfigurationAsJson);
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
		if (advancedMode) {
			columnsSelectionLogic.select(attributesMap.get("task.path"),
					attributesMap.get("task.name"),
					attributesMap.get("collaborator.login"));
		} else {
			columnsSelectionLogic.select(attributesMap.get("task.path"),
					attributesMap.get("task.name"));
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
		new AbstractTaskChooserLogic(this, selectedTask) {
			@Override
			public void onOkButtonClicked(long taskId) {
				Task task = getModelMgr().getTask(taskId);
				try {
					String taskCodePath = getModelMgr().getTaskCodePath(task);
					ReportsLogicImpl.this.taskScopePath = taskCodePath;
					ReportsLogicImpl.this.getView().setTaskScopePath(
							taskCodePath);
					ReportsLogicImpl.this.updateUI();
				} catch (ModelException e) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					e.printStackTrace(new PrintWriter(out));
					getRoot().getView().showErrorNotification(
							"Unexpeced error while retrieving task path",
							new String(out.toByteArray()));
				}
			}
		};
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
		computeEndFromIntervalCount();
		updateUI();
	}

	private void computeEndFromIntervalCount() {
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
					reportParameters.getIntervalType().toString());
			if (reportParameters.getStart() != null) {
				appendUrlParam(sw, AbstractReportServiceLogic.START_PARAMETER,
						new SimpleDateFormat("yyyyMMdd")
								.format(reportParameters.getStart().getTime()));
			}
			if (reportParameters.getIntervalCount() != null) {
				appendUrlParam(sw,
					AbstractReportServiceLogic.INTERVAL_COUNT_PARAMETER,
						reportParameters.getIntervalCount());
			}
			if (reportParameters.getRootTaskId() != null) {
				appendUrlParam(sw,
						AbstractReportServiceLogic.ROOT_TASK_PARAMETER,
						taskScopePath);
			}
			appendUrlParam(sw,
					AbstractReportServiceLogic.TASK_DEPTH_PARAMETER,
					reportParameters.getTaskDepth());
			if (advancedMode) {
				long[] contributorIds = reportParameters.getContributorIds();
				if (contributorIds != null) {
					Object[] array = new Object[contributorIds.length];
					for (int i = 0; i < contributorIds.length; i++) {
						array[i] = contributorIds[i];
					}
					appendUrlParam(
							sw,
							AbstractReportServiceLogic.CONTRIBUTOR_IDS_PARAMETERS,
							array);
				}
				appendUrlParam(sw,
						AbstractReportServiceLogic.BY_CONTRIBUTOR_PARAMETER,
						reportParameters.isByContributor());
				appendUrlParam(
						sw,
						AbstractReportServiceLogic.CONTRIBUTOR_CENTRIC_MODE_PARAMETER,
						reportParameters.isContributorCentricMode());
				appendUrlParam(sw,
						AbstractReportServiceLogic.COLUMN_IDS_PARAMETER,
						reportParameters.getColumnIds().toArray());
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
		sw.append(first ? "?" : "&");
		sw.append(param);
		sw.append("=");
		boolean firstValue = true;
		for (Object value : values) {
			if (!firstValue) {
				sw.append(",");
			}
			sw.append(StringHelper.urlEncodeAmpersand(String.valueOf(value)));
			firstValue = false;
		}
	}

	private void updateUI() {
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
					ReportsLogicImpl.this.end.getTime());

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

			getView().setTaskScopePath(taskScopePath);
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
			getView().setRowContentConfigurationEnabled(includeTaskAttrs);
			if (!includeTaskAttrs) {
				taskTreeDepth = 0;
			}
			else if (taskTreeDepth == 0) {
				taskTreeDepth = 1;
			}
			getView().setTaskTreeDepth(taskTreeDepth);

			getView().setOnlyKeepTaskWithContributions(
					onlyKeepTaskWithContributions);
			buildReport(true);
			setReportButtonsEnabled(true);
			// Notify
			onReportConfigurationChanged(toJson());
		} catch (ModelException e) {
			setReportButtonsEnabled(false);
			getView().setErrorMessage(e.getMessage());
		}
	}

	/**
	 * This method is intended to be subclassed.
	 * 
	 * @param json
	 *            the report configuration json.
	 */
	protected void onReportConfigurationChanged(String json) {
	}

	private void setReportButtonsEnabled(boolean enabled) {
		downloadReportButtonLogic.getView().setEnabled(enabled);
		showPreviewDialogButtonLogic.getView().setEnabled(enabled);
		showPreviewFullscreenButtonLogic.getView().setEnabled(enabled);
	}

	private Workbook buildReport(boolean dryRun) throws ModelException {
		ReportParameters reportParameters = prepareReportParameters();
		Workbook report = getModelMgr().buildReport(
				start, // Start date
				reportParameters.getIntervalType(), // Interval type
				reportParameters.getIntervalCount(), // Interval count
				reportParameters.getRootTaskId(), // Root task id
				reportParameters.getTaskDepth(), // Task tree
				// depth
				reportParameters.isOnlyKeepTasksWithContributions(), // Only
																		// keep
																		// tasks
																		// with
				// contributions
				reportParameters.isByContributor(), // Include
													// collaborators
				reportParameters.isContributorCentricMode(), // Collaborators
																// centric
																// mode
				reportParameters.getContributorIds(), // Contributor ids
				reportParameters.getColumnIds().toArray(
						new String[reportParameters.getColumnIds().size()]), // Column
																				// ids
				true, dryRun);
		return report;
	}

	private ReportParameters prepareReportParameters() throws ModelException {
		ReportParameters reportParameters = new ReportParameters();
		if (intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC) {
			reportParameters.setStart(ReportsLogicImpl.this.start);
		}
		reportParameters.setIntervalType(intervalType);
		reportParameters
				.setIntervalCount(intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC ? intervalCount
						: null);

		if (taskScopePath != null && taskScopePath.trim().length() > 0) {
			Task selectedTask = getModelMgr().getTaskByCodePath(
					taskScopePath.trim());
			reportParameters.setRootTaskId(selectedTask != null ? selectedTask
					.getId() : null);
		}

		List<DTOAttribute> selectedColumns = columnsSelectionLogic.getValue();
		for (int i = 0; i < selectedColumns.size(); i++) {
			reportParameters.getColumnIds().add(selectedColumns.get(i).getId());
		}
		reportParameters.setTaskDepth(taskTreeDepth);

		switch (collaboratorsSelectionMode) {
		case ME:
			reportParameters.setContributorIds(new long[] { getContext()
					.getConnectedCollaborator().getId() });
			break;
		case ALL_COLLABORATORS:
			reportParameters.setContributorIds(null);
			break;
		case SELECT_COLLABORATORS:
			List<Collaborator> selectedCollaborators = collaboratorsSelectionLogic
					.getValue();
			if (selectedCollaborators.size() == 0)
				throw new ModelException(
						"At least one collaborator must be selected");
			if (selectedCollaborators.size() != collaborators.length) {
				long[] contributorIds = new long[selectedCollaborators
						.size()];
				reportParameters.setContributorIds(contributorIds);
				for (int i = 0; i < selectedCollaborators.size(); i++) {
					contributorIds[i] = selectedCollaborators
							.get(i).getId();
				}
			}
		}
		reportParameters
				.setOnlyKeepTasksWithContributions(onlyKeepTaskWithContributions);
		return reportParameters;
	}

	public String toJson() {
		JsonObject json = new JsonObject();
		json.addProperty(INTERVAL_TYPE, String.valueOf(intervalType));
		json.addProperty(INTERVAL_BOUNDS_MODE,
				String.valueOf(intervalBoundsMode));
		if (intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC) {
			json.addProperty(START, YYYYMMDD_SDF.format(start.getTime()));
		}
		if (intervalBoundsMode == ReportIntervalBoundsMode.BOTH_BOUNDS) {
			json.addProperty(INTERVAL_COUNT, intervalCount);
		}
		if (taskScopePath != null && !"".equals(taskScopePath.trim())) {
			json.addProperty(TASK_SCOPE_PATH, taskScopePath.trim());
		}
		if (advancedMode) {
			json.addProperty(COLLABORATORS_SELECTION_MODE,
					String.valueOf(collaboratorsSelectionMode));
			if (collaboratorsSelectionMode == ReportCollaboratorsSelectionMode.SELECT_COLLABORATORS) {
				JsonArray jsonArray = new JsonArray();
				json.add(SELECTED_COLLABORATORS, jsonArray);
				List<Collaborator> selectedCollaborators = collaboratorsSelectionLogic
						.getValue();
				for (Collaborator collaborator : selectedCollaborators) {
					jsonArray.add(new JsonPrimitive(collaborator.getId()));
				}
			}
			JsonArray jsonArray = new JsonArray();
			json.add(SELECTED_COLUMNS, jsonArray);
			List<DTOAttribute> selectedColumns = columnsSelectionLogic
					.getValue();
			for (DTOAttribute attribute : selectedColumns) {
				jsonArray.add(new JsonPrimitive(attribute.getId()));
			}
		}
		json.addProperty(TASK_TREE_DEPTH, taskTreeDepth);
		if (advancedMode) {
			json.addProperty(ONLY_KEEP_TASK_WITH_CONTRIBUTIONS,
				onlyKeepTaskWithContributions);
		}
		StringWriter sw = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(sw);
		jsonWriter.setIndent("  ");
		try {
			Streams.write(json, jsonWriter);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return sw.toString();
	}

	public void loadFromJson(String json) {
		this.defaultConfigurationAsJson = json;
		restoreDefaultValues();
		if (json != null && !"".equals(json.trim())) {
			JsonReader jsonReader = new JsonReader(new StringReader(json));
			JsonObject jsonObject = (JsonObject) Streams.parse(jsonReader);
			if (jsonObject.has(INTERVAL_TYPE)) {
				intervalType = ReportIntervalType.valueOf(jsonObject.get(
						INTERVAL_TYPE).getAsString());
			}
			if (jsonObject.has(INTERVAL_BOUNDS_MODE)) {
				intervalBoundsMode = ReportIntervalBoundsMode
						.valueOf(jsonObject.get(
						INTERVAL_BOUNDS_MODE).getAsString());
			}
			if (intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC
					&& jsonObject.has(START)) {
				try {
					start.setTime(YYYYMMDD_SDF.parse(jsonObject.get(START)
							.getAsString()));
				} catch (ParseException e) {
					throw new IllegalStateException(e);
				}
			}
			if (intervalBoundsMode == ReportIntervalBoundsMode.BOTH_BOUNDS
					&& jsonObject.has(INTERVAL_COUNT)) {
				intervalCount = jsonObject.get(INTERVAL_COUNT).getAsInt();
				computeEndFromIntervalCount();
			}
			if (jsonObject.has(TASK_SCOPE_PATH)) {
				taskScopePath = jsonObject.get(TASK_SCOPE_PATH).getAsString();
			}
			if (advancedMode) {
				if (jsonObject.has(COLLABORATORS_SELECTION_MODE)) {
					collaboratorsSelectionMode = ReportCollaboratorsSelectionMode
							.valueOf(jsonObject.get(
									COLLABORATORS_SELECTION_MODE)
									.getAsString());
				}

				if (collaboratorsSelectionMode == ReportCollaboratorsSelectionMode.SELECT_COLLABORATORS
						&& jsonObject.has(SELECTED_COLLABORATORS)) {
					JsonArray jsonArray = jsonObject
							.get(SELECTED_COLLABORATORS)
							.getAsJsonArray();
					Collaborator[] collaborators = new Collaborator[jsonArray
							.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						long id = jsonArray.get(i).getAsLong();
						collaborators[i] = getModelMgr().getCollaborator(id);
					}
					collaboratorsSelectionLogic.select(collaborators);
				}
				if (jsonObject.has(SELECTED_COLUMNS)) {
					JsonArray jsonArray = jsonObject.get(SELECTED_COLUMNS)
							.getAsJsonArray();
					DTOAttribute[] columns = new DTOAttribute[jsonArray
							.size()];
					for (int i = 0; i < jsonArray.size(); i++) {
						String id = jsonArray.get(i).getAsString();
						columns[i] = attributesMap.get(id);
					}
					columnsSelectionLogic.select(columns);
				}
			}
			if (jsonObject.has(TASK_TREE_DEPTH)) {
				taskTreeDepth = jsonObject.get(TASK_TREE_DEPTH).getAsInt();
			}
			if (advancedMode) {
				if (jsonObject.has(ONLY_KEEP_TASK_WITH_CONTRIBUTIONS)) {
					onlyKeepTaskWithContributions = jsonObject.get(
							ONLY_KEEP_TASK_WITH_CONTRIBUTIONS).getAsBoolean();
				}
			}
			updateUI();
		}
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

	private Calendar start;
	private ReportIntervalType intervalType;
	private Integer intervalCount;
	private Long rootTaskId;
	private int taskDepth;
	private boolean onlyKeepTasksWithContributions;
	private long[] contributorIds;
	private List<String> columnIds = new ArrayList<String>();

	public Calendar getStart() {
		return start;
	}

	public void setStart(Calendar start) {
		this.start = start;
	}

	public ReportIntervalType getIntervalType() {
		return intervalType;
	}

	public void setIntervalType(ReportIntervalType intervalType) {
		this.intervalType = intervalType;
	}

	public Integer getIntervalCount() {
		return intervalCount;
	}

	public void setIntervalCount(Integer intervalCount) {
		this.intervalCount = intervalCount;
	}

	public Long getRootTaskId() {
		return rootTaskId;
	}

	public void setRootTaskId(Long rootTaskId) {
		this.rootTaskId = rootTaskId;
	}

	public int getTaskDepth() {
		return taskDepth;
	}

	public void setTaskDepth(int taskDepth) {
		this.taskDepth = taskDepth;
	}

	public boolean isOnlyKeepTasksWithContributions() {
		return onlyKeepTasksWithContributions;
	}

	public void setOnlyKeepTasksWithContributions(
			boolean onlyKeepTasksWithContributions) {
		this.onlyKeepTasksWithContributions = onlyKeepTasksWithContributions;
	}

	public long[] getContributorIds() {
		return contributorIds;
	}

	public void setContributorIds(long[] contributorIds) {
		this.contributorIds = contributorIds;
	}

	public List<String> getColumnIds() {
		return columnIds;
	}

	boolean isContributorCentricMode() {
		return columnIds.size() > 0
				&& columnIds.get(0).startsWith(ReportsLogicImpl.COLLABORATOR);
	}

	boolean isByContributor() {
		for (String columnId : columnIds) {
			if (columnId.startsWith(ReportsLogicImpl.COLLABORATOR)) {
				return true;
			}
		}
		return false;
	}

}