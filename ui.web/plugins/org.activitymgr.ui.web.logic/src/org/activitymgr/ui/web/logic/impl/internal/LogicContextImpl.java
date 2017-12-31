package org.activitymgr.ui.web.logic.impl.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.ui.web.logic.IEventBus;
import org.activitymgr.ui.web.logic.ILogicContext;
import org.eclipse.core.runtime.IConfigurationElement;

import com.google.inject.Inject;

// TODO Inject ?
public class LogicContextImpl implements ILogicContext {

	private Collaborator connectedCollaborator;

	@Inject
	private IEventBus eventBus;
	
	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ILogicContext#getConnectedCollaborator()
	 */
	@Override
	public Collaborator getConnectedCollaborator() {
		return connectedCollaborator;
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ILogicContext#setConnectedCollaborator(org.activitymgr.core.dto.Collaborator)
	 */
	@Override
	public void setConnectedCollaborator(Collaborator connectedCollaborator) {
		this.connectedCollaborator = connectedCollaborator;
	}
	
	@Override
	public IEventBus getEventBus() {
		return eventBus;
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ILogicContext#getSingletonExtension(java.lang.String, java.lang.Class, java.lang.Class, CONSTRUCTOR_ARG_TYPE)
	 */
	@Override
	public <EXTENSION_TYPE, CONSTRUCTOR_ARG_TYPE> EXTENSION_TYPE getSingletonExtension(
			String extensionPointId, Class<EXTENSION_TYPE> defaultType,
			Class<CONSTRUCTOR_ARG_TYPE> constructorArgType,
			CONSTRUCTOR_ARG_TYPE constructorArg) {
		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor(extensionPointId);
		Class<EXTENSION_TYPE> type = defaultType;
		if (cfgs.length > 0) {
			if (cfgs.length > 1) {
				System.err.println(
						"More than one '" + extensionPointId + "' is provided.\n" +
						"Only the first occurence will be used");
			}
			IConfigurationElement cfg = cfgs[0];
			try {
				type = Activator.getDefault().<EXTENSION_TYPE>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
				
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		return newExtensionInstance(type, constructorArgType, constructorArg);
	}

//	public <EXTENSION_TYPE, CONSTRUCTOR_ARG_TYPE> List<EXTENSION_TYPE> getExtensions(
//			String extensionPointId, Class<CONSTRUCTOR_ARG_TYPE> constructorArgType,
//			CONSTRUCTOR_ARG_TYPE constructorArg) {
//		List<EXTENSION_TYPE> result = new ArrayList<EXTENSION_TYPE>();
//		IConfigurationElement[] cfgs = Activator.getDefault().getExtensionRegistryService().getConfigurationElementsFor(extensionPointId);
//		for (IConfigurationElement cfg : cfgs) {
//			try {
//				Class<EXTENSION_TYPE> type = Activator.getDefault().<EXTENSION_TYPE>loadClass(cfg.getContributor().getName(), cfg.getAttribute("class"));
//				result.add(newExtensionInstance(type, constructorArgType, constructorArg));
//				
//			} catch (ClassNotFoundException e) {
//				throw new IllegalStateException(e);
//			}
//		}
//		return result;
//	}
//
	/* (non-Javadoc)
	 * @see org.activitymgr.ui.web.logic.impl.ILogicContext#newExtensionInstance(java.lang.Class, java.lang.Class, CONSTRUCTOR_ARG_TYPE)
	 */
	@Override
	public <EXTENSION_TYPE, CONSTRUCTOR_ARG_TYPE> EXTENSION_TYPE newExtensionInstance(Class<EXTENSION_TYPE> type,
			Class<CONSTRUCTOR_ARG_TYPE> constructorArgType,
			CONSTRUCTOR_ARG_TYPE constructorArg) {
		try {
			// The type is supposed whether to have :
			// * a declared constructor accepting an abstract logic as parent
			// * or a declared constructor with two parameters : parent logic / logic context
			try {
				// First attempt
				Constructor<EXTENSION_TYPE> constructor = type.getDeclaredConstructor(constructorArgType, ILogicContext.class);
				return constructor.newInstance(constructorArg, this);
			}
			catch (NoSuchMethodException e) {
				// Second attempt
				Constructor<EXTENSION_TYPE> constructor = type.getDeclaredConstructor(constructorArgType);
				return constructor.newInstance(constructorArg);
			}
			
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

}