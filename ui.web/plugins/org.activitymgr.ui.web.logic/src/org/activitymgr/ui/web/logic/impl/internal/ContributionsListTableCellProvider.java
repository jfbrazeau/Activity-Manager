package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.IntervalContributions;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.AbstractEvent;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTextFieldLogicImpl;
import org.activitymgr.ui.web.logic.impl.LabelLogicImpl;
import org.activitymgr.ui.web.logic.impl.LogicContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

class ContributionsListTableCellProvider extends AbstractSafeTableCellProviderCallback<Long> {

	protected  static class ContributionChangeEvent extends AbstractEvent {

		/** The changed contribution */
		private Contribution contribution;

		/**
		 * Default constructor.
		 * @param source the event's source.
		 * @param contribution the changed contribution.
		 */
		public ContributionChangeEvent(ILogic<?> source, Contribution contribution) {
			super(source);
			this.contribution = contribution;
		}

		/**
		 * @return the changed contribution.
		 */
		public Contribution getChangedContribution() {
			return contribution;
		}

	}
	
	public static final String PATH_COLUMN_ID = "PATH";
	public static final String NAME_COLUMN_ID = "NAME";
	public static final String MON_COLUMN_ID = "MON";
	public static final String TUE_COLUMN_ID = "TUE";
	public static final String WED_COLUMN_ID = "WED";
	public static final String THU_COLUMN_ID = "THU";
	public static final String FRI_COLUMN_ID = "FRI";
	public static final String SAT_COLUMN_ID = "SAT";
	public static final String SUN_COLUMN_ID = "SUN";
	public static final String TOTAL_COLUMN_ID = "TOTAL";

	public static final List<String> DAY_COLUMNS_IDENTIFIERS = Collections
			.unmodifiableList(Arrays.asList(new String[] {
					MON_COLUMN_ID,
					TUE_COLUMN_ID,
					WED_COLUMN_ID,
					THU_COLUMN_ID,
					FRI_COLUMN_ID,
					SAT_COLUMN_ID,
					SUN_COLUMN_ID }));
	
	public static final List<String> PROPERTY_IDS = Collections
			.unmodifiableList(Arrays.asList(new String[] {
					PATH_COLUMN_ID,
					NAME_COLUMN_ID,
					MON_COLUMN_ID,
					TUE_COLUMN_ID,
					WED_COLUMN_ID,
					THU_COLUMN_ID,
					FRI_COLUMN_ID,
					SAT_COLUMN_ID,
					SUN_COLUMN_ID,
					TOTAL_COLUMN_ID}));

	private static final int DAY_COLUMN_WIDTH = 40;

	private static final Map<String, Integer> DEFAULT_COLUMN_WIDTHS = new HashMap<String, Integer>();

	static {
		DEFAULT_COLUMN_WIDTHS.put(PATH_COLUMN_ID, 250);
		DEFAULT_COLUMN_WIDTHS.put(NAME_COLUMN_ID, 150);
	}

	private IModelMgr modelMgr;
	
	private Map<Long, TaskContributions> contributionsMap = new HashMap<Long, TaskContributions>();
	
	private List<Long> taskIds = new ArrayList<Long>();
	private Collection<Long> unmodifiableTaskIds = Collections.unmodifiableCollection(taskIds);
	private Calendar firstDayOfWeek;
	private Collaborator contributor;
	private Map<String, String> footer = new HashMap<String, String>();

	public ContributionsListTableCellProvider(ILogic<?> source, LogicContext context) {
		super(source, context);
		this.modelMgr = context.getComponent(IModelMgr.class);
		this.contributor = context.getConnectedCollaborator();
		
		// Fake change to load contributions
		try {
			changeFirstDayOfWeek(new GregorianCalendar());
		} catch (ModelException e) {
			throw new IllegalStateException("Unexpected error while retrieving contributions", e);
		}
		
		// Update totals
		updateTotals();
	}

