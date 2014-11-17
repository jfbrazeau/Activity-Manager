package org.activitymgr.core.orm;

import java.util.HashMap;
import java.util.Properties;

import org.activitymgr.core.orm.impl.AnnotationBasedMappingConfiguration;
import org.activitymgr.core.orm.impl.DAOImpl;
import org.activitymgr.core.orm.impl.IMappgingConfiguration;
import org.activitymgr.core.orm.impl.PropertiesBasedMappingConfiguration;

/**
 * Mapping de classe.
 * @author jbrazeau
 */
public class DAOFactory {
	
	/** Logger */
	//private static Logger log = Logger.getLogger(DbClassMapping.class);

	/** Liste des mappers */
	private HashMap<Class<?>, DAOImpl<?>> mappers = new HashMap<Class<?>, DAOImpl<?>>();
		
	/** Mapping configuration */
	private IMappgingConfiguration mappingConfiguration;
	
	/**
	 * Constructeur par d�faut.
	 * @param timeZone the time zone to use.
	 */
	public DAOFactory() {
		this.mappingConfiguration = new AnnotationBasedMappingConfiguration();
	}
	
	/**
	 * Constructeur par d�faut.
	 * @param props dictionnaire de propri�t�s de configuration.
	 */
	public DAOFactory(Properties props) {
		this.mappingConfiguration = new PropertiesBasedMappingConfiguration(props);
	}
	
	/**
	 * Retourne l'instance singleton de mappeur de la classe.
	 * @param theClass la classe mapp�e.
	 * @return l'instance singleton de mappeur de la classe.
	 */
	@SuppressWarnings("unchecked")
	public <TYPE> IDAO<TYPE> getDAO(Class<TYPE> theClass) {
		DAOImpl<TYPE> mapper = (DAOImpl<TYPE>) mappers.get(theClass);
		if (mapper==null) {
			synchronized (mappers) {
				mapper = (DAOImpl<TYPE>) mappers.get(theClass);
				if (mapper==null) {
					mapper = new DAOImpl<TYPE>(mappingConfiguration, theClass);
					mappers.put(theClass, mapper);
				}
			}
		}
		return mapper;
	}

}
