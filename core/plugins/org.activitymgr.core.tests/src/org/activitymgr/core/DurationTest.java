package org.activitymgr.core;

import java.util.GregorianCalendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Contribution;
import org.activitymgr.core.beans.Duration;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.dao.DAOException;

public class DurationTest extends AbstractModelTestCase {

	public void testGetList() throws DAOException, ModelException {
		try {
			createSampleDurations();
			Duration[] durations = getModelMgr().getDurations();
			Duration previousDuration = null;
			for (int i = 0; i < durations.length; i++) {
				Duration duration = durations[i];
				long durationId = duration.getId();
				assertTrue("Durée nulle", durationId != 0);
				if (previousDuration != null)
					assertTrue("Durations are not correctly sorted",
							durationId > previousDuration.getId());
				previousDuration = duration;
			}
		} finally {
			removeSampleDurations();
		}
	}

	public void testNullCreation() throws DAOException {
		// Création de durée nulle
		try {
			Duration duration = getFactory().newDuration();
			duration.setId(0);
			getModelMgr().createDuration(duration);
			fail("Manage to create a null duration");
		} catch (ModelException ignored) {
			// success!
		}
	}

	public void testConflictingCreation() throws DAOException, ModelException {
		// Création d'une durée
		Duration newDuration = generateNewDuration();
		newDuration = getModelMgr().createDuration(newDuration);
		try {
			// Tentative de recréation
			getModelMgr().createDuration(newDuration);
			fail(""
					+ newDuration
					+ " is supposed to exist in database, so it musn't be possible to create it");
		} catch (ModelException ignored) {
			// success!
			// Suppression
			getModelMgr().removeDuration(newDuration);
		}
	}

	public void testRemove() throws DAOException, ModelException {
		Duration duration = generateNewDuration();

		// Création
		duration = getModelMgr().createDuration(duration);
		assertTrue(getModelMgr().durationExists(duration));

		// Suppression
		getModelMgr().removeDuration(duration);
		assertFalse(getModelMgr().durationExists(duration));
	}

	public void testUpdateWithAnExistingDuration() throws DAOException,
			ModelException {
		// Création
		Duration duration = generateNewDuration();
		duration = getModelMgr().createDuration(duration);
		assertTrue(getModelMgr().durationExists(duration));
		Duration duration2 = generateNewDuration();
		duration2 = getModelMgr().createDuration(duration2);
		assertTrue(getModelMgr().durationExists(duration2));

		// Modif de la durée en une durée existante
		try {
			getModelMgr().updateDuration(duration, duration2);
			fail("Manage to update a duration whith the value of an existing duration");
		} catch (ModelException expected) {
			// Success
		}

		// Suppression des données
		getModelMgr().removeDuration(duration);
		getModelMgr().removeDuration(duration2);
	}

	public void testUpdateWithAnUnusedDuration() throws DAOException,
			ModelException {
		// Création
		Duration duration = generateNewDuration();
		duration = getModelMgr().createDuration(duration);
		assertTrue(getModelMgr().durationExists(duration));

		// Modif de la durée en une autre durée
		Duration oldDuration = duration;
		duration = generateNewDuration();
		getModelMgr().updateDuration(oldDuration, duration);

		// Suppression des données
		getModelMgr().removeDuration(duration);
	}

	public void testUpdateDurationUsedByAContribution() throws DAOException,
			ModelException {
		// Création
		Duration duration = generateNewDuration();
		duration = getModelMgr().createDuration(duration);
		assertTrue(getModelMgr().durationExists(duration));

		// Création d'une contribution
		Collaborator collaborator = getModelMgr().createNewCollaborator();
		Task task = getModelMgr().createNewTask(null);
		Contribution ctb = getFactory().newContribution();
		ctb.setContributorId(collaborator.getId());
		ctb.setTaskId(task.getId());
		ctb.setDate(new GregorianCalendar());
		ctb.setDurationId(duration.getId());
		ctb = getModelMgr().createContribution(ctb, false);

		// Tentative de modif d'une durée utilisée
		try {
			Duration duration2 = generateNewDuration();
			duration2 = getModelMgr().updateDuration(duration, duration2);
			fail("Manage to update a duration used by a contribution");
		} catch (ModelException expected) {
			// Success
		}

		// Suppression des données
		getModelMgr().removeContribution(ctb, false);
		getModelMgr().removeTask(task);
		getModelMgr().removeCollaborator(collaborator);
		getModelMgr().removeDuration(duration);
	}

	public void testSelectActiveDurations() throws DAOException,
			ModelException {
		try {
			createSampleDurations();
			// Retrieve initial durations
			Duration[] durations = getModelMgr().getDurations();
			assertEquals(3, durations.length);
			
			// Set non active
			durations[0].setIsActive(false);
			getModelMgr().updateDuration(durations[0]);
			
			// Check that only 2 durations remain active
			durations = getModelMgr().getActiveDurations();
			assertEquals(2, durations.length);
			
		} finally {
			removeSampleDurations();
		}
	}

	private Duration generateNewDuration() throws DAOException {
		Duration duration = getFactory().newDuration();
		duration.setId(1);
		// Recherche d'une durée inexistante en base
		while (getModelMgr().durationExists(duration)) {
			duration.setId(duration.getId() + 1);
		}
		return duration;
	}

	private void createSampleDurations() throws DAOException, ModelException {
		newDuration(25l);
		newDuration(50l);
		newDuration(75l);
	}

	private void newDuration(long duration) throws DAOException, ModelException {
		Duration d = getFactory().newDuration();
		d.setId(duration);
		getModelMgr().createDuration(d);
	}

	private void removeSampleDurations() throws DAOException, ModelException {
		Duration[] durations = getModelMgr().getDurations();
		for (Duration duration : durations) {
			getModelMgr().removeDuration(duration);
		}
	}

}
