package org.activitymgr.ui.web.logic.spi;

import java.util.Collection;

import org.activitymgr.ui.web.logic.Align;

public interface ICellLogicFactory {

	Collection<String> getPropertyIds();
	
	Integer getColumnWidth(String propertyId);

	Align getColumnAlign(String propertyId);

}
