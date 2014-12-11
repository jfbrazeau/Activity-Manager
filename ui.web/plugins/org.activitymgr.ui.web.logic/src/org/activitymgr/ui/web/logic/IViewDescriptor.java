package org.activitymgr.ui.web.logic;

public interface IViewDescriptor {
	
	Object[] getConstructorArgs();

	Class<?>[] getConstructorArgTypes();

}