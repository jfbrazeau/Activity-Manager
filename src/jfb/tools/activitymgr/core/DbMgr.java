/*
 * Copyright (c) 2004, Jean-François Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jfb.tools.activitymgr.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import jfb.tools.activitymgr.core.beans.Collaborator;
import jfb.tools.activitymgr.core.beans.Contribution;
import jfb.tools.activitymgr.core.beans.Task;
import jfb.tools.activitymgr.core.beans.TaskSums;
import jfb.tools.activitymgr.core.util.StringHelper;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

/**
 * Classe offrant les services de base de persistence de 
 * l'application.
 */
public class DbMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(DbMgr.class);

	/** Formatteur de date */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

	/** Datasource */
	private static BasicDataSource ds = null;
	
	/** Contexte de thread utilisé pour détecter les anomalies associées à la gestion de transaction */
	private static ThreadLocal threadLocal = new ThreadLocal();

	/**
	 * Initialise la connexion à la base de données.
	 * @param driverName le nom du driver JDBC.
	 * @param url l'URL de connexion au serveur.
	 * @param user l'identifiant de connexion/
	 * @param password le mot de passe de connexion.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void initDatabaseAccess(String driverName, String url, String user, String password) throws DbException {
		try {
			// Si la datasource existe on la ferme
			if (ds!=null) {
				closeDatabaseAccess();
			}
			// Fermeture de la datasource
			BasicDataSource newDs = new BasicDataSource();
			
			// Initialisation de la Datasource
			newDs = new BasicDataSource();
			log.info("Connecting to '" + url + "'");
			newDs.setDriverClassName(driverName);
			newDs.setUrl(url);
			newDs.setUsername(user);
			newDs.setPassword(password);
			newDs.setDefaultAutoCommit(false);

			// Tentative de récupération d'une connexion
			// pour détecter les problèmes de connexion
			Connection con = newDs.getConnection();
			con.close();
			
			// Sauvegarde de la référence
			ds = newDs;
		}
		catch (SQLException e) {
			log.info("SQL Exception", e);
			throw new DbException("Couldn't get a SQL Connection", e);
		}
	}
	
	/**
	 * Ferme la base de données.
	 * @throws DbException levé en cas d'incident technique d'accès à la BDD.
	 */
	protected static void closeDatabaseAccess() throws DbException {
		try {
			if (ds!=null){
				// Récupération de la connexion
				Connection con = ds.getConnection();
				
				// Cas d'une base HSQLDB embarquée
				if (isEmbeddedHSQLDB(con)) {
					// Extinction de la base de données
					con.createStatement().execute("shutdown");
				}

				// Fermeture de la datasource
				ds.close();
				ds = null;
			}
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la fermeture de la base de données", e);
		}
	}
	
	/**
	 * Permet de commencer une transaction.
	 * 
	 * <p>Une connexion à la base de données est établie. Celle ci
	 * doit être validée par la couche appelante par une invocation
	 * de <code>endTransaction</code>.</p>
	 * 
	 * @return le contexte de transaction.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static DbTransaction beginTransaction() throws DbException {
		try {
			// Est-on connecté à la BDD ?
			if (ds==null)
				throw new DbException("Database connection not established", null);
			// Obtention d'une connexion
			Connection con = ds.getConnection();
			if (threadLocal.get()!=null)
				throw new Error("Conflicting transaction");
			threadLocal.set(con);
			//log.debug("Active : " + ds.getNumActive() + ", Idle : " + ds.getNumIdle() + ", Connexion : " + con);
			return new DbTransaction(con);
		}
		catch (SQLException e) {
			log.info("SQL Exception", e);
			throw new DbException("Couldn't get a SQL Connection", e);
		}
	}

	/**
	 * Valide une transactrion.
	 * @param tx contexte de transaction.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void commitTransaction(DbTransaction tx) throws DbException {
		try { tx.getConnection().commit();	}
		catch (SQLException e ) {
			log.info("Incident SQL", e);
			throw new DbException("Echec du commit", e);
		}
	}
	
	/**
	 * Vérifie si les tables existent dans le modèle.
	 * @param tx le contexte de transaction.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static boolean tablesExist(DbTransaction tx) throws DbException {
		boolean tablesExist = true;
		tablesExist &= tableExists(tx, "COLLABORATOR");
		tablesExist &= tableExists(tx, "CONTRIBUTION");
		tablesExist &= tableExists(tx, "DURATION");
		tablesExist &= tableExists(tx, "TASK");
		return tablesExist;
	}
	
	/**
	 * Vérifie si une table existe dans le modèle.
	 * @param tx le contexte de transaction.
	 * @param tableName le nom de la table.
	 * @return un booléen indiquant si la table spécifiée existe dans le modèle.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	private static boolean tableExists(DbTransaction tx, String tableName) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Recherche de la table
			ResultSet rs = con.getMetaData().getTables(null, null, tableName, new String[] { "TABLE" } );

			// Récupération du résultat
			boolean exists = rs.next();

			// Retour du résultat
			return exists;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors du test d'existance de la table '" + tableName + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}
	
	/**
	 * Crée les tables du modèle de données.
	 * @param tx contexte de transaction.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void createTables(DbTransaction tx) throws DbException {
		Statement stmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Lecture du fichier SQL de création de la BDD
			String batchName = "sql/" + (isHSQLDB(con) ? "hsqldb.sql" : "mysqldb.sql");
			InputStream in = DbMgr.class.getResourceAsStream(batchName);
			String batchContent = null;
			try { batchContent = StringHelper.fromInputStream(in); }
			catch (IOException e) {
				log.info("I/O error while loading table creation SQL script.", e);
				throw new DbException("I/O error while loading table creation SQL script.", null);
			}

			// Découpage et exécution du batch
			stmt = con.createStatement();
			LineNumberReader lnr = new LineNumberReader(new StringReader(batchContent));
			// TODO Externaliser le découpage du script SQL
			StringBuffer buf = new StringBuffer();
			boolean proceed = true;
			do {
				String line = null;
				// On ne lit dans le flux que si la ligne courante n'est pas
				// encore totalement traitée
				if (line==null) {
					try { line = lnr.readLine(); }
					catch (IOException e) {
						log.info("Unexpected I/O error while reading memory stream!", e);
						throw new DbException("Unexpected I/O error while reading memory stream!", null);
					}
					log.debug("Line read : '" + line + "'");
				}
				// Si le flux est vide, on sort de la boucle
				if (line==null) {
					proceed = false;
				}
				// Sinon on traite la ligne
				else {
					line = line.trim();
					// Si la ligne est un commentaire on l'ignore
					if (line.startsWith("--")) {
						line = null;
					}
					else {
						// Sinon on regarde si la ligne possède
						// un point virgule
						int idx = line.indexOf(';');
						// Si c'est le cas, on découpe la chaîne et on 
						// exécute la requête
						if (idx>=0) {
							buf.append(line.subSequence(0, idx));
							line = line.substring(idx);
							String sql = buf.toString();
							buf.setLength(0);
							log.debug(" - sql='" + sql + "'"); 
							if (!"".equals(sql)) 
								stmt.executeUpdate(sql);
						}
						// sinon on ajoute la ligne au buffer de requeête
						else {
							buf.append(line);
							buf.append('\n');
						}
					}
				}
				
			}
			while (proceed);
			
			// Test de l'existence des tables
			if (!tablesExist(tx))
				throw new DbException("Database table creation failure", null);

			// Fermeture du statement
			stmt.close();
			stmt = null;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Database table creation failure", e);
		}
		finally {
			if (stmt!=null) try { stmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Crée un collaborateur.
	 * 
	 * @param tx contexte de transaction.
	 * @param newCollaborator le collaborateur à créer.
	 * @return le collaborateur après création.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Collaborator createCollaborator(DbTransaction tx, Collaborator newCollaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("insert into collaborator (clb_login, clb_first_name, clb_last_name) values (?, ?, ?)");
			pStmt.setString(1, newCollaborator.getLogin());
			pStmt.setString(2, newCollaborator.getFirstName());
			pStmt.setString(3, newCollaborator.getLastName());
			pStmt.executeUpdate();

			// Récupération de l'identifiant généré
			long generatedId = getGeneratedId(pStmt);
			log.debug("Generated id=" + generatedId);
			newCollaborator.setId(generatedId);

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return newCollaborator;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la création du collaborateur '" + newCollaborator.getLogin() + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Crée une contribution.
	 * 
	 * @param tx contexte de transaction.
	 * @param newContribution la nouvelle contribution.
	 * @return la contribution après création.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Contribution createContribution(DbTransaction tx, Contribution newContribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("insert into contribution (ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration) values (?, ?, ?, ?, ?, ?)");
			pStmt.setInt   (1, newContribution.getYear());
			pStmt.setInt   (2, newContribution.getMonth());
			pStmt.setInt   (3, newContribution.getDay());
			pStmt.setLong  (4, newContribution.getContributorId());
			pStmt.setLong  (5, newContribution.getTaskId());
			pStmt.setLong  (6, newContribution.getDuration());
			pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return newContribution;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la création d'une contribution", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Crée une contribution.
	 * 
	 * @param tx contexte de transaction.
	 * @param newDuration la nouvelle durée.
	 * @return la durée après création.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static long createDuration(DbTransaction tx, long newDuration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("insert into duration (dur_id) values (?)");
			pStmt.setLong (1, newDuration);
			pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return newDuration;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la création de la durée : '" + newDuration + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}
	
	/**
	 * Crée une tache.
	 * 
	 * <p>La tache parent peut être nulle pour indiquer que la nouvelle tache
	 * est une tache racine.</p>
	 * 
	 * @param tx le contexte de transaction.
	 * @param parentTask la tache parent accueillant la nouvelle tache.
	 * @param newTask la nouvelle tache.
	 * @return la tache après création.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task createTask(DbTransaction tx, Task parentTask, Task newTask) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Mise à jour du chemin de la tâche
			String parentPath = parentTask==null ? "" : parentTask.getFullPath();
			newTask.setPath(parentPath);

			// Génération du numéro de la tâche
			byte taskNumber = newTaskNumber(tx, parentPath);
			newTask.setNumber(taskNumber);
			
			// Préparation de la requête
			pStmt = con.prepareStatement("insert into task (tsk_path, tsk_number, tsk_code, tsk_name, tsk_budget, tsk_initial_cons, tsk_todo) values (?, ?, ?, ?, ?, ?, ?)");
			pStmt.setString(1, newTask.getPath());
			pStmt.setByte  (2, newTask.getNumber());
			pStmt.setString(3, newTask.getCode());
			pStmt.setString(4, newTask.getName());
			pStmt.setLong  (5, newTask.getBudget());
			pStmt.setLong  (6, newTask.getInitiallyConsumed());
			pStmt.setLong  (7, newTask.getTodo());
			pStmt.executeUpdate();

			// Récupération de l'identifiant généré
			long generatedId = getGeneratedId(pStmt);
			log.debug("Generated id=" + generatedId);
			newTask.setId(generatedId);

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return newTask;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la création de la tache '" + newTask.getName() + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}
	
	/**
	 * Vérifie si la durée est utilisée en base.
	 * @param tx le contexte de transaction.
	 * @param duration la durée à vérifier.
	 * @return un booléen indiquant si la durée est utilisée.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static boolean durationIsUsed(DbTransaction tx, long duration) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select count(*) from contribution where ctb_duration=?");
			pStmt.setLong  (1, duration);
	
			// Exécution de la requête
			rs = pStmt.executeQuery();
			
			// Préparation du résultat
			if (!rs.next())
				throw new DbException("Nothing returned by the query", null);
			boolean durationIsUsed = rs.getInt(1)>0;

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return durationIsUsed;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la vérification de l'utilisation de la durée '" + duration + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Ferme une transactrion.
	 * @param tx le contexte de transaction.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void endTransaction(DbTransaction tx) throws DbException {
		try { tx.getConnection().close();	}
		catch (SQLException e ) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la cloture de la connexion", e);
		}
		threadLocal.set(null);
	}
	
	/**
	 * @param tx le contexte de transaction.
	 * @param collaboratorId l'identifiant du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant est spécifié.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Collaborator getCollaborator(DbTransaction tx, long collaboratorId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select clb_login, clb_first_name, clb_last_name from collaborator where clb_id=?");
			pStmt.setLong  (1, collaboratorId);
	
			// Exécution de la requête
			rs = pStmt.executeQuery();
			
			// Préparation du résultat
			Collaborator collaborator = null;
			if (rs.next()) {
				collaborator = new Collaborator();
				collaborator.setId(collaboratorId);
				collaborator.setLogin(rs.getString(1));
				collaborator.setFirstName(rs.getString(2));
				collaborator.setLastName(rs.getString(3));
			}

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return collaborator;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération du collaborateur d'identifiant '" + collaboratorId + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param login l'identifiant de connexion du collaborateur recherché.
	 * @return le collaborateur dont l'identifiant de connexion est spécifié.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Collaborator getCollaborator(DbTransaction tx, String login) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select clb_id from collaborator where clb_login=?");
			pStmt.setString(1, login);
	
			// Exécution de la requête
			rs = pStmt.executeQuery();
			
			// Préparation du résultat
			Collaborator collaborator = null;
			if (rs.next()) {
				long collaboratorId = rs.getLong(1);
				collaborator = getCollaborator(tx, collaboratorId);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return collaborator;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération ddu collaborateur de login '" + login + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @return la liste des collaborateurs.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Collaborator[] getCollaborators(DbTransaction tx) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select clb_id from collaborator");

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList list = new ArrayList();
			while (rs.next()) {
				long collaboratorId = rs.getLong(1);
				Collaborator collaborator = getCollaborator(tx, collaboratorId);
				list.add(collaborator);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			log.debug("  => found " + list.size() + " entrie(s)");
			return (Collaborator[]) list.toArray(new Collaborator[list.size()]);
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des collaborateurs'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}
	
	/**
	 * @param tx le contexte de transaction.
	 * @param contributor le collaborateur associé aux contributions.
	 * @param task la tache associée aux contributions.
	 * @param fromDate la date de départ.
	 * @param toDate la date de fin.
	 * @return la liste des contributions associées aux paramètres spécifiés.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Contribution[] getContributions(DbTransaction tx, Collaborator contributor, Task task, Calendar fromDate, Calendar toDate) throws DbException {
		log.debug("getContributions(" + contributor + ", " + task + ", " + sdf.format(fromDate.getTime()) + ", " + sdf.format(toDate.getTime()) + ")");
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration from contribution where ctb_contributor=? and ctb_task=? and ctb_year*10000 + ( ctb_month*100 + ctb_day ) between ? and ?");
			pStmt.setLong  (1, contributor.getId());
			pStmt.setLong  (2, task.getId());
			pStmt.setString(3, sdf.format(fromDate.getTime()));
			pStmt.setString(4, sdf.format(toDate.getTime()));

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Extraction du résultat
			Contribution[] result = extractContributions(rs);
			
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return result;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des contributions", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Extrait les contributions du resultat de la requête SQL.
	 * @param rs le résultat de la requête SQL.
	 * @return les contributions extraites.
	 * @throws SQLException levé en cas d'incident avec la base de données.
	 */
	private static Contribution[] extractContributions(ResultSet rs) throws SQLException {
		// Recherche des sous-taches
		ArrayList list = new ArrayList();
		while (rs.next()) {
			// Préparation du résultat
			Contribution contribution = new Contribution();
			contribution.setYear(rs.getInt(1));
			contribution.setMonth(rs.getInt(2));
			contribution.setDay(rs.getInt(3));
			contribution.setContributorId(rs.getInt(4));
			contribution.setTaskId(rs.getInt(5));
			contribution.setDuration(rs.getLong(6));
			list.add(contribution);
		}
		log.debug("  => found " + list.size() + " entrie(s)");
		return (Contribution[]) list.toArray(new Contribution[list.size()]);
	}
		
	/**
	 * Retourne les contributions associées aux paramètres spécifiés.
	 * 
	 * <p>Tous les paramètres sont facultatifs. Chaque paramètre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramètre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.</p>
	 * 
	 * <p>La spécification des paramètres répond aux mêmes règles que pour la
	 * méthode <code>getContributionsSum</code>.</p>
	 * 
	 * @param tx le contexte de transaction.
	 * @param task la tâche associée aux contributions (facultative).
	 * @param contributor le collaborateur associé aux contributions (facultatif).
	 * @param year l'année (facultative).
	 * @param month le mois (facultatif).
	 * @param day le jour (facultatif).
	 * @return les contributions.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 * 
	 * @see jfb.tools.activitymgr.core.DbMgr#getContributionsSum(DbTransaction, Task, Collaborator, Integer, Integer, Integer)
	 */
	protected static Contribution[] getContributions(DbTransaction tx, Task task, Collaborator contributor, Integer year, Integer month, Integer day) throws DbException {
		log.debug("getContributions(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")");
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			StringBuffer baseRequest = new StringBuffer("select ctb_year, ctb_month, ctb_day, ctb_contributor, ctb_task, ctb_duration from contribution, task where ctb_task=tsk_id");
			String orderByClause = " order by ctb_year, ctb_month, ctb_day, tsk_path, tsk_number, ctb_contributor, ctb_duration";
			// Cas ou la tache n'est pas spécifiée
			if (task==null) {
				// Préparation de la requête
				completeContributionRequest(
						baseRequest,
						contributor,
						year,
						month,
						day
					);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = con.prepareStatement(request);
				completeContributionReqParams(pStmt, 1, contributor, year, month, day);
			}
			// Si la tache n'admet pas de sous-taches, le cumul de 
			// budget, de consommé initial, de reste à faire sont
			// égaux à ceux de la tache
			else if (task.getSubTasksCount()==0) {
				// Préparation de la requête
				baseRequest.append(" and tsk_id=?");
				completeContributionRequest(
						baseRequest,
						contributor,
						year,
						month,
						day
					);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = con.prepareStatement(request);
				pStmt.setLong(1, task.getId());
				log.debug(" taskId=" + task.getId());
				completeContributionReqParams(pStmt, 2, contributor, year, month, day);
			}
			// Sinon, il faut calculer
			else {
				// Paramètre pour la clause 'LIKE'
				String pathLike = task.getFullPath() + "%";
					
				// Préparation de la requête
				baseRequest.append(" and tsk_path like ?");
				completeContributionRequest(
						baseRequest,
						contributor,
						year,
						month,
						day
					);
				baseRequest.append(orderByClause);
				String request = baseRequest.toString();
				pStmt = con.prepareStatement(request);
				pStmt.setString(1, pathLike);
				completeContributionReqParams(pStmt, 2, contributor, year, month, day);
			}		

			// Exécution de la requête
			log.debug("Request : " + baseRequest);
			rs = pStmt.executeQuery();

			// Extraction du résultat
			Contribution[] result = extractContributions(rs);
			
			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return result;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des contributions", e);
		}
		finally {
			try { if (pStmt!=null) pStmt.close(); } catch (Throwable ignored) { }
		}
	}


	/**
	 * Calcule le nombre des contributions associée aux paramètres spécifiés.
	 * 
	 * <p>Tous les paramètres sont facultatifs. Chaque paramètre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramètre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.</p>
	 * 
	 * <p>En spécifiant la tache X, on connaîtra la somme des contribution pour
	 * la taches X. En ne spécifiant pas de tache, la somme sera effectuée quelque
	 * soit les tâches.</p>
	 * 
	 * @param tx le contexte de transaction.
	 * @param task la tâche associée aux contributions (facultative).
	 * @param contributor le collaborateur associé aux contributions (facultatif).
	 * @param year l'année (facultative).
	 * @param month le mois (facultatif).
	 * @param day le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static long getContributionsNb(DbTransaction tx, Task task, Collaborator contributor, Integer year, Integer month, Integer day) throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")");
		return getContributionsAggregation(tx, "count(ctb_duration)", task, contributor, year, month, day);
	}

	/**
	 * Calcule le cumuls des consommations associees aux contributions pour
	 * les paramètres spécifiés.
	 * 
	 * <p>Tous les paramètres sont facultatifs. Chaque paramètre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramètre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.</p>
	 * 
	 * <p>En spécifiant la tache X, on connaîtra la somme des contribution pour
	 * la taches X. En ne spécifiant pas de tache, la somme sera effectuée quelque
	 * soit les tâches.</p>
	 * 
	 * @param tx le contexte de transaction.
	 * @param task la tâche associée aux contributions (facultative).
	 * @param contributor le collaborateur associé aux contributions (facultatif).
	 * @param year l'année (facultative).
	 * @param month le mois (facultatif).
	 * @param day le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static long getContributionsSum(DbTransaction tx, Task task, Collaborator contributor, Integer year, Integer month, Integer day) throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")");
		return getContributionsAggregation(tx, "sum(ctb_duration)", task, contributor, year, month, day);
	}

	/**
	 * Calcule une aggregation associee aux contributions pour les paramètres
	 * spécifiés.
	 * 
	 * <p>Tous les paramètres sont facultatifs. Chaque paramètre spécifié agît
	 * comme un filtre sur le résultat. A l'inverse, l'omission d'un paramètre
	 * provoque l'inclusion de toutes les contributions, quelque soit leurs
	 * valeurs pour l'attribut considéré.</p>
	 * 
	 * <p>En spécifiant la tache X, on connaîtra la somme des contribution pour
	 * la taches X. En ne spécifiant pas de tache, la somme sera effectuée quelque
	 * soit les tâches.</p>
	 * 
	 * @param tx le contexte de transaction.
	 * @param aggregation la chaîne représentant l'aggrégation (ex: <code>sum(ctb_contribution)</code>).
	 * @param task la tâche associée aux contributions (facultative).
	 * @param contributor le collaborateur associé aux contributions (facultatif).
	 * @param year l'année (facultative).
	 * @param month le mois (facultatif).
	 * @param day le jour (facultatif).
	 * @return la seomme des contributions.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	private static long getContributionsAggregation(DbTransaction tx, String aggregation, Task task, Collaborator contributor, Integer year, Integer month, Integer day) throws DbException {
		log.debug("getContributionsSum(" + task + ", " + contributor + ", " + year + ", " + month + ", " + day + ")");
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			StringBuffer baseRequest = new StringBuffer("select ")
				.append(aggregation)
				.append(" from contribution, task where ctb_task=tsk_id");
			// Cas ou la tache n'est pas spécifiée
			if (task==null) {
				// Préparation de la requête
				completeContributionRequest(
						baseRequest,
						contributor,
						year,
						month,
						day
					);
				String request = baseRequest.toString();
				pStmt = con.prepareStatement(request);
				completeContributionReqParams(pStmt, 1, contributor, year, month, day);
			}
			// Si la tache n'admet pas de sous-taches, le cumul de 
			// budget, de consommé initial, de reste à faire sont
			// égaux à ceux de la tache
			else if (task.getSubTasksCount()==0) {
				// Préparation de la requête
				baseRequest.append(" and tsk_id=?");
				completeContributionRequest(
						baseRequest,
						contributor,
						year,
						month,
						day
					);
				String request = baseRequest.toString();
				pStmt = con.prepareStatement(request);
				pStmt.setLong(1, task.getId());
				log.debug(" taskId=" + task.getId());
				completeContributionReqParams(pStmt, 2, contributor, year, month, day);
			}
			// Sinon, il faut calculer
			else {
				// Paramètre pour la clause 'LIKE'
				String pathLike = task.getFullPath() + "%";
					
				// Préparation de la requête
				baseRequest.append(" and tsk_path like ?");
				completeContributionRequest(
						baseRequest,
						contributor,
						year,
						month,
						day
					);
				String request = baseRequest.toString();
				pStmt = con.prepareStatement(request);
				pStmt.setString(1, pathLike);
				completeContributionReqParams(pStmt, 2, contributor, year, month, day);
			}		

			// Exécution de la requête
			log.debug("Request : " + baseRequest);
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException("Nothing returned from this query", null);
			long agregation = rs.getLong(1);
			
			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			log.info("agregation=" + agregation);
			return agregation;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des cumuls", e);
		}
		finally {
			try { if (pStmt!=null) pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @return la liste des durées.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static long[] getDurations(DbTransaction tx) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select dur_id from duration order by dur_id asc");

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList list = new ArrayList();
			while (rs.next()) {
				long durationId = rs.getLong(1);
				list.add(new Long(durationId));
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			log.debug("  => found " + list.size() + " entrie(s)");
			long[] result = new long[list.size()];
			for (int i=0; i<result.length; i++)
				result[i] = ((Long) list.get(i)).longValue();
			return result;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des collaborateurs'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param task la tache dont on veut connaitre la tache parent.
	 * @return la tache parent d'une tache spécifiée.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task getParentTask(DbTransaction tx, Task task) throws DbException {
		Task parentTask = null;
		String parentTaskFullPath = task.getPath();
log.debug(parentTaskFullPath);
		// Si le chemin est vide, la tache parent est nulle (tache racine)
		if (parentTaskFullPath!=null && !"".equals(parentTaskFullPath)) {
			// Extraction du chemin et du numéro de la tache recherchée
			log.debug("Fullpath='" + parentTaskFullPath + "'");
			String path = parentTaskFullPath.substring(0, parentTaskFullPath.length()-2);
			byte number = StringHelper.toByte(parentTaskFullPath.substring(parentTaskFullPath.length()-2));
			log.debug(" => path=" + path);
			log.debug(" => number=" + number);
			
			// Recherche de la tache
			parentTask = getTask(tx, path, number);
		}
		// Retour du résultat
		return parentTask;
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param path le chemin dont on veut connaître les taches.
	 * @return la liste des taches associées à un chemin donné.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task[] getTasks(DbTransaction tx, String path) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
			
			// Préparation de la requête
			pStmt = con.prepareStatement("select tsk_id from task where tsk_path=? order by tsk_number");
			pStmt.setString(1, path);

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList list = new ArrayList();
			while (rs.next()) {
				long taskId = rs.getLong(1);
				Task task = getTask(tx, taskId);
				list.add(task);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			log.debug("  => found " + list.size() + " entrie(s)");
			return (Task[]) list.toArray(new Task[list.size()]);
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des sous taches de chemin '" + path + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param parentTask la tache parent dont on veut connaitre les sous-taches.
	 * @return la liste des sous-taches.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task[] getSubtasks(DbTransaction tx, Task parentTask) throws DbException {
		// Récupération du chemin à partir de la tache parent
		String fullpath = parentTask==null ? "" : parentTask.getFullPath();
		log.debug("Looking for tasks with path='" + fullpath + "'");
		return getTasks(tx, fullpath);
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param taskId l'identifiant de la tache recherchée.
	 * @return la tache dont l'identifiant est spécifié.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task getTask(DbTransaction tx, long taskId) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("select tsk_path, tsk_number, tsk_code, tsk_name, tsk_budget, tsk_initial_cons, tsk_todo from task where tsk_id=?");
			pStmt.setLong  (1, taskId);
	
			// Exécution de la requête
			rs = pStmt.executeQuery();
			
			// Préparation du résultat
			Task task = null;
			if (rs.next()) {
				task = new Task();
				task.setId(taskId);
				task.setPath(rs.getString(1));
				task.setNumber(rs.getByte(2));
				task.setCode(rs.getString(3));
				task.setName(rs.getString(4));
				task.setBudget(rs.getLong(5));
				task.setInitiallyConsumed(rs.getLong(6));
				task.setTodo(rs.getLong(7));
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Si la tache existe bien
			if (task!=null) {
				// Recherche du nombre de sous-taches
				String taskFullPath = task.getFullPath();
				pStmt = con.prepareStatement("select count(*) from task where tsk_path=?");
				pStmt.setString(1, taskFullPath);
		
				// Exécution de la requête
				rs = pStmt.executeQuery();
				if (rs.next()) {
					int subTasksCount = rs.getInt(1);
					task.setSubTasksCount(subTasksCount);
				}
				// Fermeture du ResultSet
				pStmt.close();
				pStmt = null;
			}
	
			// Retour du résultat
			return task;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération de la tache d'identifiant '" + taskId + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param taskPath le chemin de la tache recherchée.
	 * @param taskNumber le numéro de la tache recherchée.
	 * @return la tache dont le chemin et le numéro sont spécifiés.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task getTask(DbTransaction tx, String taskPath, byte taskNumber) throws DbException {
		log.debug("getTask(" + taskPath + ", " + taskNumber + ")");
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("select tsk_id from task where tsk_path=? and tsk_number=?");
			pStmt.setString(1, taskPath);
			pStmt.setByte  (2, taskNumber);

			// Exécution de la requête
			rs = pStmt.executeQuery();
			
			// Préparation du résultat
			Task task = null;
			if (rs.next()) {
				long taskId = rs.getLong(1);
				task = getTask(tx, taskId);
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			log.debug("task = " + task);
			return task;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération de la tache N° " + taskNumber + " du chemin '" + taskPath + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param taskPath le chemin de la tache recherchée.
	 * @param taskCode le code de la tache recherchée.
	 * @return la tache dont le code et la tache parent sont spécifiés.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task getTask(DbTransaction tx, String taskPath, String taskCode) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("select tsk_id from task where tsk_path=? and tsk_code=?");
			pStmt.setString(1, taskPath);
			pStmt.setString(2, taskCode);

			// Exécution de la requête
			rs = pStmt.executeQuery();
			
			// Préparation du résultat
			Task task = null;
			if (rs.next()) {
				long taskId = rs.getLong(1);
				task = getTask(tx, taskId);
			}
			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return task;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération de la tache de code '" + taskCode + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param collaborator le collaborateur.
	 * @param fromDate date de début.
	 * @param toDate date de fin.
	 * @return la liste de taches associées au collaborateur entre les 2 dates spécifiées.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task[] getTasks(DbTransaction tx, Collaborator collaborator, Calendar fromDate, Calendar toDate) throws DbException {
		log.debug("getTasks(" + collaborator + ", " + sdf.format(fromDate.getTime()) + ", " + sdf.format(toDate.getTime()) + ")");
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("select distinct ctb_task, tsk_path from contribution, task where ctb_task=tsk_id and ctb_contributor=? and ctb_year*10000 + ( ctb_month*100 + ctb_day ) between ? and ? order by tsk_path");
			pStmt.setLong  (1, collaborator.getId());
			pStmt.setString(2, sdf.format(fromDate.getTime()));
			pStmt.setString(3, sdf.format(toDate.getTime()));

			// Exécution de la requête
			rs = pStmt.executeQuery();

			// Recherche des sous-taches
			ArrayList list = new ArrayList();
			while (rs.next()) {
				long taskId = rs.getLong(1);
				Task task = getTask(tx, taskId);
				list.add(task);
			}

			// Fermeture du ResultSet
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			log.debug("  => found " + list.size() + " entrie(s)");
			return (Task[]) list.toArray(new Task[list.size()]);
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des taches associées à un collaborateur", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * @param tx le contexte de transaction.
	 * @param task la tâche pour laquelle on souhaite connaître les totaux.
	 * @return les totaux associés à une tache (consommé, etc.).
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static TaskSums getTaskSums(DbTransaction tx, Task task) throws DbException {
		// TODO Factoriser cette méthose avec getContributionsSum
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation du résultat
			TaskSums taskSums = new TaskSums();
			
			// Si la tache n'admet pas de sous-taches, le cumul de 
			// budget, de consommé initial, de reste à faire sont
			// égaux à ceux de la tache
			if (task!=null && task.getSubTasksCount()==0) {
				taskSums.setBudgetSum(task.getBudget());
				taskSums.setInitiallyConsumedSum(task.getInitiallyConsumed());
				taskSums.setTodoSum(task.getTodo());
	
				// Calcul du consommé
				pStmt = con.prepareStatement("select sum(ctb_duration), count(ctb_duration) from contribution, task where ctb_task=tsk_id and tsk_id=?");
				pStmt.setLong(1, task.getId());
				rs = pStmt.executeQuery();
				if (!rs.next())
					throw new DbException("Nothing returned from this query", null);
				taskSums.setConsumedSum(rs.getLong(1));
				taskSums.setContributionsNb(rs.getLong(2));
				pStmt.close();
				pStmt = null;
			}
			// Sinon, il faut calculer
			else {
				// Paramètre pour la clause 'LIKE'
				String pathLike = (task==null ? "" : task.getFullPath()) + "%";
	
				// Calcul des cumuls
				pStmt = con.prepareStatement("select sum(tsk_budget), sum(tsk_initial_cons), sum(tsk_todo) from task where tsk_path like ?");
				pStmt.setString(1, pathLike);
				rs = pStmt.executeQuery();
				if (!rs.next())
					throw new DbException("Nothing returned from this query", null);
				taskSums.setBudgetSum(rs.getLong(1));
				taskSums.setInitiallyConsumedSum(rs.getLong(2));
				taskSums.setTodoSum(rs.getLong(3));
				pStmt.close();
				pStmt = null;
				
				// Calcul du consommé
				pStmt = con.prepareStatement("select sum(ctb_duration), count(ctb_duration) from contribution, task where ctb_task=tsk_id and tsk_path like ?");
				pStmt.setString(1, pathLike);
				rs = pStmt.executeQuery();
				if (!rs.next())
					throw new DbException("Nothing returned from this query", null);
				taskSums.setConsumedSum(rs.getLong(1));
				taskSums.setContributionsNb(rs.getLong(2));
				pStmt.close();
				pStmt = null;
				
			}		
			// Retour du résultat
			return taskSums;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération des cumuls pour la tache d'identifiant '" + task.getId()+ "'", e);
		}
		finally {
			try { if (pStmt!=null) pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Supprime un collaborateur.
	 * @param tx le contexte de transaction.
	 * @param collaborator le collaborateur à supprimer.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void removeCollaborator(DbTransaction tx, Collaborator collaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("delete from collaborator where clb_id=?");
			pStmt.setLong  (1, collaborator.getId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed!=1)
				throw new SQLException("No row was deleted");

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la suppression du collaborateur '" + collaborator.getLogin() + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Supprime une contribution.
	 * @param tx le contexte de transaction.
	 * @param contribution la contribution à supprimer.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void removeContribution(DbTransaction tx, Contribution contribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("delete from contribution where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?");
			pStmt.setInt   (1, contribution.getYear());
			pStmt.setInt   (2, contribution.getMonth());
			pStmt.setInt   (3, contribution.getDay());
			pStmt.setLong  (4, contribution.getContributorId());
			pStmt.setLong  (5, contribution.getTaskId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed!=1)
				throw new SQLException("No row was deleted");

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la suppression d'une contribution", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Supprime une durée du référentiel de durées.
	 * @param tx le contexte de transaction.
	 * @param duration la durée à supprimer.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void removeDuration(DbTransaction tx, long duration) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("delete from duration where dur_id=?");
			pStmt.setLong  (1, duration);

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed!=1)
				throw new SQLException("No row was deleted");

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la suppression de la durée '" + duration + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Supprime une tache.
	 * @param tx le contexte de transaction.
	 * @param task la tache à supprimer.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void removeTask(DbTransaction tx, Task task) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Control sur les sous taches
			Task[] subTasks = DbMgr.getSubtasks(tx, task);
			for (int i=0; i<subTasks.length; i++) {
				DbMgr.removeTask(tx, subTasks[i]);
			}

			// Préparation de la requête
			pStmt = con.prepareStatement("delete from task where tsk_id=?");
			pStmt.setLong  (1, task.getId());

			// Exécution de la requête
			int removed = pStmt.executeUpdate();
			if (removed!=1)
				throw new SQLException("No row was deleted");

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la suppression de la tache '" + task + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Annule le modifications effectuées dans le cadre d'une transactrion.
	 * @param tx contexte de transaction.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static void rollbackTransaction(DbTransaction tx) throws DbException {
		try { tx.getConnection().rollback();	}
		catch (SQLException e ) {
			log.info("Incident SQL", e);
			throw new DbException("Echec du rollback", e);
		}
	}

	/**
	 * Modifie les attributs d'un collaborateur.
	 * @param tx contexte de transaction.
	 * @param collaborator le collaborateur à modifier.
	 * @return le collaborateur modifié.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Collaborator updateCollaborator(DbTransaction tx, Collaborator collaborator) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("update collaborator set clb_login=?, clb_first_name=?, clb_last_name=? where clb_id=?");
			pStmt.setString(1, collaborator.getLogin());
			pStmt.setString(2, collaborator.getFirstName());
			pStmt.setString(3, collaborator.getLastName());
			pStmt.setLong  (4, collaborator.getId());

			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new SQLException("No row was updated");

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return collaborator;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la mise à jour du collaborateur '" + collaborator.getLogin() + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Modifie les attributs d'une contribution.
	 * @param tx contexte de transaction.
	 * @param contribution la contribution à modifier.
	 * @return la contribution modifiée.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Contribution updateContribution(DbTransaction tx, Contribution contribution) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("update contribution set ctb_duration=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?");
			pStmt.setLong  (1, contribution.getDuration());
			pStmt.setInt   (2, contribution.getYear());
			pStmt.setInt   (3, contribution.getMonth());
			pStmt.setInt   (4, contribution.getDay());
			pStmt.setLong  (5, contribution.getContributorId());
			pStmt.setLong  (6, contribution.getTaskId());
			
			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new SQLException("No row was updated");

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contribution;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la mise à jour d'une contribution", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Change la tache d'une contribution.
	 * @param tx contexte de transaction.
	 * @param contribution la contribution.
	 * @param newContributionTask la tache à affecter.
	 * @return la contribution mise à jour.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Contribution changeContributionTask(
		DbTransaction tx, Contribution contribution, Task newContributionTask)
		throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("update contribution set ctb_task=? where ctb_year=? and ctb_month=? and ctb_day=? and ctb_contributor=? and ctb_task=?");
			pStmt.setLong  (1, newContributionTask.getId());
			pStmt.setInt   (2, contribution.getYear());
			pStmt.setInt   (3, contribution.getMonth());
			pStmt.setInt   (4, contribution.getDay());
			pStmt.setLong  (5, contribution.getContributorId());
			pStmt.setLong  (6, contribution.getTaskId());
			
			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new SQLException("No row was updated");

			// Mise à jour de la contribution
			contribution.setTaskId(newContributionTask.getId());

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return contribution;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la mise à jour d'une contribution", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}
	
	/**
	 * Modifie les attributs d'une tache.
	 * @param tx contexte de transaction.
	 * @param task la tache à modifier.
	 * @return la tache modifiée.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static Task updateTask(DbTransaction tx, Task task) throws DbException {
		PreparedStatement pStmt = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();

			// Préparation de la requête
			pStmt = con.prepareStatement("update task set tsk_path=?, tsk_number=?, tsk_code=?, tsk_name=?, tsk_budget=?, tsk_initial_cons=?, tsk_todo=? where tsk_id=?");
			pStmt.setString(1, task.getPath());
			pStmt.setByte  (2, task.getNumber());
			pStmt.setString(3, task.getCode());
			pStmt.setString(4, task.getName());
			pStmt.setLong  (5, task.getBudget());
			pStmt.setLong  (6, task.getInitiallyConsumed());
			pStmt.setLong  (7, task.getTodo());
			pStmt.setLong  (8, task.getId());
	
			// Exécution de la requête
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new SQLException("No row was updated");
	
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du résultat
			return task;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la mise à jour de la tache '" + task.getName() + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}
	
	/**
	 * Complete la requete de calcul de la somme des contributions.
	 * @param requestBase buffer utilisé pour la construction de la requête.
	 * @param contributor le collaborateur ayant contribué à la tache (facultatif).
	 * @param year l'année (facultative)
	 * @param month le mois (facultatif)
	 * @param day le jour (facultatif)
	 */
	private static void completeContributionRequest(StringBuffer requestBase, Collaborator contributor, Integer year, Integer month, Integer day) {
		if (contributor!=null) requestBase.append(" and ctb_contributor=?");
		if (year!=null) requestBase.append(" and ctb_year=?");
		if (month!=null) requestBase.append(" and ctb_month=?");
		if (day!=null) requestBase.append(" and ctb_day=?");
		log.debug("built request : " + requestBase.toString());
	}

	/**
	 * Complete les paramètres de la requete de calcul de la somme des contributions.
	 * @param pStmt le statement.
	 * @param startIndex
	 * @param contributor le collaborateur ayant contribué à la tache (facultatif).
	 * @param year l'année (facultative)
	 * @param month le mois (facultatif)
	 * @param day le jour (facultatif)
	 * @throws SQLException levé en cas d'incident avec la base de données.
	 */
	private static void completeContributionReqParams(PreparedStatement pStmt, int startIndex, Collaborator contributor, Integer year, Integer month, Integer day) throws SQLException {
		int idx = startIndex;
		log.debug("contributorId=" + (contributor!=null ? String.valueOf(contributor.getId()) : "null"));
		log.debug("year=" + year);
		log.debug("month=" + month);
		log.debug("day=" + day);
		if (contributor!=null) pStmt.setLong(idx++, contributor.getId());
		if (year!=null) pStmt.setInt(idx++, year.intValue());
		if (month!=null) pStmt.setInt(idx++, month.intValue());
		if (day!=null) pStmt.setInt(idx++, day.intValue());
	}
	
	/**
	 * Génère un nouveau numéro de tache pour un chemin donné.
	 * @param tx le contexte de transaction.
	 * @param path le chemin considéré.
	 * @return le numéro généré.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	protected static byte newTaskNumber(DbTransaction tx, String path) throws DbException {
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		try {
			// Récupération de la connexion
			Connection con = tx.getConnection();
	
			// Recherche du max
			pStmt = con.prepareStatement("select max(tsk_number) from task where tsk_path=?");
			pStmt.setString(1, path);
			rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbException("Nothing returned from this query", null);
			byte max = rs.getByte(1);
			log.debug("  => max= : " + max);
	
			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du résultat
			return (byte) (max + 1);
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la génération d'un nouveau numéro de tache pour le chemin '" + path + "'", e);
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Retourne l'identifiant généré automatiquement par la base de données.
	 * @param pStmt le statement SQL.
	 * @return l'identifiant généré.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	private static long getGeneratedId(PreparedStatement pStmt) throws DbException {
		long generatedId = -1;
		PreparedStatement pStmt1 = null;
		try {
			// Récupération de la connexion
			Connection con = pStmt.getConnection();
			// Cas de HSQLDB
			if (isHSQLDB(con)) {
				log.debug("HSQL Database detected");
				pStmt1 = con.prepareStatement("call identity()");
				ResultSet rs = pStmt1.executeQuery();
				if (!rs.next())
					throw new DbException("Nothing returned from this query", null);
				generatedId = rs.getLong(1);
				
				// Fermeture du statement
				pStmt1.close();
				pStmt1 = null;
			}
			else {
				log.debug("Generic Database detected");
				// Récupération de l'identifiant généré
				ResultSet rs = pStmt.getGeneratedKeys();
				if (!rs.next())
					throw new DbException("Nothing returned from this query", null);
				generatedId = rs.getLong(1);
			}
			// Retour du résultat
			log.debug("Generated id=" + generatedId);
			return generatedId;
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération de l'identifiant du nouvel objet", e);
		}
		finally {
			if (pStmt1!=null) try { pStmt1.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Indique si la BDD de données est une base HSQLDB.
	 * @param con la connexion SQL.
	 * @return un booléen indiquant si la BDD est de type HSQLDB.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	private static boolean isHSQLDB(Connection con) throws DbException {
		try {
			// Récupération du nom de la base de données
			String dbName = con.getMetaData().getDatabaseProductName();
			log.debug("DbName=" + dbName);
			return "HSQL Database Engine".equals(dbName);
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération du nom de la BDD", e);
		}
	}

	/**
	 * Indique si la BDD de données est une base HSQLDB embarquée.
	 * @param con la connexion SQL.
	 * @return un booléen indiquant si la BDD est de type HSQLDB embarquée.
	 * @throws DbException levé en cas d'incident technique d'accès à la base.
	 */
	private static boolean isEmbeddedHSQLDB(Connection con) throws DbException {
		try {
			// Récupération du nom de la base de données
			String dbName = con.getMetaData().getDatabaseProductName();
			log.debug("DbName=" + dbName);
			return isHSQLDB(con) && ds.getUrl().startsWith("jdbc:hsqldb:file");
		}
		catch (SQLException e) {
			log.info("Incident SQL", e);
			throw new DbException("Echec lors de la récupération du nom de la BDD", e);
		}
	}

}
