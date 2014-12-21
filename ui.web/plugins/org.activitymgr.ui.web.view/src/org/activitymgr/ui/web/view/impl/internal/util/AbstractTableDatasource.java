package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Collection;

import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

@SuppressWarnings("serial")
public abstract class AbstractTableDatasource<ITEMID_TYPE, CELLPROVIDER_TYPE extends ITableCellProviderCallback<ITEMID_TYPE>>
		implements Container {

	private IResourceCache resourceCache;

	private CELLPROVIDER_TYPE cellProvider;

	protected AbstractTableDatasource(IResourceCache resourceCache,
			CELLPROVIDER_TYPE cellProvider) {
		this.resourceCache = resourceCache;
		this.cellProvider = cellProvider;
	}

	protected final IResourceCache getResourceCache() {
		return resourceCache;
	}

	protected CELLPROVIDER_TYPE getCellProvider() {
		return cellProvider;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsId(Object itemId) {
		return getCellProvider().contains((ITEMID_TYPE) itemId);
	}


	@Override
	public final Collection<ITEMID_TYPE> getItemIds() {
		return getCellProvider().getRootElements();
	}

	@Override
	public final int size() {
		return getItemIds().size();
	}

	/*
	 * Property management methods
	 */

	@Override
	public final Collection<String> getContainerPropertyIds() {
		return cellProvider.getPropertyIds();
	}

	@SuppressWarnings("unchecked")
	@Override
	public final Property<?> getContainerProperty(Object itemId,
			Object propertyId) {
		return (Property<?>) cellProvider.getCell((ITEMID_TYPE) itemId, (String) propertyId);
	}

	/*
	 * Unsupported operations
	 */

	@Override
	public final Class<?> getType(Object propertyId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item getItem(Object itemId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean removeItem(Object itemId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Item addItem(Object itemId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Object addItem() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean removeAllItems() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean addContainerProperty(Object propertyId, Class<?> type,
			Object defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean removeContainerProperty(Object propertyId) {
		throw new UnsupportedOperationException();
	}

}