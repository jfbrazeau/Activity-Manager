package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;

@SuppressWarnings("serial")
public class TreeTableDatasource<ITEMID_TYPE> extends
		AbstractTableDatasource<ITEMID_TYPE, ITreeContentProviderCallback<ITEMID_TYPE>> implements
		Container.Hierarchical {

	public TreeTableDatasource(IResourceCache resourceCache,
			ITreeContentProviderCallback<ITEMID_TYPE> cellProvider) {
		super(resourceCache, cellProvider);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<?> getChildren(Object element) {
		return getCellProvider().getChildren((ITEMID_TYPE) element);
	}

	@Override
	public Collection<?> rootItemIds() {
		return getItemIds();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isRoot(Object element) {
		return getCellProvider().isRoot((ITEMID_TYPE) element);
	}

	/*
	 * Property management methods
	 */

	@Override
	public final boolean areChildrenAllowed(Object element) {
		return hasChildren(element);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final boolean hasChildren(Object element) {
		return getCellProvider().hasChildren((ITEMID_TYPE) element);
	}

	/*
	 * Not implemented
	 */

	@SuppressWarnings("unchecked")
	@Override
	public final Object getParent(Object element) {
		return getCellProvider().getParent((ITEMID_TYPE) element);
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
