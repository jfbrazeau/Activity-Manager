/*
 * Copyright (c) 2004-2006, Jean-François Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jfb.tools.activitymgr.report;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jfb.tools.activitymgr.core.util.Strings;

import org.apache.log4j.Logger;

/**
 * Classe offrant des services d'accès aux propriétés de configuration
 * pour la génération de rapports avec Velocity.
 * 
 * <p>
 * Pour Velocity, une variable de type <code>${a.b}</code> peut vouloir
 * dire au choix :
 * <ul>
 * 	<li>l'attibut b de l'instance a (ou a est un bean Java)</li>
 *  <li>la propriété b de l'instance a (ou a est une Map)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * On exploite la deuxième possibilité pour rendre '<i>visible</i>' une
 * propriété définie dans le fichier de configuration des rapports dans
 * un template velocity. Une instance de <code>PropertiesHelper</code> 
 * est enregistrée dans le contexte velocity sous le nom <code>props</code>.
 * Comme cette classe implémente l'interface <code>java.util.Map</code>,
 * Velocity invoque la méthode <code>get(String)</code> en lui passant en 
 * argument la chaine aussitot placée après <code>props.</code>.
 * La fin de la propriété est marquée par l'invocation de la méthode
 * <code>get()</code>.
 * </p>
 * 
 * <p>
 * Il est supposé que les propriétés contenues dans le fichier de configuration
 * et accédées depuis les templates Velocity sont préfixées par :<br>
 * <code>reports.&lt;reportId&gt;</code> ou <code>reportId</code> désigne
 * l'identifiant du rapport.
 * </p>
 * 
 * <p>
 * Exemple : <code>${props.startYear.get()}</code> pour le rapport dont 
 * l'identifiant est <code>myReport</code> va référencer la propriété 
 * <code>reports.myReport.startYear</code> dans le fichier de configuration.
 * Si cette propriété n'est pas définie, le moteur regarde ensuite
 * tour à tour les propriétés <code>reports.startYear</code> et 
 * <code>startYear</code>. Si toutes ces propriétés sont indéfinies, une 
 * exception est levée.
 * </p>
 * 
 * <p>
 * Dans le cas ou le nom de la propriété est composée 
 * (ex : <code>start.year</code>), la méthode <code>get</code> retourne 
 * l'instance de <code>PropertiesHelper</code> et la première partie du nom de
 * la propriété est déposée sur la pile. Lors de l'invocation suivante, les
 * différentes parties du nom de la propriété sont réassemblées pour former
 * le nom de la propriété dans son ensemble.
 * </p>
 * 
 * <p>Pour cette raison, cette classe n'est pas thread safe.</p>
 * 
 * @see jfb.tools.activitymgr.report.ReportMgr
 */
public class PropertiesHelper implements Map {
	
	/** Logger */
	private static Logger log = Logger.getLogger(PropertiesHelper.class);

	/** Dictionnaire de propriétés */
	private Properties props;

	/** Identifiant courant de rapport */
	private String currentReportId;
	
	/** Pile utilisée pour extraire les propriétés */
	private Stack stack = new Stack();
	
	/**
	 * Constructeur par défaut.
	 * @param reportId identifiant du rapport courant.
	 * @param props dictionnaire par défaut.
	 */
	public PropertiesHelper(String reportId, Properties props) {
		this.currentReportId = reportId;
		this.props = props;
	}

	/**
	 * Retourne la valeur de la propriété dont la clé est constitué
	 * des chaînes empilées dans la pile.
	 * @return la valeur de la propriété.
	 * @throws ReportException levé dans le cas où la clé n'existe pas.
	 */
	public String get() throws ReportException {
		// Construction de la fin de la clé
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<stack.size(); i++) {
			if (i>0)
				buf.append('.');
			buf.append(stack.get(i));
		}
		log.debug("get(" + buf.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Purge de la pile
		stack.clear();
		// Lecture de la 1° clé par défaut
		String propKey0 = new StringBuffer("reports.") //$NON-NLS-1$
			.append(currentReportId)
			.append('.')
			.append(buf.toString())
			.toString();
		log.debug("PropertyKey=" + propKey0); //$NON-NLS-1$
		String propValue = props.getProperty(propKey0);
		// Lecture de la 2° clé par défaut si la 1° n'a rien donné
		if (propValue==null) {
			String propKey1 = new StringBuffer("reports.") //$NON-NLS-1$
				.append(buf.toString())
				.toString();
			log.debug("PropertyKey=" + propKey1); //$NON-NLS-1$
			propValue = props.getProperty(propKey1);
			// Lecture de la 3° clé par défaut si la 1° et la 2°
			// n'ont rien donné
			if (propValue==null) {
				String propKey2 = buf.toString();
				log.debug("PropertyKey=" + propKey2); //$NON-NLS-1$
				propValue = props.getProperty(propKey2);
				if (propValue==null)
					throw new ReportException(Strings.getString("PropertiesHelper.errors.PARAMETER_NOT_SPECIFIED", new Object[] { buf.toString(), propKey0, propKey1, propKey2 }), null); //$NON-NLS-1$
			}
		}
		// Retour du résultat
		String value = substitueVariables(propValue);
		log.debug("PropertyValue=" + value); //$NON-NLS-1$
		return value;
	}
	
