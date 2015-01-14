package org.activitymgr.ui.web.logic.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.misc.IntervalContributions;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.event.ContributionChangeEvent;

public class ContributionsCellLogicFatory {

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
	private AbstractContributionTabLogicImpl parentLogic;
	private LogicContext context;

	public ContributionsCellLogicFatory(AbstractContributionTabLogicImpl parentLogic, LogicContext context) {
		this.parentLogic = parentLogic;
		this.context = context;
		this.modelMgr = context.getComponent(IModelMgr.class);
	}

	public ILogic<?> createCellLogic(final Collaborator contributor, final Calendar firstDayOfWeek, final TaskContributions weekContributions, final String propertyId) {
		ILogic<?> logic = null;
		if (DAY_COLUMNS_IDENTIFIERS.contains(propertyId)) {
			final int dayOfWeek = DAY_COLUMNS_IDENTIFIERS.indexOf(propertyId);
			Contribution c = weekContributions.getContributions()[dayOfWeek];
			String duration = (c == null) ? "" : StringHelper.hundredthToEntry(c.getDurationId());
			ITextFieldLogic textFieldLogic = new AbstractSafeTextFieldLogicImpl(parentLogic, duration, false) {
				@Override
				protected void unsafeOnValueChanged(String newValue) {
					onDurationChanged(contributor, firstDayOfWeek, weekContributions, dayOfWeek, newValue, this, propertyId);
				}
			};
			textFieldLogic.getView().setNumericFieldStyle();
			logic = textFieldLogic;
		}
		else if (PATH_COLUMN_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, weekContributions.getTaskCodePath());
		}
		else if (NAME_COLUMN_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, weekContributions.getTask().getName());
		}
		else if (TOTAL_COLUMN_ID.equals(propertyId)) {
			logic = new LabelLogicImpl(parentLogic, "");
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
		return logic;
	}

	protected LogicContext getContext() {
		return context;
	}

	private void onDurationChanged(Collaborator contributor, Calendar firstDayOfWeek, TaskContributions weekContributions, int dayOfWeek, String duration, ITextFieldLogic textFieldLogic, String propertyId) {
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
					contribution.setContributorId(contributor.getId());
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

			// FIre a change event
			long oldDuration = contribution != null ? contribution.getDurationId() : 0;
			getContext().getEventBus().fire(
					new ContributionChangeEvent(getContributionLogic(),
							weekContributions.getTask().getId(), propertyId,
							oldDuration, durationId));
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
	
	public Collection<String> getPropertyIds() {
		return PROPERTY_IDS;
	}

	public List<TaskContributions> loadContributions(Collaborator contributor, Calendar firstDayOfWeek) throws ModelException {
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
		}
		return Arrays.asList(weekContributions);
	}

	public Integer getColumnWidth(String propertyId) {
		return DEFAULT_COLUMN_WIDTHS.containsKey(propertyId) ? DEFAULT_COLUMN_WIDTHS.get(propertyId) : DAY_COLUMN_WIDTH;
	}

	protected AbstractContributionTabLogicImpl getContributionLogic() {
		return parentLogic;
	}

}