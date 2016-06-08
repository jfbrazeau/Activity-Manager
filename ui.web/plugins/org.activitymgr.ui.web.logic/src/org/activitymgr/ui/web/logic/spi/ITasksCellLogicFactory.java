package org.activitymgr.ui.web.logic.spi;

import java.util.Arrays;
import java.util.List;

import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.TasksCellLogicFatory;

/**
 * @author jbrazeau
 * @see TasksCellLogicFatory
 */
public interface ITasksCellLogicFactory extends ICellLogicFactory {

	public static final String NAME_PROPERTY_ID = "NAME";
	public static final String CODE_PROPERTY_ID = "CODE";
	public static final String BUDGET_PROPERTY_ID = "BUDGET";
	public static final String INITIAL_PROPERTY_ID = "INITIAL";
	public static final String CONSUMMED_PROPERTY_ID = "CONSUMMED";
	public static final String ETC_PROPERTY_ID = "ETC";
	public static final String DELTA_PROPERTY_ID = "DELTA";
	public static final String COMMENT_PROPERTY_ID = "COMMENT";
	public static final List<String> PROPERTY_IDS = Arrays.asList(new String[] { NAME_PROPERTY_ID, CODE_PROPERTY_ID, BUDGET_PROPERTY_ID, 
			INITIAL_PROPERTY_ID, CONSUMMED_PROPERTY_ID, ETC_PROPERTY_ID, DELTA_PROPERTY_ID, COMMENT_PROPERTY_ID } );

	public abstract ILogic<?> createCellLogic(AbstractLogicImpl<?> parentLogic, ILogicContext context, String filter, TaskSums task,
			String propertyId, boolean readonly);

}