	private Cache<Long, Cache<String, ILogic<?>>> cellLogics = CacheBuilder.newBuilder().build(new CacheLoader<Long, Cache<String, ILogic<?>>>() {
		@Override
		public Cache<String, ILogic<?>> load(final Long taskId) throws Exception {
			return CacheBuilder.newBuilder().build(new CacheLoader<String, ILogic<?>>() {
				@Override
				public ILogic<?> load(String propertyId) throws Exception {
					final TaskContributions weekContributions = contributionsMap.get(taskId);
					final AbstractLogicImpl<?> source = (AbstractLogicImpl<?>) getSource();
					ILogic<?> logic = null;
					if (DAY_COLUMNS_IDENTIFIERS.contains(propertyId)) {
						final int dayOfWeek = DAY_COLUMNS_IDENTIFIERS.indexOf(propertyId);
						Contribution c = weekContributions.getContributions()[dayOfWeek];
						String duration = (c == null) ? "" : StringHelper.hundredthToEntry(c.getDurationId());
						ITextFieldLogic textFieldLogic = new AbstractSafeTextFieldLogicImpl(source, duration, false) {
							@Override
							protected void unsafeOnValueChanged(String newValue) {
								onDurationChanged(weekContributions, dayOfWeek, newValue, this);
							}
						};
						textFieldLogic.getView().setNumericFieldStyle();
						logic = textFieldLogic;
					}
					else if (PATH_COLUMN_ID.equals(propertyId)) {
						logic = new LabelLogicImpl(source, weekContributions.getTaskCodePath());
					}
					else if (NAME_COLUMN_ID.equals(propertyId)) {
						logic = new LabelLogicImpl(source, weekContributions.getTask().getName());
					}
					else if (TOTAL_COLUMN_ID.equals(propertyId)) {
						logic = new LabelLogicImpl(source, "");
					}
					else {
						throw new IllegalArgumentException(propertyId);
					}
					return logic;
				}
			});
		}
	});
	
