package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Collection;

import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

@SuppressWarnings("serial")
public class BasicListDatasource extends
		AbstractContainerDatasource<BasicItem> {

	private IListContentProviderCallback<Object> contentProvider;
	
	@SuppressWarnings("unchecked")
	public BasicListDatasource(IResourceCache resourceCache,
			IListContentProviderCallback<?> contentProvider) {
		super(resourceCache);
		this.contentProvider = (IListContentProviderCallback<Object>) contentProvider;
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
	public final Collection<?> getContainerPropertyIds() {
		return contentProvider.getPropertyIds();
	}

	@Override
	public final Class<?> getType(Object propertyId) {
		return String.class;
	}

	@Override
	public boolean containsId(Object itemId) {
		return contentProvider.contains(itemId);
	}


	@Override
	public final synchronized Collection<?> getItemIds() {
		return contentProvider.getRootElements();
	}

	@Override
	public final int size() {
		return getItemIds().size();
	}

}
