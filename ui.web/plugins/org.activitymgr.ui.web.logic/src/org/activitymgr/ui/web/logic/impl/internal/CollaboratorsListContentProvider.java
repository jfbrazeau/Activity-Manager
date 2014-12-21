package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeCheckBoxLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTextFieldLogicImpl;
import org.activitymgr.ui.web.logic.impl.LabelLogicImpl;
import org.activitymgr.ui.web.logic.impl.LogicContext;

class CollaboratorsListContentProvider extends AbstractSafeTableCellProviderCallback<Long> {

	protected static final String IS_ACTIVE_PROPERTY_NAME_ID = "IS_ACTIVE";
	protected static final String LOGIN_PROPERTY_ID = "LOGIN";
	protected static final String FIRST_PROPERTY_NAME_ID = "FIRST_NAME";
	protected static final String LAST_PROPERTY_NAME_ID = "LAST_NAME";
	protected static final List<String> PROPERTY_IDS = Arrays.asList(new String[] { IS_ACTIVE_PROPERTY_NAME_ID, LOGIN_PROPERTY_ID, FIRST_PROPERTY_NAME_ID, LAST_PROPERTY_NAME_ID } );

	private IModelMgr modelMgr;
	private boolean showInactiveCollaborators;
	private boolean readOnly;
	
	public CollaboratorsListContentProvider(ILogic<?> source, LogicContext context, IModelMgr modelMgr, boolean showInactiveCollaborators, boolean readOnly) {
		super(source, context.getEventBus());
		this.modelMgr = modelMgr;
		this.showInactiveCollaborators = showInactiveCollaborators;
		this.readOnly = readOnly;
	}

	@Override
	protected IView<?> unsafeGetCell(final Long collaboratorId, String propertyId) {
		Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
		AbstractLogicImpl<?> source = (AbstractLogicImpl<?>) getSource();
		ILogic<?> logic = null;
		if (IS_ACTIVE_PROPERTY_NAME_ID.equals(propertyId)) {
			if (isReadOnly()) {
				logic = new LabelLogicImpl(source, collaborator.getIsActive() ? "X" : "");
			} else {
				logic = new AbstractSafeCheckBoxLogicImpl(source, collaborator.getIsActive()) {
					@Override
					protected void unsafeOnValueChanged(boolean newValue)
							throws ModelException {
						Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
						if (newValue != collaborator.getIsActive()) {
							System.out.println("Update isActive " + collaborator.getLogin());
							collaborator.setIsActive(newValue);
							modelMgr.updateCollaborator(collaborator);
						}
					}
				};
			}
		}
		else if (LOGIN_PROPERTY_ID.equals(propertyId)) {
			if (isReadOnly()) {
				logic = new LabelLogicImpl(source, collaborator.getLogin());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(source, collaborator.getLogin(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
						System.out.println("Update login " + collaborator.getLogin());
						collaborator.setLogin(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		}
		else if (FIRST_PROPERTY_NAME_ID.equals(propertyId)) {
			if (isReadOnly()) {
				logic = new LabelLogicImpl(source, collaborator.getFirstName());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(source, collaborator.getFirstName(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
						collaborator.setFirstName(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		}
		else if (LAST_PROPERTY_NAME_ID.equals(propertyId)) {
			if (isReadOnly()) {
				logic = new LabelLogicImpl(source, collaborator.getLastName());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(source, collaborator.getLastName(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
						collaborator.setLastName(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
		return logic.getView();
	}

	@Override
	protected Collection<String> unsafeGetPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	protected final synchronized Collection<Long> unsafeGetRootElements() throws Exception {
		List<Long> collaboratorIds = new ArrayList<Long>();
		Collaborator[] collaborators = showInactiveCollaborators ? modelMgr
				.getCollaborators(Collaborator.FIRST_NAME_FIELD_IDX, true)
				: modelMgr.getActiveCollaborators(
						Collaborator.FIRST_NAME_FIELD_IDX, true);
		for (Collaborator collaborator : collaborators) {
			collaboratorIds.add(collaborator.getId());
		}
		return collaboratorIds;
	}

	@Override
	protected final boolean unsafeContains(Long collaboratorId) {
		Collaborator collaborator = modelMgr.getCollaborator(collaboratorId);
		return collaborator != null;
	}

	protected boolean isReadOnly() {
		return readOnly;
	}

}