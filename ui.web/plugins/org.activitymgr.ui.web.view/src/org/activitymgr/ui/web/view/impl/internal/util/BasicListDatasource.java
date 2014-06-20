package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Arrays;
import java.util.Collection;

import org.activitymgr.ui.web.logic.ILabelProviderCallback;
import org.activitymgr.ui.web.logic.IListContentProviderCallback;
import org.activitymgr.ui.web.logic.ITreeContentProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.server.Resource;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class BasicListDatasource extends
		AbstractContainerDatasource<BasicItem> {

	private IListContentProviderCallback contentProvider;

	public BasicListDatasource(IResourceCache resourceCache,
			IListContentProviderCallback contentProvider) {
		super(resourceCache);
		this.contentProvider = contentProvider;
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
	public final Collection<?> getItemIds() {
		return contentProvider.rootItemIds();
	}

	@Override
	public final int size() {
		return getItemIds().size();
	}

}
