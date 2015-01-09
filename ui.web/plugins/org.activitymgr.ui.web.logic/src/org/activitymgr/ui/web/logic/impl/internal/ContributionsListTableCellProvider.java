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
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.AbstractContributionTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.ContributionsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.LogicContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

class ContributionsListTableCellProvider extends AbstractSafeTableCellProviderCallback<Long> {
	
	private IModelMgr modelMgr;
	private Map<Long, TaskContributions> contributionsMap = new HashMap<Long, TaskContributions>();
	private List<Long> taskIds = new ArrayList<Long>();
	private Collection<Long> unmodifiableTaskIds = Collections.unmodifiableCollection(taskIds);
	private Calendar firstDayOfWeek;
	private Collaborator contributor;
	private Map<String, String> footer = new HashMap<String, String>();
	private ContributionsCellLogicFatory cellLogicFactory;

	public ContributionsListTableCellProvider(AbstractContributionTabLogicImpl source, LogicContext context) {
		super(source, context);
		this.modelMgr = context.getComponent(IModelMgr.class);
		this.contributor = context.getConnectedCollaborator();
		this.cellLogicFactory = context.getSingletonExtension("org.activitymgr.ui.web.logic.contributionsCellLogicFactory", ContributionsCellLogicFatory.class, AbstractContributionTabLogicImpl.class, source);
	}

	private Cache<Long, Cache<String, ILogic<?>>> cellLogics = CacheBuilder.newBuilder().build(new CacheLoader<Long, Cache<String, ILogic<?>>>() {
		@Override
		public Cache<String, ILogic<?>> load(final Long taskId) throws Exception {
			return CacheBuilder.newBuilder().build(new CacheLoader<String, ILogic<?>>() {
				@Override
				public ILogic<?> load(String propertyId) throws Exception {
					return cellLogicFactory.createCellLogic(contributor, firstDayOfWeek, contributionsMap.get(taskId), propertyId);
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
		return footer.get(propertyId);
	}

	protected TaskContributions getWeekContributions(long taskId) {
		return contributionsMap.get(taskId);
	}

	protected void setFooter(String propertyId, String text) {
		footer.put(propertyId, text);
	}

	public ILogic<?> getCellLogic(long taskId, String propertyId) {
		try {
			return cellLogics.get(taskId).get(propertyId);
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

}