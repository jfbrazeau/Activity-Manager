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
			request.append("select distinct (ctb_contributor), ");
			request.append(getColumnNamesRequestFragment());
			request.append(" from CONTRIBUTION, COLLABORATOR");
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
				list.add(read(rs, 2));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return (Collaborator[]) list.toArray(new Collaborator[list.size()]);
		} catch (SQLException e) {
			log.info("Incident SQL", e); //$NON-NLS-1$
			throw new DAOException(
					Strings.getString("DbMgr.errors.TASK_SELECTION_BY_COLLABORATOR_FAILURE"), e); //$NON-NLS-1$
		} finally {
			lastAttemptToClose(pStmt);
		}
	}

}