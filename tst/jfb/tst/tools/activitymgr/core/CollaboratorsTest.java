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
		collaborator.setIsActive(true);

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
		collaborator.setIsActive(true);
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
		collaborator2.setLogin("login1");
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

	public void testActiveCollaborator() throws DbException, ModelException {
		Collaborator collaborator = new Collaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");
		collaborator.setIsActive(true);
		collaborator = ModelMgr.createCollaborator(collaborator);
		
		// Récupération du collaborateur actif
		Collaborator[] collaborators = ModelMgr.getActiveCollaborators(Collaborator.ID_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(1, collaborators.length);
		assertEquals(collaborator.getId(), collaborators[0].getId());
		
		// Mise du collaborateur en non actif puis récupération
		// => la liste doit être vide
		collaborator.setIsActive(false);
		collaborator = ModelMgr.updateCollaborator(collaborator);
		collaborators = ModelMgr.getActiveCollaborators(Collaborator.ID_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(0, collaborators.length);

		// Suppression
		ModelMgr.removeCollaborator(collaborator);
	}

	public void testSortCollaborators() throws DbException, ModelException {
		Collaborator c0 = new Collaborator();
		c0.setFirstName("FN0");
		c0.setLastName("LN2");
		c0.setLogin("l0");
		c0.setIsActive(true);
		c0 = ModelMgr.createCollaborator(c0);
		
		Collaborator c1 = new Collaborator();
		c1.setFirstName("FN1");
		c1.setLastName("LN1");
		c1.setLogin("l1");
		c1.setIsActive(false);
		c1 = ModelMgr.createCollaborator(c1);

		Collaborator c2 = new Collaborator();
		c2.setFirstName("FN2");
		c2.setLastName("LN0");
		c2.setLogin("l2");
		c2.setIsActive(true);
		c2 = ModelMgr.createCollaborator(c2);

		// Tri par identifiant
		Collaborator[] collaborators = ModelMgr.getCollaborators(Collaborator.ID_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c0.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c2.getId(), collaborators[2].getId());
		
		// Tri par login
		collaborators = ModelMgr.getCollaborators(Collaborator.LOGIN_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c0.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c2.getId(), collaborators[2].getId());

		// Tri par prénom
		collaborators = ModelMgr.getCollaborators(Collaborator.FIRST_NAME_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c0.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c2.getId(), collaborators[2].getId());

		// Tri par nom
		collaborators = ModelMgr.getCollaborators(Collaborator.LAST_NAME_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c2.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c0.getId(), collaborators[2].getId());

		// Tri par flag is-active
		collaborators = ModelMgr.getCollaborators(Collaborator.IS_ACTIVE_FIELD_IDX, false);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c1.getId(), collaborators[2].getId());

		// Suppression
		ModelMgr.removeCollaborator(c0);
		ModelMgr.removeCollaborator(c1);
		ModelMgr.removeCollaborator(c2);
	}

}
