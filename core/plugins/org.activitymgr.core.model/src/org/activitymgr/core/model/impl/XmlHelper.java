package org.activitymgr.core.model.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.IDTOFactory;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Classe offrant des services de manipulation ou de génération de documents
 * XML.
 */
public class XmlHelper implements EntityResolver, ErrorHandler, ContentHandler {

	/**
	 * Interface de création des objets en base de données.
	 */
	public static interface ModelMgrDelegate {

		/**
		 * Crée une durée dans un contexte de transaction.
		 * 
		 * @param duration
		 *            la durée à créer.
		 * @return la durée créée.
		 * @throws DAOException
		 *             levé en cas d'incident technique d'accès à la base.
		 * @throws ModelException
		 *             levé dans la cas ou la durée existe déjà.
		 * @see IModelMgr#createDuration(Duration)
		 */
		public Duration createDuration(Duration duration)
				throws ModelException, DAOException;

		/**
		 * Crée un collaborateur dans un contexte de transaction.
		 * 
		 * @param collaborator
		 *            le collaborateur à créer.
		 * @return le collaborateur après création.
		 * @throws DAOException
		 *             levé en cas d'incident technique d'accès à la base.
		 * @throws ModelException
		 *             levé dans la cas ou la tache de destination ne peut
		 *             recevoir de sous-tache.
		 * @see IModelMgr#createCollaborator(Collaborator)
		 */
		public Collaborator createCollaborator(Collaborator collaborator)
				throws DAOException, ModelException;

		/**
		 * Crée une nouvelle tache dans un contexte de transaction.
		 * 
		 * @param parentTask
		 *            la tache parent de destination.
		 * @param task
		 *            la tache à créer.
		 * @return la tache créée.
		 * @throws DAOException
		 *             levé en cas d'incident technique d'accès à la base.
		 * @throws ModelException
		 *             levé dans la cas ou la tache de destination ne peut
		 *             recevoir de sous-tache.
		 * @see IModelMgr#createTask(Task, Task)
		 */
		public Task createTask(Task parentTask, Task task) throws DAOException,
				ModelException;

		/**
		 * Crée une contribution dans un contexte de transaction.
		 * 
		 * @param contribution
		 *            la contribution à créer.
		 * @return la contribution après création.
		 * @throws DAOException
		 *             levé en cas d'incident technique d'accès à la base.
		 * @throws ModelException
		 *             levé dans la cas ou la tache de destination ne peut
		 *             recevoir de contribution.
		 * @see IModelMgr#createCollaborator(Collaborator)
		 */
		public Contribution createContribution(Contribution contribution)
				throws DAOException, ModelException;

		/**
		 * Retourne la tache associée à un chemin construit à partir de codes de
		 * taches.
		 * 
		 * @param codePath
		 *            le chemin à base de code.
		 * @return la tache trouvée.
		 * @throws DAOException
		 *             levé en cas d'incident technique avec la base de données.
		 * @throws ModelException
		 *             levé dans le cas ou le chemin de tache est inconnu.
		 */
		public Task getTaskByCodePath(String codePath)
				throws DAOException, ModelException;

		/**
		 * Retourne le collabirateur dont le login est spécifié dans un contexte
		 * de transaction.
		 * 
		 * @param login
		 *            l'identifiant de connexion du collaborateur recherché.
		 * @return le collaborateur dont l'identifiant de connexion est
		 *         spécifié.
		 * @throws DAOException
		 *             levé en cas d'incident technique d'accès à la base.
		 */
		public Collaborator getCollaborator(String login)
				throws DAOException;

	}

	/** Logger */
	private static Logger log = Logger.getLogger(XmlHelper.class);

