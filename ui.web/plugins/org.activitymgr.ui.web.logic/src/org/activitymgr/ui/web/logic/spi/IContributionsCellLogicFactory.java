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

	String PATH_COLUMN_ID = "PATH";
	String NAME_COLUMN_ID = "NAME";
	String MON_COLUMN_ID = "MON";
	String TUE_COLUMN_ID = "TUE";
	String WED_COLUMN_ID = "WED";
	String THU_COLUMN_ID = "THU";
	String FRI_COLUMN_ID = "FRI";
	String SAT_COLUMN_ID = "SAT";
	String SUN_COLUMN_ID = "SUN";
	String TOTAL_COLUMN_ID = "TOTAL";
	List<String> DAY_COLUMNS_IDENTIFIERS = Collections
			.unmodifiableList(Arrays.asList(new String[] { MON_COLUMN_ID,
					TUE_COLUMN_ID, WED_COLUMN_ID, THU_COLUMN_ID, FRI_COLUMN_ID,
					SAT_COLUMN_ID, SUN_COLUMN_ID }));
	List<String> PROPERTY_IDS = Collections
			.unmodifiableList(Arrays.asList(new String[] { PATH_COLUMN_ID,
					NAME_COLUMN_ID, MON_COLUMN_ID, TUE_COLUMN_ID,
					WED_COLUMN_ID, THU_COLUMN_ID, FRI_COLUMN_ID, SAT_COLUMN_ID,
					SUN_COLUMN_ID, TOTAL_COLUMN_ID }));

	ILogic<?> createCellLogic(AbstractLogicImpl<?> parentLogic, ILogicContext context, Collaborator contributor,
			Calendar firstDayOfWeek, TaskContributions weekContributions,
			String propertyId);

	List<TaskContributions> loadContributions(
			Collaborator contributor, Calendar firstDayOfWeek)
			throws ModelException;

	TaskContributions newTaskContributions();

	void loadDurations();
	
}