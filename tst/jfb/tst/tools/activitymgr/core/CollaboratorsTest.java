package jfb.tst.tools.activitymgr.core;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

public class CollaboratorsTest extends AbstractModelTestCase {

	public void testGetList() throws DbException {
		ModelMgr.getCollaborators();
	}

	public void testCreateAnRemove() throws ModelException, DbException {
		Collaborator collaborator = new Collaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");

		// Création
		long clbId = ModelMgr.createCollaborator(collaborator).getId();
		assertTrue(clbId>0);

		// Récupération du collaborateur
		Collaborator _collaborator = ModelMgr.getCollaborator(clbId);
		assertNotNull(_collaborator);
		assertEquals(_collaborator.getFirstName(), collaborator.getFirstName());
		assertEquals(_collaborator.getLastName(), collaborator.getLastName());
		assertEquals(_collaborator.getLogin(), collaborator.getLogin());
		assertNotNull(_collaborator);

		// Suppression du collaborateur
		ModelMgr.removeCollaborator(_collaborator);
		_collaborator = ModelMgr.getCollaborator(clbId);
		assertNull(_collaborator);
	}

	public void testUniqueLogin() throws DbException, ModelException {
		Collaborator collaborator = new Collaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");
		collaborator = ModelMgr.createCollaborator(collaborator);
		
		Collaborator collaborator2 = new Collaborator();
		collaborator2.setFirstName("First name2");
		collaborator2.setLastName("Last name2");
		collaborator2.setLogin(collaborator.getLogin());
		// Tentative de création avec le même login => doit échouer
		try {
			collaborator2 = ModelMgr.createCollaborator(collaborator2);
			fail("A collaborator with the same login of another collaborator must not be created");
		}
		catch (ModelException ignored) {}

		// Création du collaborateur avec un autre login => doit marcher
		collaborator2.setLogin("otherLogin");
		collaborator2 = ModelMgr.createCollaborator(collaborator2);
		try {
			// Tentative d'update avec le même login
			collaborator2.setLogin(collaborator.getLogin());
			collaborator2 = ModelMgr.updateCollaborator(collaborator2);
			fail("A collaborator with the same login of another collaborator must not be updated");
		}
		catch (ModelException ignored) {}

		// Suppression
		ModelMgr.removeCollaborator(collaborator);
		ModelMgr.removeCollaborator(collaborator2);
	}
	
}
