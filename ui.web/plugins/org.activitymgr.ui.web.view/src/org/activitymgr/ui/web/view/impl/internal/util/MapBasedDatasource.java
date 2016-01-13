package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class MapBasedDatasource<K> implements Container {
	
	public static final String LABEL_PROPERTY_ID = "LABEL";

	private static final Collection<String> PROPERTY_IDS = Arrays.asList(LABEL_PROPERTY_ID);
	
	private Map<K, String> map;
	
	public MapBasedDatasource(Map<K, String> map) {
		this.map = map;
	}

	@Override
	public Collection<String> getContainerPropertyIds() {
		return PROPERTY_IDS;
	}

	@Override
	public Collection<K> getItemIds() {
		return map.keySet();
	}

	@Override
	public Property<?> getContainerProperty(Object itemId, Object propertyId) {
		if (LABEL_PROPERTY_ID.equals(propertyId)) {
			return new Label(map.get(itemId));
		}
		else {
			throw new IllegalArgumentException("Unexpected property '" + propertyId + "'");
		}
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean containsId(Object itemId) {
		return map.containsKey(itemId);
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
