/*
 * Copyright (c) 2004-2010, Jean-Fran�ois Brazeau. All rights reserved.
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
 * Classe offrant des services d'acc�s aux propri�t�s de configuration pour la
 * g�n�ration de rapports avec Velocity.
 * 
 * <p>
 * Pour Velocity, une variable de type <code>${a.b}</code> peut vouloir dire au
 * choix :
 * <ul>
 * <li>l'attibut b de l'instance a (ou a est un bean Java)</li>
 * <li>la propri�t� b de l'instance a (ou a est une Map)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * On exploite la deuxi�me possibilit� pour rendre '<i>visible</i>' une
 * propri�t� d�finie dans le fichier de configuration des rapports dans un
 * template velocity. Une instance de <code>PropertiesHelper</code> est
 * enregistr�e dans le contexte velocity sous le nom <code>props</code>. Comme
 * cette classe impl�mente l'interface <code>java.util.Map</code>, Velocity
 * invoque la m�thode <code>get(String)</code> en lui passant en argument la
 * chaine aussitot plac�e apr�s <code>props.</code>. La fin de la propri�t� est
 * marqu�e par l'invocation de la m�thode <code>get()</code>.
 * </p>
 * 
 * <p>
 * Il est suppos� que les propri�t�s contenues dans le fichier de configuration
 * et acc�d�es depuis les templates Velocity sont pr�fix�es par :<br>
 * <code>reports.&lt;reportId&gt;</code> ou <code>reportId</code> d�signe
 * l'identifiant du rapport.
 * </p>
 * 
 * <p>
 * Exemple : <code>${props.startYear.get()}</code> pour le rapport dont
 * l'identifiant est <code>myReport</code> va r�f�rencer la propri�t�
 * <code>reports.myReport.startYear</code> dans le fichier de configuration. Si
 * cette propri�t� n'est pas d�finie, le moteur regarde ensuite tour � tour les
 * propri�t�s <code>reports.startYear</code> et <code>startYear</code>. Si
 * toutes ces propri�t�s sont ind�finies, une exception est lev�e.
 * </p>
 * 
 * <p>
 * Dans le cas ou le nom de la propri�t� est compos�e (ex :
 * <code>start.year</code>), la m�thode <code>get</code> retourne l'instance de
 * <code>PropertiesHelper</code> et la premi�re partie du nom de la propri�t�
 * est d�pos�e sur la pile. Lors de l'invocation suivante, les diff�rentes
 * parties du nom de la propri�t� sont r�assembl�es pour former le nom de la
 * propri�t� dans son ensemble.
 * </p>
 * 
 * <p>
 * Pour cette raison, cette classe n'est pas thread safe.
 * </p>
 * 
 * @see jfb.tools.activitymgr.report.ReportMgr
 */
public class PropertiesHelper implements Map<Object, Object> {

	/** Logger */
	private static Logger log = Logger.getLogger(PropertiesHelper.class);

	/** Dictionnaire de propri�t�s */
	private Properties props;

	/** Identifiant courant de rapport */
	private String currentReportId;

	/** Pile utilis�e pour extraire les propri�t�s */
	private Stack<String> stack = new Stack<String>();

	/**
	 * Constructeur par d�faut.
	 * 
	 * @param reportId
	 *            identifiant du rapport courant.
	 * @param props
	 *            dictionnaire par d�faut.
	 */
	public PropertiesHelper(String reportId, Properties props) {
		this.currentReportId = reportId;
		this.props = props;
	}

	/**
	 * Retourne la valeur de la propri�t� dont la cl� est constitu� des cha�nes
	 * empil�es dans la pile.
	 * 
	 * @return la valeur de la propri�t�.
	 * @throws ReportException
	 *             lev� dans le cas o� la cl� n'existe pas.
	 */
	public String get() throws ReportException {
		// Construction de la fin de la cl�
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < stack.size(); i++) {
			if (i > 0)
				buf.append('.');
			buf.append(stack.get(i));
		}
		log.debug("get(" + buf.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		// Purge de la pile
		stack.clear();
		// Lecture de la 1� cl� par d�faut
		String propKey0 = new StringBuffer("reports.") //$NON-NLS-1$
				.append(currentReportId).append('.').append(buf.toString())
				.toString();
		log.debug("PropertyKey=" + propKey0); //$NON-NLS-1$
		String propValue = props.getProperty(propKey0);
		// Lecture de la 2� cl� par d�faut si la 1� n'a rien donn�
		if (propValue == null) {
			String propKey1 = new StringBuffer("reports.") //$NON-NLS-1$
					.append(buf.toString()).toString();
			log.debug("PropertyKey=" + propKey1); //$NON-NLS-1$
			propValue = props.getProperty(propKey1);
			// Lecture de la 3� cl� par d�faut si la 1� et la 2�
			// n'ont rien donn�
			if (propValue == null) {
				String propKey2 = buf.toString();
				log.debug("PropertyKey=" + propKey2); //$NON-NLS-1$
				propValue = props.getProperty(propKey2);
				if (propValue == null)
					throw new ReportException(
							Strings.getString(
									"PropertiesHelper.errors.PARAMETER_NOT_SPECIFIED", new Object[] { buf.toString(), propKey0, propKey1, propKey2 }), null); //$NON-NLS-1$
			}
		}
		// Retour du r�sultat
		String value = substitueVariables(propValue);
		log.debug("PropertyValue=" + value); //$NON-NLS-1$
		return value;
	}

