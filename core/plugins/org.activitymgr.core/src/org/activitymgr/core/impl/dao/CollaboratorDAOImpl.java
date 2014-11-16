package org.activitymgr.core.impl.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import org.activitymgr.core.beans.Collaborator;
import org.activitymgr.core.beans.Task;
import org.activitymgr.core.dao.DAOException;
import org.activitymgr.core.dao.ICollaboratorDAO;
import org.activitymgr.core.util.Strings;
import org.apache.log4j.Logger;

public class CollaboratorDAOImpl extends AbstractORMDAOImpl<Collaborator> implements
		ICollaboratorDAO {

	/** Logger */
	private static Logger log = Logger.getLogger(CollaboratorDAOImpl.class);

	/* (non-Javadoc)
	 * @see org.activitymgr.core.IDbMgr#getContributors(org.activitymgr.core.beans.Task, java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public Collaborator[] getContributors(Task task, Calendar fromDate,
			Calendar toDate) throws DAOException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Préparation de la requête
			StringBuffer request = new StringBuffer();
			request.append("select distinct (ctb_contributor), clb_login, clb_first_name, clb_last_name, clb_is_active from CONTRIBUTION, COLLABORATOR");
			if (task != null) {
				request.append(", TASK");
			}
			request.append("  where ctb_contributor=clb_id");
			pStmt = buildIntervalRequest(request, null, task, fromDate,
					toDate, false, "clb_login");

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList<Collaborator> list = new ArrayList<Collaborator>();
			while (rs.next()) {
				list.add(rsToCollaborator(rs));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			// FIXME
			return (Collaborator[]) list.toArray(new Collaborator[list.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_COLLABORATOR_FAILURE"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

	/**
	 * Convertit le résultat d'une requête en collaborateur.
	 * 
	 * @param rs
	 *            le result set.
	 * @return le collaborateur.
	 * @throws SQLException
	 *             levé en cas de problème SQL.
	 */
	@Deprecated
	private Collaborator rsToCollaborator(ResultSet rs) throws SQLException {
		Collaborator collaborator = new Collaborator();
		collaborator.setId(rs.getLong(1));
		collaborator.setLogin(rs.getString(2));
		collaborator.setFirstName(rs.getString(3));
		collaborator.setLastName(rs.getString(4));
		collaborator.setIsActive(rs.getBoolean(5));
		return (Collaborator) collaborator;
	}

}