	@Override
	protected IView<?> unsafeGetCell(final Long taskId, String propertyId) {
		try {
			return cellLogics.get(taskId).get(propertyId).getView();
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	private void onDurationChanged(TaskContributions weekContributions, int dayOfWeek, String duration, ITextFieldLogic textFieldLogic) {
		try {
			long durationId = 0;
			if (duration != null && !"".equals(duration.trim())) {
				durationId = StringHelper.entryToHundredth(duration.replaceAll(",", "."));
			}

			Contribution contribution = weekContributions.getContributions()[dayOfWeek];
			// First case : the contribution must be created
			if (contribution == null) {
				if (durationId != 0) {
					// Let's create the new contribution
					contribution = getContext().getBeanFactory().newContribution();
					contribution.setContributorId(getContext().getConnectedCollaborator().getId());
					contribution.setDurationId(durationId);
					contribution.setTaskId(weekContributions.getTask().getId());
					Calendar clone = (Calendar) firstDayOfWeek.clone();
					clone.add(Calendar.DATE, dayOfWeek);
					contribution.setDate(clone);
					modelMgr.createContribution(contribution, true);
					weekContributions.getContributions()[dayOfWeek] = contribution;
				}
			}
			// Second case : the contribution must be removed
			else if (durationId == 0) {
				// Let's remove the duration
				modelMgr.removeContribution(contribution, true);
				weekContributions.getContributions()[dayOfWeek] = null;
			}
			// Third case : the contribution must be updated
			else {
				// contribution update
				contribution.setDurationId(durationId);
				modelMgr.updateContribution(contribution, true);
			}
			
			// Update the view
			duration = durationId == 0 ? "" : StringHelper.hundredthToEntry(durationId);
			textFieldLogic.getView().setValue(duration);

			// Update totals
			updateTotals();

			// Reload the footer on the UI side
			((IContributionsTabLogic.View)getSource().getView()).reloadContributionTableFooter();
		}
		catch (ModelException e) {
			textFieldLogic.getView().focus();
			throw new IllegalStateException(e.getMessage(), e);
		}
		catch (StringFormatException e) {
			textFieldLogic.getView().focus();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	
	
	@Override
	protected Collection<String> unsafeGetPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	protected final synchronized Collection<Long> unsafeGetRootElements() throws Exception {
		return taskIds;
	}

	@Override
	protected final boolean unsafeContains(Long taskId) {
		return true;
	}

	protected void changeContributor(Collaborator contributor) throws ModelException {
		if (contributor == null || this.contributor.getId() != contributor.getId()) {
			this.contributor = contributor; 
			loadContributions();
		}
	}
	
	protected void changeFirstDayOfWeek(Calendar newDay) throws ModelException {
		firstDayOfWeek = moveToFirstDayOfWeek(newDay);
		loadContributions();
	}
	
	protected void changeFirstDayOfWeek(int amountType, int amount) throws ModelException {
		// Update date
		firstDayOfWeek.add(amountType, amount);
		firstDayOfWeek = moveToFirstDayOfWeek(firstDayOfWeek);
		loadContributions();
	}
	
	protected void addEmptyWeekContribution(long taskId) throws ModelException {
		Task task = modelMgr.getTask(taskId);
		TaskContributions weekContribution = new TaskContributions();
		weekContribution.setContributions(new Contribution[7]);
		weekContribution.setTask(task);
		weekContribution.setTaskCodePath(modelMgr.getTaskCodePath(task));
		// Insert the task at the right place
		taskIds.add(taskId);
		contributionsMap.put(taskId, weekContribution);
		// Resort the contributions
		sortWeekContributions();
	}

	private void loadContributions() throws ModelException {
		taskIds.clear();
		contributionsMap.clear();
		cellLogics.invalidateAll();

		if (contributor != null) {
			// Load week contributions
			// Recherche des taches déclarées pour cet utilisateur
			// pour la semaine courante (et la semaine passée pour
			// réafficher automatiquement les taches de la semaine
			// passée)
			Calendar fromDate = (Calendar) firstDayOfWeek.clone();
			fromDate.add(Calendar.DATE, -7);
			Calendar toDate = (Calendar) firstDayOfWeek.clone();
			toDate.add(Calendar.DATE, 6);
			IntervalContributions intervalContributions = modelMgr.getIntervalContributions(contributor, null, fromDate,
							toDate);
			TaskContributions[] weekContributions = intervalContributions.getTaskContributions();
			
			// The result contains the contributions of the previous
			// week. We truncate it before proceeding.
			for (TaskContributions tc : weekContributions) {
				Contribution[] newContribs = new Contribution[7];
				System.arraycopy(tc.getContributions(), 7,
						newContribs, 0, 7);
				tc.setContributions(newContribs);
				long taskId = tc.getTask().getId();
				// Populate cache
				taskIds.add(taskId);
				contributionsMap.put(taskId, tc);
			}
		
			// Sort the tasks
			sortWeekContributions();

			// Update totals
			updateTotals();
		}
	}

	private void sortWeekContributions() {
		// Sort task identifiers
		Collections.sort(taskIds, new Comparator<Long>() {
			@Override
			public int compare(Long taskId1, Long taskId2) {
				Task task1 = contributionsMap.get(taskId1).getTask();
				Task task2 = contributionsMap.get(taskId2).getTask();
				return task1.getFullPath().compareTo(task2.getFullPath());
			}
		});
	}

	public Calendar getFirstDayOfWeek() {
		return firstDayOfWeek;
	}

	public Collaborator getContributor() {
		return contributor;
	}

	private static Calendar moveToFirstDayOfWeek(Calendar date) {
		Calendar dateCursor = (Calendar) date.clone();
		while (dateCursor.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
			dateCursor.add(Calendar.DATE, -1);
		return dateCursor;
	}

	public Collection<Long> getTaskIds() {
		return unmodifiableTaskIds;
	}

	@Override
	protected Integer unsafeGetColumnWidth(String propertyId) {
		return DEFAULT_COLUMN_WIDTHS.containsKey(propertyId) ? DEFAULT_COLUMN_WIDTHS.get(propertyId) : DAY_COLUMN_WIDTH;
	}

	private void updateTotals() {
		long total = 0;
		for (int dayOfWeek=0; dayOfWeek<7; dayOfWeek++) {
			long dayTotal = 0;
			for (long taskId : taskIds) {
				TaskContributions tc = contributionsMap.get(taskId);
				Contribution c = tc.getContributions()[dayOfWeek];
				if (c != null) {
					dayTotal += c.getDurationId();
					total += c.getDurationId();
				}
			}
			footer.put(DAY_COLUMNS_IDENTIFIERS.get(dayOfWeek), StringHelper
					.hundredthToEntry(dayTotal));
		}
		footer.put(TOTAL_COLUMN_ID, StringHelper
				.hundredthToEntry(total));
		// Update the week contributions total
		for (long taskId : taskIds) {
			TaskContributions tc = contributionsMap.get(taskId);
			long taskTotal = 0;
			for (int dayOfWeek=0; dayOfWeek<7; dayOfWeek++) {
				Contribution c = tc.getContributions()[dayOfWeek];
				if (c != null) {
					taskTotal += c.getDurationId();
				}
			}
			((ILabelLogic.View)unsafeGetCell(taskId, TOTAL_COLUMN_ID)).setLabel(taskTotal != 0 ? 
					StringHelper.hundredthToEntry(taskTotal) : "");
		}
	}

	@Override
	protected String unsafeGetFooter(String propertyId) {
		return footer.get(propertyId);
	}

}