package org.activitymgr.ui.web.logic.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.misc.IntervalContributions;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.StringFormatException;
import org.activitymgr.core.util.StringHelper;
import org.activitymgr.ui.web.logic.Align;
import org.activitymgr.ui.web.logic.IFieldLogic;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.ISelectFieldLogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.impl.event.ContributionChangeEvent;
import org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory;

import com.google.inject.Inject;

public class ContributionsCellLogicFatory implements IContributionsCellLogicFactory {

	private static final int DAY_COLUMN_WIDTH_WITH_SELECT_FIELD = 60;
	private static final int DAY_COLUMN_WIDTH_WITH_TEXT_FIELD = 40;

	private static final Map<String, Integer> DEFAULT_COLUMN_WIDTHS = new HashMap<String, Integer>();

	static {
		DEFAULT_COLUMN_WIDTHS.put(PATH_COLUMN_ID, 150);
		DEFAULT_COLUMN_WIDTHS.put(NAME_COLUMN_ID, 150);
		DEFAULT_COLUMN_WIDTHS.put(TOTAL_COLUMN_ID, 40);
	}

	@Inject
	private IModelMgr modelMgr;
	
	@Inject
	private IDTOFactory dtoFactory;
	
	private Map<String, String> durationsMap;

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.IContributionsCellLogicFactory#createCellLogic(org.activitymgr.core.dto.Collaborator, java.util.Calendar, org.activitymgr.core.dto.misc.TaskContributions, java.lang.String)
	 */
	@Override
	public ILogic<?> createCellLogic(final AbstractLogicImpl<?> parentLogic, final ILogicContext context, final Collaborator contributor, final Calendar firstDayOfWeek, final TaskContributions weekContributions, final String propertyId) {
		ILogic<?> logic = null;
		if (DAY_COLUMNS_IDENTIFIERS.contains(propertyId)) {
			final int dayOfWeek = DAY_COLUMNS_IDENTIFIERS.indexOf(propertyId);
			Contribution c = weekContributions.getContributions()[dayOfWeek];
			String duration = (c == null) ? "" : StringHelper.hundredthToEntry(c.getDurationId());
			if (contributionsAsTextfield()) {
				ITextFieldLogic textFieldLogic = new AbstractSafeTextFieldLogicImpl(parentLogic, duration, false) {
					@Override
					protected void unsafeOnValueChanged(String newValue) {
						onDurationChanged(parentLogic, contributor, firstDayOfWeek, weekContributions, dayOfWeek, newValue, this, propertyId);
					}
				};
				textFieldLogic.getView().setNumericFieldStyle();
				logic = textFieldLogic;
			}
			else {
				ISelectFieldLogic<String> selectFieldLogic = new AbstractSafeSelectLogicImpl<String>(parentLogic, durationsMap, duration) {
					@Override
					protected void unsafeOnSelectedItemChanged(
							String newValue) throws Exception {
						onDurationChanged(parentLogic, contributor, firstDayOfWeek, weekContributions, dayOfWeek, newValue, this, propertyId);
					}
				};
				selectFieldLogic.getView().setWidth(DAY_COLUMN_WIDTH_WITH_SELECT_FIELD);
				logic = selectFieldLogic;
			}
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

	private void onDurationChanged(AbstractLogicImpl<?> parentLogic, Collaborator contributor, Calendar firstDayOfWeek, TaskContributions weekContributions, int dayOfWeek, String duration, IFieldLogic<String, ? extends ILogic.IView<?>> logic, String propertyId) {
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
					contribution = dtoFactory.newContribution();
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
			logic.getView().setValue(duration);

			// FIre a change event
			long oldDuration = contribution != null ? contribution.getDurationId() : 0;
			parentLogic.getEventBus().fire(
					new ContributionChangeEvent(parentLogic,
							weekContributions.getTask().getId(), propertyId,
							oldDuration, durationId));
		}
		catch (ModelException e) {
			logic.getView().focus();
			throw new IllegalStateException(e.getMessage(), e);
		}
		catch (StringFormatException e) {
			logic.getView().focus();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.IContributionsCellLogicFactory#getPropertyIds()
	 */
	@Override
	public Collection<String> getPropertyIds() {
		return PROPERTY_IDS;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.IContributionsCellLogicFactory#loadContributions(org.activitymgr.core.dto.Collaborator, java.util.Calendar)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory#loadDurations()
	 */
	public void loadDurations() {
		durationsMap = null;
		if (!contributionsAsTextfield()) {
			durationsMap = new LinkedHashMap<String, String>();
			Duration[] durations = modelMgr.getDurations();
			for (Duration duration : durations) {
				String str = StringHelper.hundredthToEntry(duration.getId());
				// We index the duration by itself
				durationsMap.put(str, str);
			}
		}
	}

	protected boolean contributionsAsTextfield() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.IContributionsCellLogicFactory#getColumnWidth(java.lang.String)
	 */
	@Override
	public Integer getColumnWidth(String propertyId) {
		if (DEFAULT_COLUMN_WIDTHS.containsKey(propertyId)) {
			return DEFAULT_COLUMN_WIDTHS.get(propertyId);
		} else if (contributionsAsTextfield()) {
			 return DAY_COLUMN_WIDTH_WITH_TEXT_FIELD;
		} else {
			 return DAY_COLUMN_WIDTH_WITH_SELECT_FIELD;
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.spi.ICellLogicFactory#getColumnAlign(java.lang.String)
	 */
	@Override
	public Align getColumnAlign(String propertyId) {
		return null;
	}
}