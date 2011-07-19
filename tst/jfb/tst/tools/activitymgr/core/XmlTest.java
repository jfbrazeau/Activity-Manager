package jfb.tst.tools.activitymgr.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Duration;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlTest extends AbstractModelTestCase {

	/** Logger */
	private static Logger log = Logger.getLogger(XmlTest.class);

	public void testEmptyFile() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		importTestFile();
	}

	public void testMissingDoctype() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Missing DOCTYPE must generate an sax error");
	}

	public void testBadXmlFormat() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Bad XML format must generate an sax error");
	}

	public void testCollaboratorInDurations() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Wrong collaborator situation in XML file must generate an sax error");
	}

	public void testMissingCollaboratorLogin() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		assertSAXExceptionThrown("Missing collaborator login must generate an sax error");
	}

	public void testCreateDurations() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		// V�rification du nombre initial de dur�es
		Duration[] initialDurations = ModelMgr.getDurations();

		// Import du fichier associ� au test
		importTestFile();
		
		// V�rification du nombre de dur�es apr�s import
		Duration[] durations = ModelMgr.getDurations();
		assertNotNull(durations);
		assertEquals(initialDurations.length + 3, durations.length);

		// Suppression des dur�es
		Duration duration = new Duration();
		duration.setId(200);
		ModelMgr.removeDuration(duration);
		duration.setId(300);
		ModelMgr.removeDuration(duration);
		duration.setId(400);
		ModelMgr.removeDuration(duration);
	}
	
	public void testCreateCollaborators() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		// V�rification du nombre initial de dur�es
		Collaborator[] initialCollaborators = ModelMgr.getCollaborators();
		assertNotNull(initialCollaborators);
		assertEquals(0, initialCollaborators.length);

		// Import du fichier associ� au test
		importTestFile();
		
		// V�rification du nombre de dur�es apr�s import
		Collaborator[] collaborators = ModelMgr.getCollaborators();
		assertNotNull(collaborators);
		assertEquals(2, collaborators.length);

		// Suppression des dur�es
		ModelMgr.removeCollaborator(collaborators[0]);
		ModelMgr.removeCollaborator(collaborators[1]);
	}
	
	public void testDuplicateCollaboratorLogin() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		// V�rification du nombre initial de collaborateurs
		Collaborator[] initialCollaborators = ModelMgr.getCollaborators();
		assertNotNull(initialCollaborators);
		assertEquals(0, initialCollaborators.length);

		// Import du fichier associ� au test
		try {
			importTestFile();
			fail("Duplicate login must generate an error");
		}
		catch (SAXParseException expected) {
			// Do nothing...
		}
	}

	public void testCreateTasks() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		// V�rification du nombre initial de taches
		Task[] initialTasks = ModelMgr.getSubtasks((Task) null);
		assertNotNull(initialTasks);
		assertEquals(0, initialTasks.length);

		// Import du fichier associ� au test
		importTestFile();
		
		// V�rification du nombre de dur�es apr�s import
		Task[] tasks = ModelMgr.getSubtasks((Task) null);
		assertNotNull(tasks);
		assertEquals(1, tasks.length);

		// Suppression des dur�es
		ModelMgr.removeTask(tasks[0]);
	}
	
	public void testCreateContributions() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
		// V�rification du nombre initial de taches
		Contribution[] initialContributions = ModelMgr.getContributions(null, null, null, null, null);
		assertNotNull(initialContributions);
		assertEquals(0, initialContributions.length);

		// Import du fichier associ� au test
		importTestFile();
		
		// V�rification du nombre de dur�es apr�s import
		Contribution[] contributions = ModelMgr.getContributions(null, null, null, null, null);
		assertNotNull(contributions);
		assertEquals(2, contributions.length);

		// Suppression des donn�es
		ModelMgr.removeContribution(contributions[0], false);
		ModelMgr.removeContribution(contributions[1], false);
		Task[] tasks = ModelMgr.getSubtasks((Task) null);
		ModelMgr.removeTask(tasks[1]);
		ModelMgr.removeTask(tasks[0]);
		Collaborator[] collaborators = ModelMgr.getCollaborators();
		ModelMgr.removeCollaborator(collaborators[0]);
		ModelMgr.removeCollaborator(collaborators[1]);
		Duration durations[] = ModelMgr.getDurations();
		for (int i=0; i<durations.length; i++)
			ModelMgr.removeDuration(durations[i]);
		
	}
	
	public void testExportAndImport() throws DbException, ModelException, IOException, ParserConfigurationException, SAXException {
		// Cr�ation des objets de test
		Duration duration = new Duration();
		duration.setId(100);
		duration = ModelMgr.createDuration(duration);
		Collaborator collaborator = new Collaborator();
		collaborator.setLogin("login");
		collaborator.setFirstName("FirstName");
		collaborator.setLastName("LastName");
		collaborator = ModelMgr.createCollaborator(collaborator);
		Task parentTask = new Task();
		parentTask.setCode("Par");
		parentTask.setName("Parent name");
		parentTask = ModelMgr.createTask(null, parentTask);
		Task task = new Task();
		task.setCode("Code");
		task.setName("Name");
		task.setBudget(1);
		task.setInitiallyConsumed(2);
		task.setTodo(3);
		task = ModelMgr.createTask(parentTask, task);
		Contribution contribution = new Contribution();
		contribution.setYear(2006);
		contribution.setMonth(01);
		contribution.setDay(01);
		contribution.setContributorId(collaborator.getId());
		contribution.setTaskId(task.getId());
		contribution.setDurationId(duration.getId());
		contribution = ModelMgr.createContribution(contribution, false);

		// Export
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ModelMgr.exportToXML(out);
		out.close();
		String export = out.toString();
		log.debug("export :");
		log.debug(export);

		// Supression des objets de test
		ModelMgr.removeContribution(contribution, true);
		ModelMgr.removeTask(task);
		ModelMgr.removeTask(parentTask);
		ModelMgr.removeCollaborator(collaborator);
		ModelMgr.removeDuration(duration);

		// R�importation
		ModelMgr.importFromXML(new ByteArrayInputStream(out.toByteArray()));
		
		// Nouvel export puis comparaison
		out = new ByteArrayOutputStream();
		ModelMgr.exportToXML(out);
		out.close();
		String export2 = out.toString();
		log.debug("export2 :");
		log.debug(export2);
		
		// Comparaison
		assertEquals(export, export2);

		// Supression des objets de test ayant �t� r�import�s
		contribution = ModelMgr.getContributions(null, null, null, null, null)[0];
		ModelMgr.removeContribution(contribution, false);
		task = ModelMgr.getTaskByCodePath("/Par/Code");
		ModelMgr.removeTask(task);
		parentTask = ModelMgr.getTaskByCodePath("/Par");
		ModelMgr.removeTask(parentTask);
		collaborator = ModelMgr.getCollaborators()[0];
		ModelMgr.removeCollaborator(collaborator);
		ModelMgr.removeDuration(duration);
	}
	
	/*
	 * M�thodes priv�es 
	 */

	private void assertSAXExceptionThrown(String testFailMessage) throws DbException, IOException, ParserConfigurationException, ModelException {
		try {
			importTestFile();
			fail(testFailMessage);
		}
		catch (SAXException e) {
			// Exception normalement lev�e en cas de mauvais format
		}
	}

	private void importTestFile() throws DbException, IOException, ParserConfigurationException, SAXException, ModelException {
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
		// Importation des donn�es
		ModelMgr.importFromXML(in);
		// Fermeture du flux
		in.close();
	}
	
}
