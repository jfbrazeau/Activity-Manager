package org.activitymgr.ui.web.logic.impl.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
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
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.inject.Inject;

public class ReportsTabLogicImpl extends
		AbstractTabLogicImpl<IReportsTabLogic.View> implements IReportsTabLogic {

	static enum ReportIntervalBoundsMode {
		AUTOMATIC, LOWER_BOUND, BOTH_BOUNDS
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
	
	private String taskScopePath;

	private int taskTreeDepth = 1;

	private AbstractSafeTwinSelectFieldLogic<Collaborator> collaboratorsSelectionLogic;

	private AbstractSafeTwinSelectFieldLogic<DTOAttribute> columnsSelectionLogic;

	private Collaborator[] collaborators;
	
	private boolean initialized = false;

	public ReportsTabLogicImpl(ITabFolderLogic parent) {
		super(parent);
		// Add buttons
		registerButtons(buttonFactories);
		for (ReportIntervalType type : ReportIntervalType.values()) {
			getView().addIntervalTypeRadioButton(type, StringHelper.toLowerFirst(type.name().toLowerCase()));
		}
		for (ReportIntervalBoundsMode mode : ReportIntervalBoundsMode.values()) {
			getView().addIntervalBoundsModeRadioButton(mode, StringHelper.toLowerFirst(mode.name().replace('_',  ' ').toLowerCase()));
		}
		getView().setTaskTreeDepth(taskTreeDepth);

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
		collaboratorsSelectionLogic.selectAll();
		getView().setCollaboratorsSelectionView(
				collaboratorsSelectionLogic.getView());
		try {
			/*
			 * Attributes twin select
			 */
			List<DTOAttribute> attributes = new ArrayList<DTOAttribute>();
			appendDTOAttributes(attributes, dtoFactory.newTask(), "task");
			appendDTOAttributes(attributes, dtoFactory.newCollaborator(),
					"collaborator");
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
			columnsSelectionLogic.select(map.get("task.path"),
					map.get("task.code"), map.get("collaborator.login"));
			getView().setColumnSelectionView(columnsSelectionLogic.getView());

			updateUI();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		getView().setBuildReportButtonView(
				new AbstractSafeDownloadButtonLogicImpl(this, "Build report",
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
						return "am-report-" + System.currentTimeMillis()
								+ ".xls";
					}

				}.getView());
		initialized = true;
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
		return "Reports";
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
					getView().setTaskScopePath(
							getModelMgr().getTaskCodePath(task));
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
	public void onTaskScopePathChanged(String value) {
		taskScopePath = value;
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

	void updateUI() {
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
			getView().setIntervalBounds(start.getTime(), end.getTime());
		}
		try {
			if (initialized) {
				buildReport(true);
				getView().setBuildReportButtonEnabled(true);
			}
		} catch (ModelException e) {
			getView().setBuildReportButtonEnabled(false);
			getView().setErrorMessage(e.getMessage());
		}

	}

	private Workbook buildReport(boolean dryRun) throws ModelException {
		Calendar start = null;
		if (intervalBoundsMode != ReportIntervalBoundsMode.AUTOMATIC) {
			start = ReportsTabLogicImpl.this.start;
		}
		Integer intervalCount = null;
		if (intervalBoundsMode == ReportIntervalBoundsMode.BOTH_BOUNDS) {
			Calendar end = ReportsTabLogicImpl.this.end;
			if (start.getTime().compareTo(end.getTime()) > 0)
				throw new ModelException(
						"Invalid interval ; start date must be before end date");
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

		Long rootTaskId = null;
		if (taskScopePath != null && taskScopePath.trim().length() > 0) {
			Task selectedTask = getModelMgr().getTaskByCodePath(taskScopePath);
			rootTaskId = selectedTask != null ? selectedTask.getId() : null;
		}

		boolean includeCollaborators = false;
		boolean includeTasks = false;
		List<DTOAttribute> selectedColumns = columnsSelectionLogic.getValue();
		for (DTOAttribute att : selectedColumns) {
			if (att.getId().startsWith("collaborator")) {
				includeCollaborators = true;
			} else {
				includeTasks = true;
			}
		}
		if (selectedColumns.size() == 0) {
			throw new ModelException(
					"At least one column must be selected");
		}
		boolean contributorCentricMode = selectedColumns.get(0).getId()
				.startsWith("collaborator");
		String[] selectedColumnIds = new String[selectedColumns.size()];
		for (int i = 0; i < selectedColumns.size(); i++) {
			selectedColumnIds[i] = selectedColumns.get(i).getId();
		}

		List<Collaborator> selectedCollaborators = collaboratorsSelectionLogic
				.getValue();
		if (selectedCollaborators.size() == 0)
			throw new ModelException(
					"At least one collaborator must be selected");
		long[] contributorIds = null;
		if (selectedCollaborators.size() != collaborators.length) {
			contributorIds = new long[selectedCollaborators.size()];
			for (int i = 0; i < selectedCollaborators.size(); i++) {
				contributorIds[i] = selectedCollaborators.get(i).getId();
			}
		}

		Workbook report = getModelMgr().buildReport(start, // Start date
				intervalType, // Interval type
				intervalCount, // Interval count
				rootTaskId, // Root task id
				includeTasks ? taskTreeDepth : 0, // Task tree
													// depth
				false, // Only keep tasks with contributions
				includeCollaborators, // Include collaborators
				contributorCentricMode, // Collaborators centric mode
				contributorIds, // Contributor ids
				selectedColumnIds, // Column ids
				dryRun);
		return report;
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
