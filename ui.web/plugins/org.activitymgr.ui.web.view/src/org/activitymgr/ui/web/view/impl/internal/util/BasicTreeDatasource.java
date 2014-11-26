package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;

@SuppressWarnings("serial")
public class BasicTreeDatasource extends
		BasicListDatasource implements
		Container.Hierarchical {

	private ITreeContentProviderCallback<Object> contentProvider;

	@SuppressWarnings("unchecked")
	public BasicTreeDatasource(IResourceCache resourceCache,
			ITreeContentProviderCallback<?> contentProvider) {
		super(resourceCache, contentProvider);
		this.contentProvider = (ITreeContentProviderCallback<Object>) contentProvider;
	}

	@Override
	public Collection<?> getChildren(Object element) {
		System.out.println("getChildren(" + element + ")");
		return contentProvider.getChildren(element);
	}

	@Override
	public Collection<?> rootItemIds() {
		return contentProvider.getRootElements();
	}

	@Override
	public boolean isRoot(Object element) {
		return contentProvider.isRoot(element);
	}

	@Override
	protected BasicItem createItem(Object value) {
		return new BasicItem(getResourceCache(), value,
				contentProvider);
	}

	/*
	 * Property management methods
	 */

	@Override
	public final boolean areChildrenAllowed(Object element) {
		return hasChildren(element);
	}

	@Override
	public final boolean hasChildren(Object element) {
		return contentProvider.hasChildren(element);
	}

	/*
	 * Not implemented
	 */

	@Override
	public final Object getParent(Object element) {
		System.out.println("getParent(" + element + ")");
		return contentProvider.getParent(element);
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
