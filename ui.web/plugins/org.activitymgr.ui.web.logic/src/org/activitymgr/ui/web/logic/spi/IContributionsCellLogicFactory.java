package org.activitymgr.ui.web.logic.spi;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.misc.TaskContributions;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.ContributionsCellLogicFatory;

/**
 * @author jbrazeau
 * @see ContributionsCellLogicFatory
 */
public interface IContributionsCellLogicFactory extends ICellLogicFactory {

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
			.unmodifiableList(Arrays.asList(new String[] { MON_COLUMN_ID,
					TUE_COLUMN_ID, WED_COLUMN_ID, THU_COLUMN_ID, FRI_COLUMN_ID,
					SAT_COLUMN_ID, SUN_COLUMN_ID }));
	public static final List<String> PROPERTY_IDS = Collections
			.unmodifiableList(Arrays.asList(new String[] { PATH_COLUMN_ID,
					NAME_COLUMN_ID, MON_COLUMN_ID, TUE_COLUMN_ID,
					WED_COLUMN_ID, THU_COLUMN_ID, FRI_COLUMN_ID, SAT_COLUMN_ID,
					SUN_COLUMN_ID, TOTAL_COLUMN_ID }));

	public abstract ILogic<?> createCellLogic(AbstractLogicImpl<?> parentLogic, ILogicContext context, Collaborator contributor,
			Calendar firstDayOfWeek, TaskContributions weekContributions,
			String propertyId);

	public abstract List<TaskContributions> loadContributions(
			Collaborator contributor, Calendar firstDayOfWeek)
			throws ModelException;

	public void loadDurations();
	
}