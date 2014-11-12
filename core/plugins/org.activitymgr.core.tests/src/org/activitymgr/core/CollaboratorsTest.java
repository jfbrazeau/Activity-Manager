package org.activitymgr.core;

import org.activitymgr.core.beans.Collaborator;

public class CollaboratorsTest extends AbstractModelTestCase {

	public void testGetList() throws DAOException {
		getModelMgr().getCollaborators();
	}

	public void testCreateAnRemove() throws ModelException, DAOException {
		Collaborator collaborator = new Collaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");
		collaborator.setIsActive(true);

		// Création
		long clbId = getModelMgr().createCollaborator(collaborator).getId();
		assertTrue(clbId>0);

		// Récupération du collaborateur
		Collaborator _collaborator = getModelMgr().getCollaborator(clbId);
		assertNotNull(_collaborator);
		assertEquals(_collaborator.getFirstName(), collaborator.getFirstName());
		assertEquals(_collaborator.getLastName(), collaborator.getLastName());
		assertEquals(_collaborator.getLogin(), collaborator.getLogin());
		assertNotNull(_collaborator);

		// Suppression du collaborateur
		getModelMgr().removeCollaborator(_collaborator);
		_collaborator = getModelMgr().getCollaborator(clbId);
		assertNull(_collaborator);
	}

	public void testUniqueLogin() throws DAOException, ModelException {
		Collaborator collaborator = new Collaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");
		collaborator.setIsActive(true);
		collaborator = getModelMgr().createCollaborator(collaborator);
		
		Collaborator collaborator2 = new Collaborator();
		collaborator2.setFirstName("First name2");
		collaborator2.setLastName("Last name2");
		collaborator2.setLogin(collaborator.getLogin());
		// Tentative de création avec le même login => doit échouer
		try {
			collaborator2 = getModelMgr().createCollaborator(collaborator2);
			fail("A collaborator with the same login of another collaborator must not be created");
		}
		catch (ModelException ignored) {}

		// Création du collaborateur avec un autre login => doit marcher
		collaborator2.setLogin("login1");
		collaborator2 = getModelMgr().createCollaborator(collaborator2);
		try {
			// Tentative d'update avec le même login
			collaborator2.setLogin(collaborator.getLogin());
			collaborator2 = getModelMgr().updateCollaborator(collaborator2);
			fail("A collaborator with the same login of another collaborator must not be updated");
		}
		catch (ModelException ignored) {}

		// Suppression
		getModelMgr().removeCollaborator(collaborator);
		getModelMgr().removeCollaborator(collaborator2);
	}

	public void testActiveCollaborator() throws DAOException, ModelException {
		Collaborator collaborator = new Collaborator();
		collaborator.setFirstName("First name");
		collaborator.setLastName("Last name");
		collaborator.setLogin("login");
		collaborator.setIsActive(true);
		collaborator = getModelMgr().createCollaborator(collaborator);
		
		// Récupération du collaborateur actif
		Collaborator[] collaborators = getModelMgr().getActiveCollaborators(Collaborator.ID_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(1, collaborators.length);
		assertEquals(collaborator.getId(), collaborators[0].getId());
		
		// Mise du collaborateur en non actif puis récupération
		// => la liste doit être vide
		collaborator.setIsActive(false);
		collaborator = getModelMgr().updateCollaborator(collaborator);
		collaborators = getModelMgr().getActiveCollaborators(Collaborator.ID_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(0, collaborators.length);

		// Suppression
		getModelMgr().removeCollaborator(collaborator);
	}

	public void testSortCollaborators() throws DAOException, ModelException {
		Collaborator c0 = new Collaborator();
		c0.setFirstName("FN0");
		c0.setLastName("LN2");
		c0.setLogin("l0");
		c0.setIsActive(true);
		c0 = getModelMgr().createCollaborator(c0);
		
		Collaborator c1 = new Collaborator();
		c1.setFirstName("FN1");
		c1.setLastName("LN1");
		c1.setLogin("l1");
		c1.setIsActive(false);
		c1 = getModelMgr().createCollaborator(c1);

		Collaborator c2 = new Collaborator();
		c2.setFirstName("FN2");
		c2.setLastName("LN0");
		c2.setLogin("l2");
		c2.setIsActive(true);
		c2 = getModelMgr().createCollaborator(c2);

		// Tri par identifiant
		Collaborator[] collaborators = getModelMgr().getCollaborators(Collaborator.ID_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c0.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c2.getId(), collaborators[2].getId());
		
		// Tri par login
		collaborators = getModelMgr().getCollaborators(Collaborator.LOGIN_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c0.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c2.getId(), collaborators[2].getId());

		// Tri par prénom
		collaborators = getModelMgr().getCollaborators(Collaborator.FIRST_NAME_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c0.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c2.getId(), collaborators[2].getId());

		// Tri par nom
		collaborators = getModelMgr().getCollaborators(Collaborator.LAST_NAME_FIELD_IDX, true);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c2.getId(), collaborators[0].getId());
		assertEquals(c1.getId(), collaborators[1].getId());
		assertEquals(c0.getId(), collaborators[2].getId());

		// Tri par flag is-active
		collaborators = getModelMgr().getCollaborators(Collaborator.IS_ACTIVE_FIELD_IDX, false);
		assertNotNull(collaborators);
		assertEquals(3, collaborators.length);
		assertEquals(c1.getId(), collaborators[2].getId());

		// Suppression
		getModelMgr().removeCollaborator(c0);
		getModelMgr().removeCollaborator(c1);
		getModelMgr().removeCollaborator(c2);
	}

}