	/** Constantes */
	public static final String MODEL_NODE = "model"; //$NON-NLS-1$
	public static final String DURATIONS_NODE = "durations"; //$NON-NLS-1$
	public static final String DURATION_NODE = "duration"; //$NON-NLS-1$
	public static final String COLLABORATORS_NODE = "collaborators"; //$NON-NLS-1$
	public static final String COLLABORATOR_NODE = "collaborator"; //$NON-NLS-1$
	public static final String TASKS_NODE = "tasks"; //$NON-NLS-1$
	public static final String TASK_NODE = "task"; //$NON-NLS-1$
	public static final String CONTRIBUTIONS_NODE = "contributions"; //$NON-NLS-1$
	public static final String CONTRIBUTION_NODE = "contribution"; //$NON-NLS-1$
	public static final String LOGIN_NODE = "login"; //$NON-NLS-1$
	public static final String FIRST_NAME_NODE = "first-name"; //$NON-NLS-1$
	public static final String LAST_NAME_NODE = "last-name"; //$NON-NLS-1$
	public static final String IS_ACTIVE_NODE = "is-active"; //$NON-NLS-1$
	public static final String YEAR_ATTRIBUTE = "year"; //$NON-NLS-1$
	public static final String MONTH_ATTRIBUTE = "month"; //$NON-NLS-1$
	public static final String DAY_ATTRIBUTE = "day"; //$NON-NLS-1$
	public static final String DURATION_ATTRIBUTE = "duration"; //$NON-NLS-1$
	public static final String CONTRIBUTOR_REF_NODE = "contributor-ref"; //$NON-NLS-1$
	public static final String TASK_REF_NODE = "task-ref"; //$NON-NLS-1$
	public static final String PATH_NODE = "path"; //$NON-NLS-1$
	public static final String NAME_NODE = "name"; //$NON-NLS-1$
	public static final String BUDGET_NODE = "budget"; //$NON-NLS-1$
	public static final String INITIALLY_CONSUMED_NODE = "initially-consumed"; //$NON-NLS-1$
	public static final String TODO_NODE = "todo"; //$NON-NLS-1$
	public static final String COMMENT_NODE = "comment"; //$NON-NLS-1$
	public static final String VALUE_NODE = "value"; //$NON-NLS-1$

	/** Gestionnaire du modèle */
	private ModelMgrDelegate modelMgrDelegate;

	/** Sauvegarde des objets en cours de chargement */
	private Duration currentDuration;
	private Collaborator currentCollaborator;
	private Task currentParentTask;
	private Task currentTask;
	private Contribution currentContribution;
	private StringBuffer currentText = new StringBuffer();
	private IDTOFactory factory;

	/** Pointeur de location dans le fichier XML parsé */
	private Locator locator;


	/**
	 * Constructeur par défaut.
	 * 
	 * @param modelMgrDelegate
	 *            délégué du gestionnaire de modèle.
	 * @param tx
	 *            contexte de transaction.
	 */
	public XmlHelper(ModelMgrDelegate modelMgrDelegate, IDTOFactory factory) {
		this.modelMgrDelegate = modelMgrDelegate;
		this.factory = factory;
	}

