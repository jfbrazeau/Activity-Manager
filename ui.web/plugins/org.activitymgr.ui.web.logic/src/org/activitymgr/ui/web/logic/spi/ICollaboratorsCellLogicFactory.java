package org.activitymgr.ui.web.logic.spi;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;

/**
 * @author jbrazeau
 * @see CollaboratorsCellLogicFatory
 */
public interface ICollaboratorsCellLogicFactory {

	public static final String IS_ACTIVE_PROPERTY_NAME_ID = "IS_ACTIVE";
	public static final String LOGIN_PROPERTY_ID = "LOGIN";
	public static final String FIRST_PROPERTY_NAME_ID = "FIRST_NAME";
	public static final String LAST_PROPERTY_NAME_ID = "LAST_NAME";
	public static final List<String> PROPERTY_IDS = Arrays.asList(new String[] {
			IS_ACTIVE_PROPERTY_NAME_ID, LOGIN_PROPERTY_ID,
			FIRST_PROPERTY_NAME_ID, LAST_PROPERTY_NAME_ID });

	public abstract ILogic<?> createCellLogic(AbstractLogicImpl<?> parentLogic, ILogicContext context, Collaborator collaborator,
			String propertyId, boolean readonly);

	public abstract Collection<String> getPropertyIds();

	public abstract Integer getColumnWidth(String propertyId);

}