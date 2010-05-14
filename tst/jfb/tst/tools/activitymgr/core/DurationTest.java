package jfb.tst.tools.activitymgr.core;

import java.util.GregorianCalendar;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Duration;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

public class DurationTest extends AbstractModelTestCase {

	public void testGetList() throws DbException {
		Duration[] durations = ModelMgr.getDurations();
		Duration previousDuration = null;
		for (int i=0; i<durations.length; i++) {
			Duration duration = durations[i];
			long durationId = duration.getId();
			assertTrue("Durée nulle", durationId!=0);
			if (previousDuration!=null)
				assertTrue("Durations are not correctly sorted", durationId>previousDuration.getId());
			previousDuration = duration;
		}
	}

	public void testNullCreation() throws DbException {
		// Création de durée nulle
		try {
			Duration duration = new Duration();
			duration.setId(0);
			ModelMgr.createDuration(duration);
			fail("Manage to create a null duration");
		}
		catch (ModelException ignored) {
			// success!
		}
	}

	public void testConflictingCreation() throws DbException, ModelException {
		// Création d'une durée
		Duration newDuration = generateNewDuration();
		newDuration = ModelMgr.createDuration(newDuration);
		try {
			// Tentative de recréation
			ModelMgr.createDuration(newDuration);
			fail("" + newDuration + " is supposed to exist in database, so it musn't be possible to create it");
		}
		catch (ModelException ignored) {
			// success!
			// Suppression
			ModelMgr.removeDuration(newDuration);
		}
	}

	public void testRemove() throws DbException, ModelException {
		Duration duration = generateNewDuration();

		// Création
		duration = ModelMgr.createDuration(duration);
		assertTrue(ModelMgr.durationExists(duration));

		// Suppression
		ModelMgr.removeDuration(duration);
		assertFalse(ModelMgr.durationExists(duration));
	}

	public void testUpdateWithAnExistingDuration() throws DbException, ModelException {
		// Création
		Duration duration = generateNewDuration();
		duration = ModelMgr.createDuration(duration);
		assertTrue(ModelMgr.durationExists(duration));
		Duration duration2 = generateNewDuration();
		duration2 = ModelMgr.createDuration(duration2);
		assertTrue(ModelMgr.durationExists(duration2));

		// Modif de la durée en une durée existante
		try {
			ModelMgr.updateDuration(duration, duration2);
			fail("Manage to update a duration whith the value of an existing duration");
		}
		catch (ModelException expected) {
			// Success
		}
		
		// Suppression des données
		ModelMgr.removeDuration(duration);
		ModelMgr.removeDuration(duration2);
	}
	
	public void testUpdateWithAnUnusedDuration() throws DbException, ModelException {
		// Création
		Duration duration = generateNewDuration();
		duration = ModelMgr.createDuration(duration);
		assertTrue(ModelMgr.durationExists(duration));

		// Modif de la durée en une autre durée
		Duration oldDuration = duration;
		duration = generateNewDuration();
		ModelMgr.updateDuration(oldDuration, duration);

		// Suppression des données
		ModelMgr.removeDuration(duration);
	}
	
	public void testUpdateDurationUsedByAContribution() throws DbException, ModelException {
		// Création
		Duration duration = generateNewDuration();
		duration = ModelMgr.createDuration(duration);
		assertTrue(ModelMgr.durationExists(duration));

		// Création d'une contribution
		Collaborator collaborator = ModelMgr.createNewCollaborator();
		Task task = ModelMgr.createNewTask(null);
		Contribution ctb = new Contribution();
		ctb.setContributorId(collaborator.getId());
		ctb.setTaskId(task.getId());
		ctb.setDate(new GregorianCalendar());
		ctb.setDurationId(duration.getId());
		ctb = ModelMgr.createContribution(ctb);

		// Tentative de modif d'une durée utilisée
		try {
			Duration duration2 = generateNewDuration();
			duration2 = ModelMgr.updateDuration(duration, duration2);
			fail("Manage to update a duration used by a contribution");
		}
		catch (ModelException expected) {
			// Success
		}
		
		// Suppression des données
		ModelMgr.removeContribution(ctb);
		ModelMgr.removeTask(task);
		ModelMgr.removeCollaborator(collaborator);
		ModelMgr.removeDuration(duration);
	}
	
	private Duration generateNewDuration() throws DbException {
		Duration duration = new Duration();
		duration.setId(1);
		// Recherche d'une durée inexistante en base
		while (ModelMgr.durationExists(duration)) {
			duration.setId(duration.getId() + 1);
		}
		return duration;
	}

}