	/**
	 * Retourne la valeur de la propriété dont le postfix est spécifié.
	 * @param postPropertyKey le postfix de clé de propriété.
	 * @return la valeur de la propriété.
	 * @throws ReportException levé en cas de non conformité de la valeur 
	 *    de la chaîne.
	 */
	public String getProperty(String postPropertyKey) throws ReportException {
		String propKey0 = new StringBuffer("reports.") //$NON-NLS-1$
			.append(currentReportId)
			.append('.')
			.append(postPropertyKey)
			.toString();
		log.debug("PropertyKey=" + propKey0); //$NON-NLS-1$
		String propValue = props.getProperty(propKey0);
		if (propValue!=null)
			propValue = substitueVariables(propValue);
		log.debug("PropertyValue=" + propValue); //$NON-NLS-1$
		return propValue;
	}
	
 	/**
	 * Substitue les chaînes de type ${xxx.xxx.xxx} par leur valeur
	 * trouvées dans les propriétés système.
	 * @param s la chaîne de caractère à parcourir.
	 * @return le résultat.
	 * @throws ReportException levé en cas de non conformité de la chaîne.
	 */
	private String substitueVariables(String s)
		throws ReportException {
		return substitueVariables(new Stack(), s);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		log.debug("Append '" + key + "' to property stack"); //$NON-NLS-1$ //$NON-NLS-2$
		stack.push(key);
		return this;
	}

	/**
	 * Remplace les occurences de variables de type <code>${xxx}</code> par
	 * leur valeur lorsqu'elle existe dans le dictionnaire.
	 * @param stack pile permettant de détecter les références circulaires.
	 * @param s la chaîne contenant les variables.
	 * @return la chaîne substituée.
	 * @throws ReportException levé en cas de détection de références 
	 *     circulaires.
	 */
	private String substitueVariables(
			Stack stack, 
			String s) throws ReportException {
		String result = null;
		if (s!=null) {
			// Préparation de l'expression régulière
			Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9.'_-]+\\}"); //$NON-NLS-1$
			Matcher matcher = pattern.matcher(s);
			// Préparation du buffer accueillant la chaine substituée
			StringBuffer buf = new StringBuffer();
			int idx = 0;
			// Parcours des valeurs obtenues
			while (matcher.find()) {
				// Récupération de la chaine ${xxx.xxx.xxx}
				String matched = matcher.group();
				// Ajout du bout de chaine précédent la valeur trouvée
				buf.append(s.substring(idx, (idx=s.indexOf(matched, idx))));
				// Extraction du nom de la variable
				String name = matched.substring(2, matched.length()-1);
				// Si cette variable est dans la pile => référence cyclique
				if (stack.contains(name))
					throw new ReportException(Strings.getString("PropertiesHelper.errors.CIRCULAR_REFERENCE", name), null); //$NON-NLS-1$ //$NON-NLS-2$
				// Récupération de sa valeur, et ajout au buffer
				String value = props.getProperty(name);
				if (value==null)
					value = System.getProperty(name);
				// Si la valeur est non nulle, on effectue le traitement
				// récursivement
				if (value!=null) {
					stack.push(name);
					value = substitueVariables(stack, value);
					stack.pop();
				}
				//if (value==null)
				//	throw new ReportException("Missing property " +
				//			"variable '${" + name + "}'", null);
				log.debug("Var(" + name + ")='" + value  +"'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// Concaténation...
				buf.append(value!=null ? value : matched);
				// Incrémentation de l'index
				idx += matched.length();
			}
			// Ajout de la fin de la chaine
			buf.append(s.substring(idx));
			result = buf.toString();
		}
		// Retour du résultat
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size() {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map t) {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		throw new Error(Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

}
