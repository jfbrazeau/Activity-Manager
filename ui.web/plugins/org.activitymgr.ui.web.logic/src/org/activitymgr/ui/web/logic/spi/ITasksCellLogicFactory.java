package org.activitymgr.ui.web.logic.spi;

import java.util.Arrays;
import java.util.List;

import org.activitymgr.core.dto.misc.TaskSums;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.TasksCellLogicFatory;

/**
 * @author jbrazeau
 * @see TasksCellLogicFatory
 */
public interface ITasksCellLogicFactory extends ICellLogicFactory {

	String NAME_PROPERTY_ID = "NAME";
	String CODE_PROPERTY_ID = "CODE";
	String BUDGET_PROPERTY_ID = "BUDGET";
	String INITIAL_PROPERTY_ID = "INITIAL";
	String CONSUMMED_PROPERTY_ID = "CONSUMMED";
	String ETC_PROPERTY_ID = "ETC";
	String DELTA_PROPERTY_ID = "DELTA";
	String COMMENT_PROPERTY_ID = "COMMENT";
	List<String> PROPERTY_IDS = Arrays.asList(new String[] { NAME_PROPERTY_ID, CODE_PROPERTY_ID, BUDGET_PROPERTY_ID, 
			INITIAL_PROPERTY_ID, CONSUMMED_PROPERTY_ID, ETC_PROPERTY_ID, DELTA_PROPERTY_ID, COMMENT_PROPERTY_ID } );

	ILogic<?> createCellLogic(AbstractLogicImpl<?> parentLogic, IUILogicContext context, String filter, TaskSums task,
			String propertyId, boolean readonly);

}