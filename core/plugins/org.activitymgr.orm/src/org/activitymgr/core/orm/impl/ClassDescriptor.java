package org.activitymgr.core.orm.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.activitymgr.core.orm.ClassDescriptorException;
import org.apache.log4j.Logger;

/**
 * Classe permettant d'acc�der aux attributs d'une classe de mani�re dynamique.
 * <p>Un attribut est d�fini par 2 accesseurs, un en lecture, un en �criture.</p>
 * @author jbrazeau
 */
public class ClassDescriptor<TYPE> {

	/** Logger */
	private static Logger log = Logger.getLogger(ClassDescriptor.class);

	/** Pr�fixe des 'getters' */
	public static final String PRFX_GETTER = "get";

	/** Pr�fixe des 'setters' */
	public static final String PRFX_SETTER = "set";

	/** Liste des descripteurs */
	private static HashMap<Class<?>, ClassDescriptor<?>> descriptors = new HashMap<Class<?>, ClassDescriptor<?>>();

	/**
	 * Retourne l'instance singleton de descripteur de la classe.
	 * @param theClass la classe d�crite.
	 * @return l'instance singletons de descripteur de la classe.
	 */
	@SuppressWarnings("unchecked")
	public static <TYPE> ClassDescriptor<TYPE> getDescriptor(Class<TYPE> theClass) {
		ClassDescriptor<TYPE> descriptor = (ClassDescriptor<TYPE>) descriptors.get(theClass);
		if (descriptor==null) {
			synchronized (descriptors) {
				descriptor = (ClassDescriptor<TYPE>) descriptors.get(theClass);
				if (descriptor==null) {
					descriptor = new ClassDescriptor<TYPE>(theClass);
					descriptors.put(theClass, descriptor);
				}
			}
		}
		return descriptor;
	}

	/** La classe d�crite */
	private Class<TYPE> theClass;
	
	/** Liste des noms d'attributs */
	private String[] attributeNames;
	
	/** Dictionnaire d'accesseurs en lecture */
	private HashMap<String, Method> getters = new HashMap<String, Method>();
	
	/** Dictionnaire d'accesseurs en �criture */
	private HashMap<String, Method> setters = new HashMap<String, Method>();
	
	/**
	 * Constructeur priv�.
	 * @param theClass la classe d�crite.
	 */
	private ClassDescriptor(Class<TYPE> theClass) {
		this.theClass = theClass;
		if (log.isDebugEnabled())
			log.debug("Describing class : " + theClass);
		// On r�cup�re les m�thodes de la classe
		Method[] methods = theClass.getMethods();
		int nbMethods = methods.length;
		if (log.isDebugEnabled())
			log.debug("Nb Methods : " + nbMethods);
		List<String> attributeNames = new ArrayList<String>();
		for (int i=0; i<nbMethods; i++) {
			Method method = methods[i];
			if (isGetter(method)) {
				String attributeName = method.getName().substring(PRFX_GETTER.length());
				if (log.isDebugEnabled())
					log.debug("Getter found for attribute '" + attributeName + "'");
				Method getter = method;
				try {
					String setterName = PRFX_SETTER + attributeName;
					Method setter = method.getDeclaringClass().getMethod(setterName, new Class[] { method.getReturnType()});
					// Dans le cas ou la m�thode existe, on v�rifie qu'elle est publique
					if (Modifier.isPublic(setter.getModifiers())
							&& setter.getReturnType().equals(void.class)) {
						if (log.isDebugEnabled())
							log.debug("Setter found for attribute '" + attributeName + "'");
						getters.put(attributeName, getter);
						setters.put(attributeName, setter);
						attributeNames.add(attributeName);
					}
				}
				catch (NoSuchMethodException ignored) {
					// Si une exception est lev�e, cela signifie que le getter 
					// n'a pas de getter correspondant
				}
			}
		}
		// Sauvegarde des noms d'attributs
		this.attributeNames = (String[]) attributeNames.toArray(new String[attributeNames.size()]);
		if (log.isDebugEnabled())
			log.debug("Attribute names count '" + this.attributeNames.length + "'");
	}
	
	/**
	 * Retourne la liste des noms d'attributs.
	 * @return la liste des noms d'attributs.
	 */
	public String[] getAttributeNames() {
		return attributeNames;
	}

	/**
	 * Cr�e une nouvelle instance de la classe d�crite.
	 * @return la nouvelle instance.
	 * @throws ClassDescriptorException lev� en cas d'incident lors de
	 * 		l'instanciation.
	 */
	public TYPE newInstance() throws ClassDescriptorException {
		try {
			return theClass.newInstance();
		}
		catch (InstantiationException e) {
			log.error("Incident lors de l'instanciation de la classe '" + theClass.getName() + "'", e);
			throw new ClassDescriptorException("Incident lors de l'instanciation de la classe '" + theClass.getName() + "'", e);
		}
		catch (IllegalAccessException e) {
			log.error("Incident lors de l'instanciation de la classe '" + theClass.getName() + "'", e);
			throw new ClassDescriptorException("Incident lors de l'instanciation de la classe '" + theClass.getName() + "'", e);
		}
	}
	
