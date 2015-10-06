package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

class ContributionsListTableCellProvider extends AbstractSafeTableCellProviderCallback<Long> {
	
	@Inject
	private IModelMgr modelMgr;
	
	@Inject
	private IContributionsCellLogicFactory cellLogicFactory;

	@Inject
	private ILogicContext context;

	private Map<Long, TaskContributions> contributionsMap = new HashMap<Long, TaskContributions>();
	private List<Long> taskIds = new ArrayList<Long>();
	private Collection<Long> unmodifiableTaskIds = Collections.unmodifiableCollection(taskIds);
	private Calendar firstDayOfWeek;
	private Collaborator contributor;

	public ContributionsListTableCellProvider(AbstractContributionTabLogicImpl source) {
		super(source);
		this.contributor = getContext().getConnectedCollaborator();
	}

	private LoadingCache<Long, LoadingCache<String, ILogic<?>>> cellLogics = CacheBuilder.newBuilder().build(new CacheLoader<Long, LoadingCache<String, ILogic<?>>>() {
		@Override
		public LoadingCache<String, ILogic<?>> load(final Long taskId) throws Exception {
			return CacheBuilder.newBuilder().build(new CacheLoader<String, ILogic<?>>() {
				@Override
				public ILogic<?> load(String propertyId) throws Exception {
					return cellLogicFactory.createCellLogic((AbstractLogicImpl<?>) getSource(), context, contributor, firstDayOfWeek, contributionsMap.get(taskId), propertyId);
				}
			});
		}
	});
	
	@Override
	protected IView<?> unsafeGetCell(final Long taskId, String propertyId) {
		return getCellLogic(taskId, propertyId).getView();
	}

	@Override
	protected Collection<String> unsafeGetPropertyIds() {
		return cellLogicFactory.getPropertyIds();
	}

	@Override
	protected Collection<Long> unsafeGetRootElements() throws Exception {
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
	
	protected void updateTaskTotals() {
		for (long taskId : taskIds) {
			updateTaskTotal(taskId);
		}
	}
	
	protected void updateTaskTotal(long taskId) {
		ILogic<?> cellLogic = getCellLogic(taskId, IContributionsCellLogicFactory.TOTAL_COLUMN_ID);
		if (cellLogic != null && cellLogic instanceof ILabelLogic) {
			TaskContributions weekContributions = contributionsMap.get(taskId);
			long total = 0;
			for (Contribution c : weekContributions.getContributions()) {
				if (c != null) {
					total += c.getDurationId();
				}
			}
			((ILabelLogic) cellLogic).getView().setLabel(StringHelper.hundredthToEntry(total));
		}
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
			List<TaskContributions> weekContributions = cellLogicFactory.loadContributions(contributor, firstDayOfWeek);
			for (TaskContributions tc : weekContributions) {
				long taskId = tc.getTask().getId();
				// Populate cache
				taskIds.add(taskId);
				contributionsMap.put(taskId, tc);
			}

			// Sort the tasks
			sortWeekContributions();
			
			// Update task totals
			updateTaskTotals();
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
		return cellLogicFactory.getColumnWidth(propertyId);
	}

	@Override
	protected String unsafeGetFooter(String propertyId) {
		int dayIdx = IContributionsCellLogicFactory.DAY_COLUMNS_IDENTIFIERS.indexOf(propertyId);
		if (dayIdx >= 0) {
			long total = 0;
			for (TaskContributions tc : contributionsMap.values()) {
				Contribution contribution = tc.getContributions()[dayIdx];
				if (contribution != null) {
					total += contribution.getDurationId();
				}
			}
			return StringHelper.hundredthToEntry(total);
		}
		else if (IContributionsCellLogicFactory.TOTAL_COLUMN_ID.equals(propertyId)) {
			long total = 0;
			for (TaskContributions tc : contributionsMap.values()) {
				for (Contribution contribution : tc.getContributions()) {
					if (contribution != null) {
						total += contribution.getDurationId();
					}
				}
			}
			return StringHelper.hundredthToEntry(total);
		}
		else {
			return null;
		}
	}

	@Deprecated
	protected TaskContributions getWeekContributions(long taskId) {
		return contributionsMap.get(taskId);
	}

	@Deprecated
	public ILogic<?> getCellLogic(long taskId, String propertyId) {
		try {
			return cellLogics.get(taskId).get(propertyId);
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

}