package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

@SuppressWarnings("serial")
public abstract class AbstractContainerDatasource<ITEM_TYPE extends Item> implements Container {

	private IResourceCache resourceCache;
	private Map<String, ITEM_TYPE> items = new HashMap<String, ITEM_TYPE>();

	protected AbstractContainerDatasource(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
	}

	protected final IResourceCache getResourceCache() {
		return resourceCache;
	}
	
	@Override
	public final Item getItem(Object itemId) {
		String castedItemId = (String) itemId;
		ITEM_TYPE result = items.get(itemId);
		if (result == null) {
			result = createItem(castedItemId);
			items.put(castedItemId, result);
		}
		return result;
	}

	protected abstract ITEM_TYPE createItem(String itemId);

	@Override
	public final boolean removeItem(Object itemId) {
		items.remove(itemId);
		return true;
	}

	@Override
	public final boolean containsId(Object itemId) {
		return items.containsKey(itemId);
	}

	/*
	 * Property management methods
	 */

	@Override
	public final Property<?> getContainerProperty(Object itemId, Object propertyId) {
		return getItem(itemId).getItemProperty(propertyId);
	}

	/*
	 * Unsupported operations
	 */

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