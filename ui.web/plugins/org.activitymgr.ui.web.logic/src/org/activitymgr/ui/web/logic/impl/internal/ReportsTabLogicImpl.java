package org.activitymgr.ui.web.logic.impl.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

	private static final String TASK = Task.class.getSimpleName().toLowerCase();

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

	public ReportsTabLogicImpl(ITabFolderLogic parent, boolean advancedMode) {
		super(parent);
		this.advancedMode = advancedMode;

		// Global initializations
		onlyKeepTaskWithContributions = !advancedMode; // in basic mode, only
														// keep non empty tasks
														// by default
		tabLabel = advancedMode ? "Adv. reports" : "My reports";
		collaboratorsSelectionMode = advancedMode ? ReportCollaboratorsSelectionMode.ALL_COLLABORATORS
				: ReportCollaboratorsSelectionMode.ME;

		// Initialize view
		getView().initialize(advancedMode);

		// Add buttons
		registerButtons(buttonFactories);
		for (ReportIntervalType type : ReportIntervalType.values()) {
			getView().addIntervalTypeRadioButton(type, StringHelper.toLowerFirst(type.name().toLowerCase()));
		}
		for (ReportIntervalBoundsMode mode : ReportIntervalBoundsMode.values()) {
			getView().addIntervalBoundsModeRadioButton(mode, StringHelper.toLowerFirst(mode.name().replace('_',  ' ').toLowerCase()));
		}
		if (advancedMode) {
			for (ReportCollaboratorsSelectionMode mode : ReportCollaboratorsSelectionMode.values()) {
				getView().addCollaboratorsSelectionModeRadioButton(
						mode,
						StringHelper.toLowerFirst(mode.name().replace('_', ' ')
								.toLowerCase()));
			}
		}
		/*
		 * Collaborators twin select
		 */
		if (advancedMode) {
			collaborators = getModelMgr().getCollaborators();
			collaboratorsSelectionLogic = new AbstractSafeTwinSelectFieldLogic<Collaborator>(
					this, false, COLLABORATORS_INFOS_PROVIDER, collaborators) {
				@Override
				protected void unsafeOnValueChanged(Collection<String> newValue)
						throws Exception {
					updateUI();
				}
			};
			getView().setCollaboratorsSelectionView(
					collaboratorsSelectionLogic.getView());
		}
		try {
			/*
			 * Attributes twin select
			 */
			List<DTOAttribute> attributes = new ArrayList<DTOAttribute>();
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
			Map<String, DTOAttribute> map = new HashMap<String, DTOAttribute>();
			for (DTOAttribute att : attributes) {
				map.put(att.getId(), att);
			}
			if (advancedMode) {
				columnsSelectionLogic.select(map.get("task.path"),
						map.get("task.name"),
						map.get("collaborator.login"));
				getView().setColumnSelectionView(
						columnsSelectionLogic.getView());
			} else {
				columnsSelectionLogic.select(map.get("task.path"),
						map.get("task.name"));
			}

			getView().setTaskTreeDepth(taskTreeDepth);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
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
		getView().setBuildReportButtonView(
				downloadReportButtonLogic.getView());
		updateUI();
	}

	private static void appendDTOAttributes(List<DTOAttribute> attributes,
			Object dto, String dtoLabel) throws ReflectiveOperationException {
		for (Object property : BeanUtils.describe(dto).keySet()) {
			if (!"class".equals(property)) {
				DTOAttribute att = new DTOAttribute(dtoLabel + "." + property,
						property + " (" + dtoLabel + ")");
				attributes.add(att);
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

	@Override
	public void onBuildHtmlReportButtonClicked() {
		StringWriter url = new StringWriter();
		url.append("/service/load");
		appendUrlParam(url, true, "service", "/service/report/html");
		appendUrlParam(url,
				AbstractReportServiceLogic.INTERVAL_TYPE_PARAMETER,
				intervalType.toString());
		appendUrlParam(url, AbstractReportServiceLogic.START_PARAMETER,
				new SimpleDateFormat("yyyyMMdd").format(start.getTime()));
		appendUrlParam(url,
				AbstractReportServiceLogic.INTERVAL_COUNT_PARAMETER,
				intervalCount);
		appendUrlParam(url, AbstractReportServiceLogic.ROOT_TASK_PARAMETER,
				taskScopePath);
		if (advancedMode) {
			switch (collaboratorsSelectionMode) {
			case ME:
				appendUrlParam(url,
						AbstractReportServiceLogic.CONTRIBUTOR_IDS_PARAMETERS,
						getContext().getConnectedCollaborator().getId());
			case ALL_COLLABORATORS:
				appendUrlParam(url,
						AbstractReportServiceLogic.CONTRIBUTOR_IDS_PARAMETERS,
						"*");
			case SELECT_COLLABORATORS:
				List<Collaborator> selectedCollaborators = collaboratorsSelectionLogic
						.getValue();
				Object[] contributorIds = new Object[selectedCollaborators
						.size()];
				int idx = 0;
				for (Collaborator selectedCollaborator : selectedCollaborators) {
					contributorIds[idx++] = selectedCollaborator.getId();
				}
				appendUrlParam(url,
						AbstractReportServiceLogic.CONTRIBUTOR_IDS_PARAMETERS,
						contributorIds);
			}
			List<DTOAttribute> selectedAttributes = columnsSelectionLogic
					.getValue();
			Object[] columnIds = new String[selectedAttributes.size()];
			int idx = 0;
			for (DTOAttribute selectedAttribute : selectedAttributes) {
				columnIds[idx++] = selectedAttribute.getId();
			}
			appendUrlParam(url,
					AbstractReportServiceLogic.COLUMN_IDS_PARAMETER, columnIds);
		}
		appendUrlParam(url, AbstractReportServiceLogic.TASK_DEPTH_PARAMETER,
				taskTreeDepth);
		if (advancedMode) {
			appendUrlParam(
					url,
					AbstractReportServiceLogic.ONLY_KEEP_TASKS_WITH_CONTRIBUTIONS_PARAMETER,
					onlyKeepTaskWithContributions);
		}
		ExternalContentDialogLogicImpl popup = new ExternalContentDialogLogicImpl(
				this, "Preview", url.toString());
		getRoot().getView().openWindow(popup.getView());
	}

	private static void appendUrlParam(StringWriter sw, String param,
			Object... values) {
		appendUrlParam(sw, false, param, values);
	}

	private static void appendUrlParam(StringWriter sw, boolean first,
			String param, Object... values) {
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
			if (intervalBoundsMode == ReportIntervalBoundsMode.AUTOMATIC) {
				Long rootTaskId = null;
				if (taskScopePath != null && taskScopePath.trim().length() > 0) {
					Task selectedTask = getModelMgr().getTaskByCodePath(
							taskScopePath);
					rootTaskId = selectedTask != null ? selectedTask.getId()
							: null;
				}
				Calendar[] interval = getModelMgr().getContributionsInterval(rootTaskId);
				start = interval[0];
				end = interval[1];
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
			downloadReportButtonLogic.getView().setEnabled(true);
		} catch (ModelException e) {
			downloadReportButtonLogic.getView().setEnabled(false);
			getView().setErrorMessage(e.getMessage());
		}
	}

	private Workbook buildReport(boolean dryRun) throws ModelException {
		Calendar start = null;
		if (intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC) {
			start = ReportsTabLogicImpl.this.start;
		}

		Long rootTaskId = null;
		if (taskScopePath != null && taskScopePath.trim().length() > 0) {
			Task selectedTask = getModelMgr().getTaskByCodePath(taskScopePath);
			rootTaskId = selectedTask != null ? selectedTask.getId() : null;
		}

		List<DTOAttribute> selectedColumns = columnsSelectionLogic.getValue();
		String[] selectedColumnIds = new String[selectedColumns.size()];
		for (int i = 0; i < selectedColumns.size(); i++) {
			selectedColumnIds[i] = selectedColumns.get(i).getId();
		}
		boolean contributorCentricMode = selectedColumns.size() > 0
				&& selectedColumns.get(0).getId()
				.startsWith(COLLABORATOR);
		boolean includeCollaborators = containsDTOAttribute(selectedColumns,
					COLLABORATOR);

		boolean includeTasks = containsDTOAttribute(selectedColumns, TASK);

		long[] contributorIds = null;
		switch (collaboratorsSelectionMode) {
		case ME:
			contributorIds = new long[] { getContext()
					.getConnectedCollaborator().getId() };
			break;
		case ALL_COLLABORATORS:
			contributorIds = null;
			break;
		case SELECT_COLLABORATORS:
			List<Collaborator> selectedCollaborators = collaboratorsSelectionLogic
					.getValue();
			if (selectedCollaborators.size() == 0)
				throw new ModelException(
						"At least one collaborator must be selected");
			if (selectedCollaborators.size() != collaborators.length) {
				contributorIds = new long[selectedCollaborators.size()];
				for (int i = 0; i < selectedCollaborators.size(); i++) {
					contributorIds[i] = selectedCollaborators.get(i).getId();
				}
			}
		}

		Workbook report = getModelMgr().buildReport(start, // Start date
				intervalType, // Interval type
						intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC ? intervalCount
								: null, // Interval count
				rootTaskId, // Root task id
				includeTasks ? taskTreeDepth : 0, // Task tree
													// depth
				onlyKeepTaskWithContributions, // Only keep tasks with
												// contributions
				includeCollaborators, // Include collaborators
				contributorCentricMode, // Collaborators centric mode
				contributorIds, // Contributor ids
				selectedColumnIds, // Column ids
				dryRun);
		return report;
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
