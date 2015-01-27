package org.activitymgr.ui.web.logic.impl.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.ui.web.logic.ILogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.impl.AbstractLogicImpl;
import org.activitymgr.ui.web.logic.impl.AbstractSafeTableCellProviderCallback;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.LogicContext;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

class CollaboratorsListTableCellProvider extends AbstractSafeTableCellProviderCallback<Long> {

	private IModelMgr modelMgr;
	private boolean showInactiveCollaborators;
	private CollaboratorsCellLogicFatory cellLogicFactory;
	private List<Long> collaboratorIds = new ArrayList<Long>();
	private Map<Long, Collaborator> collaboratorsMap = new HashMap<Long, Collaborator>();
	private boolean readOnly;
	
	private LoadingCache<Long, LoadingCache<String, ILogic<?>>> cellLogics = CacheBuilder.newBuilder().build(new CacheLoader<Long, LoadingCache<String, ILogic<?>>>() {
		@Override
		public LoadingCache<String, ILogic<?>> load(final Long collaboratorId) throws Exception {
			return CacheBuilder.newBuilder().build(new CacheLoader<String, ILogic<?>>() {
				@Override
				public ILogic<?> load(String propertyId) throws Exception {
					Collaborator collaborator = collaboratorsMap.get(collaboratorId);
					return cellLogicFactory.createCellLogic(collaborator, propertyId, readOnly);
				}

			});
		}
	});
	
	public CollaboratorsListTableCellProvider(AbstractLogicImpl<?> source, LogicContext context, boolean showInactiveCollaborators, boolean readOnly) {
		super(source, context);
		this.modelMgr = context.getComponent(IModelMgr.class);
		this.showInactiveCollaborators = showInactiveCollaborators;
		this.readOnly = readOnly;
		this.cellLogicFactory = context.getSingletonExtension("org.activitymgr.ui.web.logic.collaboratorsCellLogicFactory", CollaboratorsCellLogicFatory.class, AbstractLogicImpl.class, source);
	}

	@Override
	protected IView<?> unsafeGetCell(final Long collaboratorId, String propertyId) {
		try {
			return cellLogics.get(collaboratorId).get(propertyId).getView();
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected Collection<String> unsafeGetPropertyIds() {
		return cellLogicFactory.getPropertyIds();
	}

	@Override
	protected final synchronized Collection<Long> unsafeGetRootElements() throws Exception {
		if (collaboratorIds.size() == 0) {
			Collaborator[] collaborators = showInactiveCollaborators ? modelMgr
					.getCollaborators(Collaborator.FIRST_NAME_FIELD_IDX, true)
					: modelMgr.getActiveCollaborators(
							Collaborator.FIRST_NAME_FIELD_IDX, true);
			for (Collaborator collaborator : collaborators) {
				long collaboratorId = collaborator.getId();
				collaboratorIds.add(collaboratorId);
				collaboratorsMap.put(collaboratorId, collaborator);
			}
			// Sort collaborators identifiers
			Collections.sort(collaboratorIds, new Comparator<Long>() {
				@Override
				public int compare(Long collaboratorId1, Long collaboratorId2) {
					Collaborator collaborator1 = collaboratorsMap.get(collaboratorId1);
					Collaborator collaborator2 = collaboratorsMap.get(collaboratorId2);
					return collaborator1.getFirstName().compareTo(collaborator2.getFirstName());
				}
			});
		}
		
		return collaboratorIds;
	}

	@Override
	protected final boolean unsafeContains(Long collaboratorId) {
		return collaboratorsMap.containsKey(collaboratorId);
	}

	protected boolean isReadOnly() {
		return readOnly;
	}

	@Override
	protected Integer unsafeGetColumnWidth(String propertyId) {
		return cellLogicFactory.getColumnWidth(propertyId);
	}

}