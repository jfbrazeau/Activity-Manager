package org.activitymgr.ui.web.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.activitymgr.core.beans.TaskContributions;

public interface IContributionCellLogicProvider {

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

	ILogic<?> getCellLogic(IContributionsLogic parent,
			String columnId, TaskContributions weekContributions);
	
}
