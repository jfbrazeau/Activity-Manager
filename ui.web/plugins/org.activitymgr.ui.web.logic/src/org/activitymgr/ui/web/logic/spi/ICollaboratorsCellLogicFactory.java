package org.activitymgr.ui.web.logic.spi;

import java.util.Arrays;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.IUILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;

/**
 * @author jbrazeau
 * @see CollaboratorsCellLogicFatory
 */
public interface ICollaboratorsCellLogicFactory extends ICellLogicFactory {

	String IS_ACTIVE_PROPERTY_NAME_ID = "IS_ACTIVE";
	String LOGIN_PROPERTY_ID = "LOGIN";
	String FIRST_PROPERTY_NAME_ID = "FIRST_NAME";
	String LAST_PROPERTY_NAME_ID = "LAST_NAME";
	List<String> PROPERTY_IDS = Arrays.asList(new String[] {
			IS_ACTIVE_PROPERTY_NAME_ID, LOGIN_PROPERTY_ID,
			FIRST_PROPERTY_NAME_ID, LAST_PROPERTY_NAME_ID });

	ILogic<?> createCellLogic(AbstractLogicImpl<?> parentLogic, IUILogicContext context, Collaborator collaborator,
			String propertyId, boolean readonly);

}