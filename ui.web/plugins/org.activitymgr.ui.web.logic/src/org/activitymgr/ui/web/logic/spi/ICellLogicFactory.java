package org.activitymgr.ui.web.logic.spi;

import java.util.Collection;

import org.activitymgr.ui.web.logic.Align;

public interface ICellLogicFactory {

	public abstract Collection<String> getPropertyIds();
	
	public abstract Integer getColumnWidth(String propertyId);

	public abstract Align getColumnAlign(String propertyId);

}
