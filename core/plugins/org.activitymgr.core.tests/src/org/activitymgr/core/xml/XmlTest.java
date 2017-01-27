package org.activitymgr.core.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.activitymgr.core.AbstractModelTestCase;
import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Contribution;
import org.activitymgr.core.dto.Duration;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.ModelException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

public class XmlTest extends AbstractModelTestCase {

	/** Logger */
	private static Logger log = Logger.getLogger(XmlTest.class);

	public void testEmptyFile() throws IOException, ParserConfigurationException, SAXException, ModelException {
		importTestFile();
	}

	public void testMissingDoctype() throws IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Missing DOCTYPE must generate an sax error");
	}

	public void testBadXmlFormat() throws IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Bad XML format must generate an sax error");
	}

	public void testCollaboratorInDurations() throws IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Wrong collaborator situation in XML file must generate an sax error");
	}

	public void testMissingCollaboratorLogin() throws IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Missing collaborator login must generate an sax error");
	}

	public void testCreateDurations() throws IOException, ParserConfigurationException, SAXException, ModelException {
		// Vérification du nombre initial de durées
		Duration[] initialDurations = getModelMgr().getDurations();

		// Import du fichier associé au test
		importTestFile();
		
		// Vérification du nombre de durées après import
		Duration[] durations = getModelMgr().getDurations();
		assertNotNull(durations);
		assertEquals(initialDurations.length + 3, durations.length);

		// Suppression des durées
		Duration duration = getFactory().newDuration();
		duration.setId(200);
		getModelMgr().removeDuration(duration);
		duration.setId(300);
		getModelMgr().removeDuration(duration);
		duration.setId(400);
		getModelMgr().removeDuration(duration);
	}
	
	public void testCreateCollaborators() throws IOException, ParserConfigurationException, SAXException, ModelException {
		// Vérification du nombre initial de durées
		Collaborator[] initialCollaborators = getModelMgr().getCollaborators();
		assertNotNull(initialCollaborators);
		assertEquals(0, initialCollaborators.length);

		// Import du fichier associé au test
		importTestFile();
		
		// Vérification du nombre de durées après import
		Collaborator[] collaborators = getModelMgr().getCollaborators();
		assertNotNull(collaborators);
		assertEquals(2, collaborators.length);

		// Suppression des durées
		getModelMgr().removeCollaborator(collaborators[0]);
		getModelMgr().removeCollaborator(collaborators[1]);
	}
	
	public void testDuplicateCollaboratorLogin() throws IOException, ParserConfigurationException, SAXException, ModelException {
		// Vérification du nombre initial de collaborateurs
		Collaborator[] initialCollaborators = getModelMgr().getCollaborators();
		assertNotNull(initialCollaborators);
		assertEquals(0, initialCollaborators.length);

		// Import du fichier associé au test
		try {
			importTestFile();
			fail("Duplicate login must generate an error");
		}
		catch (ModelException expected) {
			// Do nothing...
		}
	}

	public void testCreateTasks() throws IOException, ParserConfigurationException, SAXException, ModelException {
		// Vérification du nombre initial de taches
		Task[] initialTasks = getModelMgr().getSubTasks(null);
		assertNotNull(initialTasks);
		assertEquals(0, initialTasks.length);

		// Import du fichier associé au test
		importTestFile();
		
		// Vérification du nombre de durées après import
		Task[] tasks = getModelMgr().getSubTasks(null);
		assertNotNull(tasks);
		assertEquals(1, tasks.length);

		// Suppression des durées
		getModelMgr().removeTask(tasks[0]);
	}
	
	public void testCreateContributions() throws IOException, ParserConfigurationException, SAXException, ModelException {
		// Vérification du nombre initial de taches
		Contribution[] initialContributions = getModelMgr().getContributions(null, null, null, null);
		assertNotNull(initialContributions);
		assertEquals(0, initialContributions.length);

		// Import du fichier associé au test
		importTestFile();
		
		// Vérification du nombre de durées après import
		Contribution[] contributions = getModelMgr().getContributions(null, null, null, null);
		assertNotNull(contributions);
		assertEquals(2, contributions.length);

		// Suppression des données
		getModelMgr().removeContribution(contributions[0], false);
		getModelMgr().removeContribution(contributions[1], false);
		Task[] tasks = getModelMgr().getSubTasks(null);
		getModelMgr().removeTask(tasks[1]);
		getModelMgr().removeTask(tasks[0]);
		Collaborator[] collaborators = getModelMgr().getCollaborators();
		getModelMgr().removeCollaborator(collaborators[0]);
		getModelMgr().removeCollaborator(collaborators[1]);
		Duration durations[] = getModelMgr().getDurations();
		for (int i=0; i<durations.length; i++)
			getModelMgr().removeDuration(durations[i]);
		
	}
	
	public void testExportAndImport() throws ModelException, IOException, ParserConfigurationException, SAXException {
		// Création des objets de test
		Duration duration = getFactory().newDuration();
		duration.setId(100);
		duration = getModelMgr().createDuration(duration);
		Collaborator collaborator = getFactory().newCollaborator();
		collaborator.setLogin("login");
		collaborator.setFirstName("FirstName");
		collaborator.setLastName("LastName");
		collaborator = getModelMgr().createCollaborator(collaborator);
		Task parentTask = getFactory().newTask();
		parentTask.setCode("Par");
		parentTask.setName("Parent name");
		parentTask = getModelMgr().createTask(null, parentTask);
		Task task = getFactory().newTask();
		task.setCode("Code");
		task.setName("Name");
		task.setBudget(1);
		task.setInitiallyConsumed(2);
		task.setTodo(3);
		task = getModelMgr().createTask(parentTask, task);
		Contribution contribution = getFactory().newContribution();
		contribution.setYear(2006);
		contribution.setMonth(01);
		contribution.setDay(01);
		contribution.setContributorId(collaborator.getId());
		contribution.setTaskId(task.getId());
		contribution.setDurationId(duration.getId());
		contribution = getModelMgr().createContribution(contribution, false);

		// Export
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		getModelMgr().exportToXML(out);
		out.close();
		String export = out.toString();
		log.debug("export :");
		log.debug(export);

		// Supression des objets de test
		getModelMgr().removeContribution(contribution, true);
		getModelMgr().removeTask(task);
		getModelMgr().removeTask(parentTask);
		getModelMgr().removeCollaborator(collaborator);
		getModelMgr().removeDuration(duration);

		// Réimportation
		getModelMgr().importFromXML(new ByteArrayInputStream(out.toByteArray()));
		
		// Nouvel export puis comparaison
		out = new ByteArrayOutputStream();
		getModelMgr().exportToXML(out);
		out.close();
		String export2 = out.toString();
		log.debug("export2 :");
		log.debug(export2);
		
		// Comparaison
		assertEquals(export, export2);

		// Supression des objets de test ayant été réimportés
		contribution = getModelMgr().getContributions(null, null, null, null)[0];
		getModelMgr().removeContribution(contribution, false);
		task = getModelMgr().getTaskByCodePath("/Par/Code");
		getModelMgr().removeTask(task);
		parentTask = getModelMgr().getTaskByCodePath("/Par");
		getModelMgr().removeTask(parentTask);
		collaborator = getModelMgr().getCollaborators()[0];
		getModelMgr().removeCollaborator(collaborator);
		getModelMgr().removeDuration(duration);
	}
	
	/*
	 * Méthodes privées 
	 */

	private void assertSAXExceptionThrown(String testFailMessage) throws IOException, ParserConfigurationException, ModelException {
		try {
			importTestFile();
			fail(testFailMessage);
		}
		catch (SAXException e) {
			// Exception normalement levée en cas de mauvais format
		}
	}

	private void importTestFile() throws IOException, ParserConfigurationException, SAXException, ModelException {
		String testName = getName();
		log.debug("testName='" + testName + "'");
		String filePostfix = testName.substring(4);
		log.debug("filePostfix='" + filePostfix + "'");
		String fileName = "XmlTest." + filePostfix + ".xml";
		log.debug("fileName='" + fileName + "'");
		// Ouverture du fichier de test
		InputStream in = XmlTest.class.getResourceAsStream(fileName);
		if (in==null) {
			throw new FileNotFoundException(fileName);
		}
		// Importation des données
		getModelMgr().importFromXML(in);
		// Fermeture du flux
		in.close();
	}
	
}
