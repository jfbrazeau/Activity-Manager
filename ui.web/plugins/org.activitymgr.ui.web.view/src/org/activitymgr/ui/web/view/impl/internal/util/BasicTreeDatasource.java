package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;
import com.vaadin.server.Resource;

@SuppressWarnings("serial")
public class BasicTreeDatasource extends
		AbstractContainerDatasource<BasicItem> implements
		Container.Hierarchical {

	private ITreeContentProviderCallback contentProvider;

	public BasicTreeDatasource(IResourceCache resourceCache,
			ITreeContentProviderCallback contentProvider) {
		super(resourceCache);
		this.contentProvider = contentProvider;
	}

	@Override
	public Collection<?> getChildren(Object itemId) {
		return contentProvider.getChildren((String) itemId);
	}

	@Override
	public Collection<?> rootItemIds() {
		return contentProvider.rootItemIds();
	}

	@Override
	public boolean isRoot(Object itemId) {
		return contentProvider.isRoot((String) itemId);
	}

	@Override
	protected BasicItem createItem(String itemId) {
		return new BasicItem(getResourceCache(),
				contentProvider.getLabelProvider(itemId));
	}

	/*
	 * Property management methods
	 */

	@Override
	public final Collection<?> getContainerPropertyIds() {
		return BasicItem.PROPERTY_IDS;
	}

	@Override
	public final Class<?> getType(Object propertyId) {
		return BasicItem.NAME_PROPERTY_ID.equals(propertyId) ? String.class
				: Resource.class;
	}

	@Override
	public final boolean areChildrenAllowed(Object itemId) {
		return hasChildren(itemId);
	}

	@Override
	public final boolean hasChildren(Object itemId) {
		return getChildren(itemId).size() > 0;
	}

	/*
	 * Not implemented
	 */

	@Override
	public final Object getParent(Object itemId) {
		throw new IllegalStateException(
				"Method is not implemented {getParent(Object itemId)}");
	}

	@Override
	public final Collection<?> getItemIds() {
		throw new IllegalStateException(
				"Method is not implemented {getItemIds()}");
	}

	@Override
	public final int size() {
		throw new IllegalStateException("Method is not implemented {size()}");
	}

	/*
	 * Unsupported operations
	 */

	@Override
	public final boolean setParent(Object itemId, Object newParentId)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean setChildrenAllowed(Object itemId,
			boolean areChildrenAllowed) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