	/** EntityResolver interface methods */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
	 * java.lang.String)
	 */
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		log.debug("resolveEntity(" + publicId + ", " + systemId + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new InputSource(
				IModelMgr.class.getResourceAsStream("activitymgr.dtd")); //$NON-NLS-1$
	}

	/** ErrorHandler interface methods */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException e) throws SAXParseException {
		log.error(
				"SAX error line : " + e.getLineNumber() + " column : " + e.getColumnNumber(), e); //$NON-NLS-1$ //$NON-NLS-2$
		throw e;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	public void fatalError(SAXParseException e) throws SAXParseException {
		error(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
	 */
	public void warning(SAXParseException e) throws SAXParseException {
		error(e);
	}

	/** ContentHandlet interface methods */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		log.debug("start(" + qName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (MODEL_NODE.equals(qName) || DURATIONS_NODE.equals(qName)
				|| VALUE_NODE.equals(qName) || COLLABORATORS_NODE.equals(qName)
				|| LOGIN_NODE.equals(qName) || FIRST_NAME_NODE.equals(qName)
				|| LAST_NAME_NODE.equals(qName) || TASKS_NODE.equals(qName)
				|| PATH_NODE.equals(qName) || NAME_NODE.equals(qName)
				|| IS_ACTIVE_NODE.equals(qName) || BUDGET_NODE.equals(qName)
				|| INITIALLY_CONSUMED_NODE.equals(qName)
				|| TODO_NODE.equals(qName) || COMMENT_NODE.equals(qName)
				|| CONTRIBUTIONS_NODE.equals(qName)
				|| CONTRIBUTOR_REF_NODE.equals(qName)
				|| TASK_REF_NODE.equals(qName)) {
			// Do nothing...
		} else if (DURATION_NODE.equals(qName)) {
			currentDuration = factory.newDuration();
		} else if (COLLABORATOR_NODE.equals(qName)) {
			currentCollaborator = factory.newCollaborator();
		} else if (TASK_NODE.equals(qName)) {
			currentTask = factory.newTask();
		} else if (CONTRIBUTION_NODE.equals(qName)) {
			currentContribution = factory.newContribution();
			currentContribution.setYear((int) getNumAttrValue(atts,
					YEAR_ATTRIBUTE));
			currentContribution.setMonth((int) getNumAttrValue(atts,
					MONTH_ATTRIBUTE));
			currentContribution.setDay((int) getNumAttrValue(atts,
					DAY_ATTRIBUTE));
			currentContribution.setDurationId(getNumAttrValue(atts,
					DURATION_ATTRIBUTE));
		} else {
			error(new SAXParseException(Strings.getString(
					"XmlHelper.errors.UNEXPECTED_NODE", qName), locator)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Retourne la valeur d'un attribut numérique ou lève une exception si
	 * l'attribut n'existe pas ou qu'il n'a pas un format correct.
	 * 
	 * @param atts
	 *            la liste des attributs.
	 * @param qName
	 *            le nom de l'attribut.
	 * @return la valeur de l'attribut.
	 * @throws SAXParseException
	 *             levé en cas d'absence ou de mauvais format de l'attribut.
	 */
	private long getNumAttrValue(Attributes atts, String qName)
			throws SAXParseException {
		String value = atts.getValue(qName);
		if (value == null)
			error(new SAXParseException(Strings.getString(
					"XmlHelper.errors.MISSING_ATTRIBUTE", qName), locator)); //$NON-NLS-1$ //$NON-NLS-2$
		// Parsing
		long result = -1;
		try {
			result = Long.parseLong(value);
		} catch (NumberFormatException e) {
			error(new SAXParseException(
					Strings.getString(
							"XmlHelper.errors.INVALID_ATTRIBUTE", value, qName), locator)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		// Retour du résultat
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		log.debug("end(" + qName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			String textToSave = currentText.toString();
			// Réinitialisation de la sauvegarde du texte courant
			currentText.setLength(0);
			if (MODEL_NODE.equals(qName) || DURATIONS_NODE.equals(qName)
					|| COLLABORATORS_NODE.equals(qName)
					|| TASKS_NODE.equals(qName)
					|| CONTRIBUTIONS_NODE.equals(qName)) {
				// Do nothing...
			} else if (DURATION_NODE.equals(qName)) {
				Duration durationToCreate = currentDuration;
				currentDuration = null;
				modelMgrDelegate.createDuration(durationToCreate);
			} else if (COLLABORATOR_NODE.equals(qName)) {
				Collaborator collaboratorToCreate = currentCollaborator;
				currentCollaborator = null;
				modelMgrDelegate.createCollaborator(
						collaboratorToCreate);
			} else if (LOGIN_NODE.equals(qName)) {
				currentCollaborator.setLogin(textToSave);
			} else if (FIRST_NAME_NODE.equals(qName)) {
				currentCollaborator.setFirstName(textToSave);
			} else if (LAST_NAME_NODE.equals(qName)) {
				currentCollaborator.setLastName(textToSave);
			} else if (IS_ACTIVE_NODE.equals(qName)) {
				boolean isActive = "true".equalsIgnoreCase(textToSave); //$NON-NLS-1$
				if (currentCollaborator != null)
					currentCollaborator.setIsActive(isActive);
				else
					currentDuration.setIsActive(isActive);
			} else if (VALUE_NODE.equals(qName)) {
				currentDuration.setId(Long.parseLong(textToSave));
			} else if (TASK_NODE.equals(qName)) {
				Task taskToCreate = currentTask;
				Task parentOfTaskToCreate = currentParentTask;
				currentTask = null;
				currentParentTask = null;
				modelMgrDelegate.createTask(parentOfTaskToCreate,
						taskToCreate);
			} else if (PATH_NODE.equals(qName)) {
				log.debug("textToSave='" + textToSave + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				String parentPath = textToSave.substring(0,
						textToSave.lastIndexOf('/'));
				log.debug("parentPath='" + parentPath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				currentParentTask = "".equals(parentPath) ? null : modelMgrDelegate.getTaskByCodePath(parentPath); //$NON-NLS-1$
				String taskCode = textToSave.substring(parentPath.length() + 1);
				currentTask.setCode(taskCode);
			} else if (NAME_NODE.equals(qName)) {
				currentTask.setName(textToSave);
			} else if (BUDGET_NODE.equals(qName)) {
				currentTask.setBudget(Long.parseLong(textToSave));
			} else if (INITIALLY_CONSUMED_NODE.equals(qName)) {
				currentTask.setInitiallyConsumed(Long.parseLong(textToSave));
			} else if (TODO_NODE.equals(qName)) {
				currentTask.setTodo(Long.parseLong(textToSave));
			} else if (COMMENT_NODE.equals(qName)) {
				String comment = textToSave != null ? textToSave.trim() : ""; //$NON-NLS-1$
				if ("".equals(comment)) //$NON-NLS-1$
					comment = null;
				currentTask.setComment(comment);
			} else if (CONTRIBUTION_NODE.equals(qName)) {
				Contribution contributionToCreate = currentContribution;
				currentContribution = null;
				modelMgrDelegate.createContribution(
						contributionToCreate);
			} else if (CONTRIBUTOR_REF_NODE.equals(qName)) {
				Collaborator collaborator = modelMgrDelegate.getCollaborator(
						 textToSave);
				currentContribution.setContributorId(collaborator.getId());
			} else if (TASK_REF_NODE.equals(qName)) {
				Task task = modelMgrDelegate.getTaskByCodePath(
						textToSave);
				currentContribution.setTaskId(task.getId());
			} else {
				error(new SAXParseException(Strings.getString(
						"XmlHelper.errors.UNEXPECTED_NODE", qName), locator)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (NumberFormatException e) {
			log.error("Number format error", e); //$NON-NLS-1$
			error(new SAXParseException(
					Strings.getString("XmlHelper.errors.NUMBER_FORMAT_ERROR") + e.getMessage(), locator, e)); //$NON-NLS-1$
		} catch (ModelException e) {
			log.error("Model violation", e); //$NON-NLS-1$
			error(new SAXParseException(
					Strings.getString("XmlHelper.errors.MODEL_VIOLATION") + e.getMessage(), locator, e)); //$NON-NLS-1$
		} catch (DAOException e) {
			log.error("Unexpected database access error", e); //$NON-NLS-1$
			throw new SAXException(
					Strings.getString("XmlHelper.errors.DATABASE_ACCESS_ERROR"), e); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		log.debug("endDocument()"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		log.debug("startDocument()"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String chars = new String(ch, start, length);
		log.debug("characters(" + chars + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		currentText.append(chars);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		log.debug("ignorableWhitespace(" + new String(ch, start, length) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
	 */
	public void endPrefixMapping(String prefix) throws SAXException {
		if (!"xml".equals(prefix) && !"xmlns".equals(prefix))
			saxFeatureNotImplemented("endPrefixMapping"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
	 */
	public void skippedEntity(String name) throws SAXException {
		saxFeatureNotImplemented("skippedEntity"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		log.debug("setDocumentLocator(" + locator + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		this.locator = locator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String,
	 * java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		saxFeatureNotImplemented("processingInstruction"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String,
	 * java.lang.String)
	 */
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		if (!"xml".equals(prefix) && !"xmlns".equals(prefix))
			saxFeatureNotImplemented("startPrefixMapping"); //$NON-NLS-1$
	}

	/**
	 * Lève une exception indiquant qu'une fontionnalité n'est pas implémentée
	 * pour ActivityManager
	 * 
	 * @param featureName
	 *            nom de la fonctionnalité
	 * @throws SAXException
	 *             levée dans tous les cas.
	 */
	private void saxFeatureNotImplemented(String featureName)
			throws SAXException {
		error(new SAXParseException(Strings.getString(
				"XmlHelper.errors.NOT_IMPLEMENTED", featureName), locator)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Static XML serialization methods */

	/**
	 * Commence un noeud XML dans le flux d'écriture.
	 * 
	 * @param indent
	 *            l'indentation.
	 * @param out
	 *            le flux d'écriture.
	 * @param name
	 *            le nom du noeud XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	public static void startXmlNode(OutputStream out, String indent, String name)
			throws IOException {
		print(out, indent);
		out.write('<');
		print(out, name);
		out.write('>');
		out.write('\n');
	}

	/**
	 * Termine un noeud XML dans le flux d'écriture avec une indentation.
	 * 
	 * @param out
	 *            le flux d'écriture.
	 * @param indent
	 *            l'indentation.
	 * @param name
	 *            le nom du noeud XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	public static void endXmlNode(OutputStream out, String indent, String name)
			throws IOException {
		print(out, indent);
		endXmlNode(out, name);
	}

	/**
	 * Termine un noeud XML dans le flux d'écriture.
	 * 
	 * @param out
	 *            le flux d'écriture.
	 * @param name
	 *            le nom du noeud XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	public static void endXmlNode(OutputStream out, String name)
			throws IOException {
		out.write('<');
		out.write('/');
		print(out, name);
		out.write('>');
		out.write('\n');
	}

	/**
	 * Ecrit un noeud XML dans le flux d'écriture.
	 * 
	 * @param indent
	 *            l'indentation.
	 * @param out
	 *            le flux d'écriture.
	 * @param name
	 *            le nom du noeud XML.
	 * @param value
	 *            la valeur du noeud XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	public static void printTextNode(OutputStream out, String indent,
			String name, String value) throws IOException {
		print(out, indent);
		out.write('<');
		print(out, name);
		out.write('>');
		printTextValue(out, value);
		endXmlNode(out, name);
	}

	/**
	 * Ecrit un attribut de noeud XML dans le flux d'écriture.
	 * 
	 * @param out
	 *            le flux d'écriture.
	 * @param name
	 *            le nom de l'attribut XML.
	 * @param value
	 *            la valeur de l'attribut XML.
	 * @throws IOException
	 *             levé en cas d'incident I/O lors de l'écriture sur le flux de
	 *             sortie.
	 */
	public static void printTextAttribute(OutputStream out, String name,
			String value) throws IOException {
		out.write(' ');
		print(out, name);
		print(out, "=\""); //$NON-NLS-1$
		printTextValue(out, value);
		print(out, "\""); //$NON-NLS-1$
	}

	/**
	 * Ecrit une chaîne de caractères dans le flux de sortie en remplaçant les
	 * caractères spéciaux.
	 * 
	 * @param out
	 *            le flux de sortie.
	 * @param str
	 *            la chaîne de caractères.
	 * @throws IOException
	 *             levé en cas d'incident lors de l'écriture des données sur le
	 *             flux.
	 */
	public static void printTextValue(OutputStream out, String str)
			throws IOException {
		str = str.replaceAll("&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
		str = str.replaceAll(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
		str = str.replaceAll("<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
		print(out, str);
	}

	/**
	 * Ecrit une chaîne de caractères dans le flux de sortie.
	 * 
	 * @param out
	 *            le flux de sortie.
	 * @param str
	 *            la chaîne de caractères.
	 * @throws IOException
	 *             levé en cas d'incident lors de l'écriture des données sur le
	 *             flux.
	 */
	public static void print(OutputStream out, String str) throws IOException {
		out.write(str.getBytes("UTF-8")); //$NON-NLS-1$
	}

	/**
	 * Ecrit une chaîne de caractères dans le flux de sortie.
	 * 
	 * @param out
	 *            le flux de sortie.
	 * @param s
	 *            la chaîne de caractères.
	 * @throws IOException
	 *             levé en cas d'incident lors de l'écriture des données sur le
	 *             flux.
	 */
	public static void println(OutputStream out, String s) throws IOException {
		print(out, s);
		out.write('\n');
	}

}
