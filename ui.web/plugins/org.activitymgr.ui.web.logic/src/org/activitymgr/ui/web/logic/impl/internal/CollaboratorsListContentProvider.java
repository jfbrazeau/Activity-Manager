package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.impl.AbstractSafeListContentProviderCallback;
import org.activitymgr.ui.web.logic.impl.LogicContext;

class CollaboratorsListContentProvider extends AbstractSafeListContentProviderCallback<Long> {

	protected static final String LOGIN_PROPERTY_ID = "LOGIN";
	protected static final String FIRST_PROPERTY_NAME_ID = "FIRST_NAME";
	protected static final String LAST_PROPERTY_NAME_ID = "LAST_NAME";
	protected static final List<String> PROPERTY_IDS = Arrays.asList(new String[] { LOGIN_PROPERTY_ID, FIRST_PROPERTY_NAME_ID, LAST_PROPERTY_NAME_ID } );

	private IModelMgr modelMgr;
	
	public CollaboratorsListContentProvider(ILogic<?> source, LogicContext context, IModelMgr modelMgr) {
		super(source, context.getEventBus());
		this.modelMgr = modelMgr;
	}

	@Override
	public String unsafeGetText(Long collaboratorId, String propertyId)
			throws Exception {
		Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
		if (LOGIN_PROPERTY_ID.equals(propertyId)) {
			return collaborator.getLogin();
		}
		else if (FIRST_PROPERTY_NAME_ID.equals(propertyId)) {
			return collaborator.getFirstName();
		}
		else if (LAST_PROPERTY_NAME_ID.equals(propertyId)) {
			return collaborator.getLastName();
		}
		else {
			return "";
		}
	}
	
	@Override
	public Collection<String> getPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	protected synchronized Collection<Long> unsafeGetRootElements() throws Exception {
		List<Long> collaboratorIds = new ArrayList<Long>();
		for (Collaborator collaborator : modelMgr.getActiveCollaborators(Collaborator.FIRST_NAME_FIELD_IDX, true)) {
			collaboratorIds.add(collaborator.getId());
		}
		return collaboratorIds;
	}

	@Override
	protected boolean unsafeContains(Long collaboratorId) {
		Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
		return collaborator != null && collaborator.getIsActive();
	}

}