	/**
	 * Retourne le type de l'attribut sp�cifi�.
	 * @param attributeName le nom de l'attribut.
	 * @return le type de l'attribut sp�cifi�.
	 * @throws ClassDescriptorException lev� dans le cas ou l'attribut dont le
	 * 		nom est sp�cifi� n'existe pas.
	 */
	public Class<?> getAttributeType(String attributeName) throws ClassDescriptorException {
		// R�cup�ration du getter
		Method getter = (Method) getters.get(attributeName);
		if (getter==null)
			throw new ClassDescriptorException("Attribute '" + attributeName + "' does not exist", null);
		return getter.getReturnType();
	}
	
	/**
	 * Retourne la valeur de l'attribut pour une instance donn�e.
	 * @param instance l'instance sur laquelle est effectu�e la lecture.
	 * @param attributeName le nom de l'attribut.
	 * @return la valeur de l'attribut.
	 * @throws ClassDescriptorException lev� en cas d'incident lors de
	 * 		l'invocation de l'accesseur.
	 */
	public Object getInstanceAttributeValue(
			TYPE instance,
			String attributeName) throws ClassDescriptorException {
		try {
			// R�cup�ration du getter
			Method getter = (Method) getters.get(attributeName);
			if (getter==null)
				throw new ClassDescriptorException("Attribute '" + attributeName + "' does not exist", null);
			// On tente de retourner la valeur
			return getter.invoke(instance, (Object[]) null);
		}
		// Cas m�thode innaccessible
		catch (IllegalAccessException e) {
			log.error("L'attribut '" + attributeName + "' n'est pas accessible (private? protected?)!", e);
			throw new ClassDescriptorException("L'attribut '" + attributeName + "' n'est pas accessible (private? protected?)!", e);
		}
		// Cas d'une erreur survenue dans la m�thode elle-m�me
		catch (InvocationTargetException e) {
			log.error("Erreur lors de la lecture de l'attribut '" + attributeName + "'", e.getTargetException());
			throw new ClassDescriptorException("Erreur lors de la lecture de l'attribut '" + attributeName + "'", e.getTargetException());
		}
	}

	/**
	 * D�finit la nouvelle valeur de l'attribut pour une instance donn�e.
	 * @param instance l'instance sur laquelle est effectu�e l'�criture.
	 * @param attributeName le nom de l'attribut.
	 * @param value la nouvelle valeur de l'attribut.
	 * @throws ClassDescriptorException lev� en cas d'incident lors de
	 * 		l'invocation de l'accesseur.
	 */
	public void setInstanceAttributeValue(
			TYPE instance,
			String attributeName,
			Object value) throws ClassDescriptorException {
		// On tente d'invoquer le 'setter' en lui passant le 'parameter' en param�tre
		try {
			// R�cup�ration du setter
			Method setter = (Method) setters.get(attributeName);
			if (setter==null)
				throw new ClassDescriptorException("Attribute '" + attributeName + "' does not exist", null);
			// Invocation
			setter.invoke(instance, new Object[] { value });
		}
		// Cas m�thode innaccessible
		catch (IllegalAccessException e) {
			log.error("L'attribut '" + attributeName + "' n'est pas accessible (private? protected?)!", e);
			throw new ClassDescriptorException("L'attribut '" + attributeName + "' n'est pas accessible (private? protected?)!", e);
		}
		// Cas d'une erreur survenue dans la m�thode elle-m�me
		catch (InvocationTargetException e) {
			log.error("Erreur lors du positionnement de l'attribut '" + attributeName + "'", e.getTargetException());
			throw new ClassDescriptorException("Erreur lors du positionnement de l'attribut '" + attributeName + "'", e.getTargetException());
		}
	}

	/**
	 * Retourne la classe d�crite.
	 * @return la classe d�crite.
	 */
	public Class<TYPE> getDescribedClass() {
		return theClass;
	}
	
	/**
	 * M�thode v�rifiant si une m�thode est un 'getter'.
	 * @param method la m�thode � analyser
	 * @return un bool�en indiquant si la m�thode est un 'getter'.
	 */
	private static boolean isGetter(Method method) {
		// Si la m�thode est publique
		if (Modifier.isPublic(method.getModifiers())) {
			// Si la m�thode n'appartient pas � la classe 'Object' (je pense � 'getClass' par ex.) 
			if (!Object.class.equals(method.getDeclaringClass())) {
				// Si la m�thode ne prend aucun argument
				if (method.getParameterTypes().length == 0) {
					// Si le nom de la m�thode commence par "get"
					if (method.getName().startsWith(PRFX_GETTER)) {
						// Quelques conversions
						char[] methodNameChars = method.getName().toCharArray();
						// Si la longueur du nom est sup�rieure � celle du pr�fixe
						if (methodNameChars.length > PRFX_GETTER.length()) {
							char firstCar = methodNameChars[PRFX_GETTER.length()];
							// Si le caract�re suivant le pr�fixe est bien une majuscule
							if ((firstCar > 64) && (firstCar < 91)) {
								return true;
							}
						}
					}
				}
			}
		}
		// Sinon on retroune 'false'
		return false;
	}

}
