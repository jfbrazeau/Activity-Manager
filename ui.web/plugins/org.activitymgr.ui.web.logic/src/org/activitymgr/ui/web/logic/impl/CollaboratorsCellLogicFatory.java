package org.activitymgr.ui.web.logic.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.ILogic;

import com.google.inject.Inject;

public class CollaboratorsCellLogicFatory {

	public static final String IS_ACTIVE_PROPERTY_NAME_ID = "IS_ACTIVE";
	public static final String LOGIN_PROPERTY_ID = "LOGIN";
	public static final String FIRST_PROPERTY_NAME_ID = "FIRST_NAME";
	public static final String LAST_PROPERTY_NAME_ID = "LAST_NAME";
	
	public static final List<String> PROPERTY_IDS = Arrays.asList(new String[] {
			IS_ACTIVE_PROPERTY_NAME_ID, LOGIN_PROPERTY_ID,
			FIRST_PROPERTY_NAME_ID, LAST_PROPERTY_NAME_ID });

	@Inject
	private IModelMgr modelMgr;

	private AbstractLogicImpl<?> parentLogic;

	@Inject
	private ILogicContext context;

	public CollaboratorsCellLogicFatory(AbstractLogicImpl<?> parentLogic) {
		this.parentLogic = parentLogic;
		parentLogic.injectMembers(this);
	}

	public ILogic<?> createCellLogic(final Collaborator collaborator, String propertyId, boolean readonly) {
		ILogic<?> logic = null;
		if (IS_ACTIVE_PROPERTY_NAME_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getIsActive() ? "X" : "");
			} else {
				logic = new AbstractSafeCheckBoxLogicImpl(parentLogic, collaborator.getIsActive()) {
					@Override
					protected void unsafeOnValueChanged(boolean newValue)
							throws ModelException {
						if (newValue != collaborator.getIsActive()) {
							collaborator.setIsActive(newValue);
							modelMgr.updateCollaborator(collaborator);
						}
					}
				};
			}
		}
		else if (LOGIN_PROPERTY_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getLogin());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(parentLogic, collaborator.getLogin(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						collaborator.setLogin(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		}
		else if (FIRST_PROPERTY_NAME_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getFirstName());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(parentLogic, collaborator.getFirstName(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						collaborator.setFirstName(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		}
		else if (LAST_PROPERTY_NAME_ID.equals(propertyId)) {
			if (readonly) {
				logic = new LabelLogicImpl(parentLogic, collaborator.getLastName());
			} else {
				logic = new AbstractSafeTextFieldLogicImpl(parentLogic, collaborator.getLastName(), true) {
					@Override
					protected void unsafeOnValueChanged(String newValue)
							throws ModelException {
						collaborator.setLastName(newValue);
						modelMgr.updateCollaborator(collaborator);
					}
				};
			}
		}
		else {
			throw new IllegalArgumentException(propertyId);
		}
		return logic;
	}
	
	public Collection<String> getPropertyIds() {
		return PROPERTY_IDS;
	}

	protected ILogicContext getContext() {
		return context;
	}

	public Integer getColumnWidth(String propertyId) {
		if (IS_ACTIVE_PROPERTY_NAME_ID.equals(propertyId)) {
			return 30;
		}
		else if (LOGIN_PROPERTY_ID.equals(propertyId)) {
			return 150;
		}
		else if (FIRST_PROPERTY_NAME_ID.equals(propertyId)) {
			return 200;
		}
		else if (LAST_PROPERTY_NAME_ID.equals(propertyId)) {
			return 200;
		}
		else {
			return null;
		}
	}

	protected AbstractLogicImpl<?> getParentLogic() {
		return parentLogic;
	}

}