	/**
	 * Retourne la valeur de la propri�t� dont le postfix est sp�cifi�.
	 * 
	 * @param postPropertyKey
	 *            le postfix de cl� de propri�t�.
	 * @return la valeur de la propri�t�.
	 * @throws ReportException
	 *             lev� en cas de non conformit� de la valeur de la cha�ne.
	 */
	public String getProperty(String postPropertyKey) throws ReportException {
		String propKey0 = new StringBuffer("reports.") //$NON-NLS-1$
				.append(currentReportId).append('.').append(postPropertyKey)
				.toString();
		log.debug("PropertyKey=" + propKey0); //$NON-NLS-1$
		String propValue = props.getProperty(propKey0);
		if (propValue != null)
			propValue = substitueVariables(propValue);
		log.debug("PropertyValue=" + propValue); //$NON-NLS-1$
		return propValue;
	}

	/**
	 * Substitue les cha�nes de type ${xxx.xxx.xxx} par leur valeur trouv�es
	 * dans les propri�t�s syst�me.
	 * 
	 * @param s
	 *            la cha�ne de caract�re � parcourir.
	 * @return le r�sultat.
	 * @throws ReportException
	 *             lev� en cas de non conformit� de la cha�ne.
	 */
	private String substitueVariables(String s) throws ReportException {
		return substitueVariables(new Stack<String>(), s);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		log.debug("Append '" + key + "' to property stack"); //$NON-NLS-1$ //$NON-NLS-2$
		stack.push((String) key);
		return this;
	}

	/**
	 * Remplace les occurences de variables de type <code>${xxx}</code> par leur
	 * valeur lorsqu'elle existe dans le dictionnaire.
	 * 
	 * @param stack
	 *            pile permettant de d�tecter les r�f�rences circulaires.
	 * @param s
	 *            la cha�ne contenant les variables.
	 * @return la cha�ne substitu�e.
	 * @throws ReportException
	 *             lev� en cas de d�tection de r�f�rences circulaires.
	 */
	private String substitueVariables(Stack<String> stack, String s)
			throws ReportException {
		String result = null;
		if (s != null) {
			// Pr�paration de l'expression r�guli�re
			Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9.'_-]+\\}"); //$NON-NLS-1$
			Matcher matcher = pattern.matcher(s);
			// Pr�paration du buffer accueillant la chaine substitu�e
			StringBuffer buf = new StringBuffer();
			int idx = 0;
			// Parcours des valeurs obtenues
			while (matcher.find()) {
				// R�cup�ration de la chaine ${xxx.xxx.xxx}
				String matched = matcher.group();
				// Ajout du bout de chaine pr�c�dent la valeur trouv�e
				buf.append(s.substring(idx, (idx = s.indexOf(matched, idx))));
				// Extraction du nom de la variable
				String name = matched.substring(2, matched.length() - 1);
				// Si cette variable est dans la pile => r�f�rence cyclique
				if (stack.contains(name))
					throw new ReportException(
							Strings.getString(
									"PropertiesHelper.errors.CIRCULAR_REFERENCE", name), null); //$NON-NLS-1$ //$NON-NLS-2$
				// R�cup�ration de sa valeur, et ajout au buffer
				String value = props.getProperty(name);
				if (value == null)
					value = System.getProperty(name);
				// Si la valeur est non nulle, on effectue le traitement
				// r�cursivement
				if (value != null) {
					stack.push(name);
					value = substitueVariables(stack, value);
					stack.pop();
				}
				// if (value==null)
				// throw new ReportException("Missing property " +
				// "variable '${" + name + "}'", null);
				log.debug("Var(" + name + ")='" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// Concat�nation...
				buf.append(value != null ? value : matched);
				// Incr�mentation de l'index
				idx += matched.length();
			}
			// Ajout de la fin de la chaine
			buf.append(s.substring(idx));
			result = buf.toString();
		}
		// Retour du r�sultat
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#size()
	 */
	public int size() {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection<Object> values() {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<?, ?> t) {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set<Entry<Object, Object>> entrySet() {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#keySet()
	 */
	public Set<Object> keySet() {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		throw new Error(
				Strings.getString("PropertiesHelper.errors.NOT_IMPLEMENTED")); //$NON-NLS-1$
	}

}
