package jfb.tst.tools.activitymgr.core;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tst.tools.activitymgr.AbstractModelTestCase;

public class DurationTest extends AbstractModelTestCase {

	public void testGetList() throws DbException {
		long[] durations = ModelMgr.getDurations();
		long previousDuration = -1;
		for (int i=0; i<durations.length; i++) {
			long duration = durations[i];
			assertTrue("Durée nulle", duration!=0);
			assertTrue("Durée non positive", duration>0);
			assertTrue("Durations are not correctly sorted", duration>previousDuration);
			previousDuration = duration;
		}
	}

	public void testNullCreation() throws DbException {
		// Création de durée nulle
		try {
			ModelMgr.createDuration(0);
			fail("Manage to create a null duration");
		}
		catch (ModelException ignored) {
			// success!
		}
	}

	public void testConflictingCreation() throws DbException {
		// Création de durée déjà existante
		try {
			ModelMgr.createDuration(100);
			fail("1.00 is supposed to exist in database, so it musn't be possible to create it");
		}
		catch (ModelException ignored) {
			// success!
		}
	}

	public void testNegativeCreation() throws DbException {
		// Création de durée négative
		try {
			ModelMgr.createDuration(-1);
			fail("Manage to create a negative duration");
		}
		catch (ModelException ignored) {
			// success!
		}
	}

	public void testRemove() throws DbException, ModelException {
		long duration = 1;
		// Recherche d'une durée inexistante en base
		while (ModelMgr.durationExists(duration)) {
			duration ++;
		}
		// Création
		ModelMgr.createDuration(duration);
		assertTrue(ModelMgr.durationExists(duration));

		// Suppression
		ModelMgr.removeDuration(duration);
		assertFalse(ModelMgr.durationExists(duration));
	}

}
