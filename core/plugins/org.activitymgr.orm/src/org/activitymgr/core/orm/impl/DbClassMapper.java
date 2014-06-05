package org.activitymgr.core.orm.impl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.activitymgr.core.orm.AbstractOrderByClause;
import org.activitymgr.core.orm.AbstractStatement;
import org.activitymgr.core.orm.AscendantOrderByClause;
import org.activitymgr.core.orm.BetweenStatement;
import org.activitymgr.core.orm.ClassDescriptorException;
import org.activitymgr.core.orm.DbClassMapping;
import org.activitymgr.core.orm.DbClassMappingException;
import org.activitymgr.core.orm.DescendantOrderByClause;
import org.activitymgr.core.orm.GreaterThanStatement;
import org.activitymgr.core.orm.IDbClassMapper;
import org.activitymgr.core.orm.InStatement;
import org.activitymgr.core.orm.LikeStatement;
import org.activitymgr.core.orm.LowerThanStatement;
import org.apache.log4j.Logger;

/**
 * Classe peremettant de mapper une classe Java avec une table dans une base
 * de donn�es.
 * @author jbrazeau
 * TODO Javadoc
 */
public class DbClassMapper<TYPE> implements IDbClassMapper<TYPE> {

	/** Logger */
	private static Logger log = Logger.getLogger(DbClassMapper.class);

	/** Logger */
	private static Logger sqlLog = Logger.getLogger("dbClassMapper.logsqlrequests");

	/** Descripteur de classe */
	private ClassDescriptor<TYPE> descriptor;

	/** Nom de la table mapp�e */
	private String tableName;
	
	/** Liste des attributs mapp�s dans la table */
	private String[] attributeNames;
	
	/** Table contenant les associations entre nom d'attribut et colonne SQL */
	private Properties columnNamesDictionnary = new Properties();
	
	/** Liste des noms d'attributs de la cl� primaire */
	private String[] pkAttributeNames; 

	/** Nom de l'attribut auto g�n�r�s par la BDD si il existe */
	private String autoGeneratedAttributeName; 

	/** Table contenant les format de colonne */
	private Properties columnFormats = new Properties();
	
	/** Timezone utilis� pour les dates */
	private TimeZone timeZone = TimeZone.getDefault();

	/** Requ�tes */
	private String selectAllRequest;
	private String selectWithPKRequest;
	private String deletAllRequest;
	private String deletWithPKRequest;
	private String updateRequest;
	private String insertRequest;
	private String countAllRequest;

	/**
	 * Constructeur priv�.
	 * @param mapping mapping de la classe mapp�e.
	 * @param theClass la classe mapp�e.
	 * @param timeZone le timeZone utilis� pour les dates.
	 * @throws DbClassMappingException lev�e en cas de mauvaise configuration du
	 * 		mapping.
	 */
	public DbClassMapper(DbClassMapping mapping, Class<TYPE> theClass, TimeZone timeZone) throws DbClassMappingException {
		this.timeZone = timeZone;
		descriptor = ClassDescriptor.getDescriptor(theClass);
		if (log.isDebugEnabled())
			log.debug("Descriptor loaded");
		tableName = mapping.getSQLTableName(theClass);
		// R�cup�ration de la liste des attributs de la cl� primaire
		pkAttributeNames = mapping.getPrimaryKeyAttributesName(theClass);
		// R�cup�ration de l'�ventuel attribut auto g�n�r�
		autoGeneratedAttributeName = mapping.getAutoGeneratedAttributeName(theClass);
		// R�cup�ration des attributs de la classe
		attributeNames = descriptor.getAttributeNames();
		// Parcours des attributs, construction du dictionnaire de colonnes et
		// d�tection de l'�ventuel attribut auto g�n�r� par la BDD
		for (int i=0; i<attributeNames.length; i++) {
			String attributeName = attributeNames[i];
			String columnName = mapping.getSQLColumnName(theClass, attributeName);
			// Si l'attribut n'est pas d�fini, il est ignor�
			if (columnName!=null) {
				columnNamesDictionnary.put(attributeName, columnName);
				String columnFormat = mapping.getAttributeFormat(theClass, attributeName);
				if (columnFormat!=null)
					columnFormats.setProperty(attributeName, columnFormat);
			}
		}
		// R�cup�ration de la liste des attributs filtr�e puis tri
		attributeNames = (String[]) columnNamesDictionnary.keySet().toArray(new String[columnNamesDictionnary.size()]);
		Arrays.sort(attributeNames);

		// Construction de la requ�te de s�lection de toute les valeurs
		// d'un table
		StringBuffer buf = new StringBuffer("select ");
		appendColumnNames(buf, true, true, false);
		buf.append(" from ").append(tableName);
		selectAllRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("selectAllRequest='" + selectAllRequest + "'");

		// Construction de la requ�te de s�lection � partir de la cl� primaire
		// (r�utilisation de la requ�te selectAllRequest)
		buf.setLength(0);
		buf.append("select ");
		appendColumnNames(buf, false, true, false);
		buf.append(" from ").append(tableName);
		appendWherePK(buf);
		selectWithPKRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("selectWithPKRequest='" + selectWithPKRequest + "'");
		
		// Construction de la requ�te de suppression � partir de la cl� primaire
		buf.setLength(0);
		buf.append("delete from ").append(tableName);
		deletAllRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("deletAllRequest='" + deletAllRequest + "'");

		// Construction de la requ�te de suppression � partir de la cl� primaire
		buf.setLength(0);
		buf.append(deletAllRequest);
		appendWherePK(buf);
		deletWithPKRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("deletWithPKRequest='" + deletWithPKRequest + "'");

		// Construction de la requ�te d'insertion
		buf.setLength(0);
		buf.append("insert into ").append(tableName).append(" (");
		appendColumnNames(buf, true, false, false);
		buf.append(") values (");
		int parameterIdx = 1;
		for (int i=0; i<attributeNames.length; i++) {
			String attributeName = attributeNames[i];
			if (!attributeName.equals(autoGeneratedAttributeName)) {
				buf.append(parameterIdx==1 ? "?" :", ?");
				parameterIdx++;
			}
		}
		buf.append(")");
		insertRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("insertRequest='" + insertRequest + "'");

		// Construction de la requ�te de mise � jour
		buf.setLength(0);
		buf.append("update ").append(tableName).append(" set ");
		appendColumnNames(buf, false, false, true);
		appendWherePK(buf);
		updateRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("updateRequest='" + updateRequest + "'");
		
		// Construction de la requ�te de comptage de toutes les lignes
		buf.setLength(0);
		buf.append("select count(*) from ").append(tableName);
		countAllRequest = buf.toString();
		if (log.isInfoEnabled())
			log.info("countAllRequest='" + countAllRequest + "'");
		
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#selectWithPK(java.sql.Connection, java.lang.Object[])
	 */
	@Override
	public TYPE selectByPK(Connection con, Object[] pkValue) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(selectWithPKRequest);
		PreparedStatement pStmt = null;
		try {
			checkPkAttributeValues(pkValue);
			TYPE result = null;
			// Si le nombre d'attributs de la classe est �gal � la cl� primaire, 
			// la requ�te g�n�r�e est fausse (car elle ignore par d�faut les colonnes
			// de la cl� primaire puisqu'elles sont sp�cifi�es en param�tre)
			if (pkAttributeNames.length==attributeNames.length) {
				boolean exists = count(con, pkAttributeNames, pkValue)>0;
				if (exists) {
					result = descriptor.newInstance();
					for (int i=0; i<pkValue.length; i++) {
						descriptor.setInstanceAttributeValue(
								result, pkAttributeNames[i], pkValue[i]);
					}
				}
			}
			// Autres cas
			else {
				pStmt = con.prepareStatement(selectWithPKRequest);
				for (int i=0; i<pkValue.length; i++) {
					String attributeName = pkAttributeNames[i];
					Object attibuteValue = pkValue[i];
					attributeValueToStatementColumn(attributeName, attibuteValue, pStmt, i+1);
				}
				ResultSet rs = pStmt.executeQuery();
				if (rs.next()) {
					result = descriptor.newInstance();
					resultSetToInstanceAttributes(rs, result, false);
					for (int i=0; i<pkValue.length; i++) {
						descriptor.setInstanceAttributeValue(
								result, pkAttributeNames[i], pkValue[i]);
					}
				}
	
				// Fermeture du statement
				pStmt.close();
				pStmt = null;
			}

			// Retour du r�sultat
			return result;
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#deleteWithPK(java.sql.Connection, TYPE)
	 */
	@Override
	public boolean deleteByPK(Connection con, TYPE instance) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(deletWithPKRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(deletWithPKRequest);
			int parameterIdx = 1;
			for (int i=0; i<pkAttributeNames.length; i++) {
				String attributeName = pkAttributeNames[i];
				instanceAttributeToStatementColumn(instance, attributeName, pStmt, parameterIdx);
				parameterIdx++;
			}
			// Construction du r�sultat
			boolean deleted = pStmt.executeUpdate()==1;

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du r�sultat
			return deleted;
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#delete(java.sql.Connection, java.lang.String[], java.lang.Object[])
	 */
	@Override
	public int delete(Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues) throws SQLException {
		StringBuffer buf = new StringBuffer(deletAllRequest);
		appendCustomWhereClause(buf, whereClauseAttributeNames, whereClauseAttributeValues);
		String request = buf.toString();
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("customDeleteRequest=" + request);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(request);
			bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			int deleted = pStmt.executeUpdate();

			// Fermeture du statement
			pStmt.close();
			pStmt = null;
			
			// Retour du r�sultat
			return deleted;
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	private void checkPkAttributeValues(Object[] pkAttributeValues) throws DbClassMappingException {
		if (pkAttributeValues==null)
			throw new DbClassMappingException("PK attributes must be specified", null);
		if (pkAttributeValues.length!=pkAttributeNames.length) 
			throw new DbClassMappingException("Wrong parameters number. Primary key contains " + pkAttributeNames.length + " items", null);
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#selectAll(java.sql.Connection)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TYPE[] selectAll(Connection con) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(selectAllRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(selectAllRequest);
			ResultSet rs = pStmt.executeQuery();
			List<Object> result = new ArrayList<Object>();
			while (rs.next()) {
				TYPE newInstance = descriptor.newInstance();
				result.add(newInstance);
				if (log.isDebugEnabled())
					log.debug("newInstance=" + newInstance);
				resultSetToInstanceAttributes(rs, newInstance, true);
			}
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return result.toArray((TYPE[]) Array.newInstance(descriptor.getDescribedClass(), result.size()));
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#dump(java.io.OutputStream, java.lang.String, java.sql.Connection, java.lang.String[], java.lang.Object[], java.lang.Object[], int)
	 */
	@Override
	public void dump(OutputStream out, String encoding, Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues, Object[] orderByClauseItems, int maxRows) throws SQLException {
		// Pr�paration de la requ�te de s�lection
		String request = builSelectRequest(whereClauseAttributeNames, whereClauseAttributeValues, orderByClauseItems, maxRows);
		PreparedStatement pStmt = null;
		try {
			// G�n�ration du dump (script SQL contenant les insert)
			PrintStream pOut = new PrintStream(out, true, encoding);
			pStmt = con.prepareStatement(request);
			// Binding de la clause where
			int parametersCount = bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			// Binding de la clause limit
			if (maxRows>0)
				pStmt.setInt(parametersCount, maxRows);
			ResultSet rs = pStmt.executeQuery();
			boolean requestBeginningFlusehd = false;
			while (rs.next()) {
				// Cr�ation de la base de la requ�te d'insertion
				if (!requestBeginningFlusehd) {
					StringBuffer buf = new StringBuffer("insert into ").append(tableName).append(" (");
					appendColumnNames(buf, true, true, false);
					buf.append(") values");
					pOut.println(buf);
					requestBeginningFlusehd = true;
				}
				// Lignes suivantes
				else {
					pOut.println(",");
				}
				// Ajout des donn�es
				pOut.print("(");
				for (int i=0; i<attributeNames.length; i++) {
					pOut.flush();
					if (i!=0) pOut.print(", ");
					pOut.print('\'');
					pOut.print(rs.getString(i+1).replaceAll("'", "''"));
					pOut.print('\'');
				}
				pOut.print(")");
			}
			pOut.println(";");
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Flush
			pOut.flush();
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		catch (UnsupportedEncodingException e) {
			log.error("Error while initializing stream", e);
			throw new DbClassMappingException("Error while initializing stream", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	private String builSelectRequest(String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues, Object[] orderByClauseItems, int maxRows) throws SQLException {
		StringBuffer buf = new StringBuffer(selectAllRequest);
		appendCustomWhereClause(buf, whereClauseAttributeNames, whereClauseAttributeValues);
		// Ajout de la clause 'order by'
		if (orderByClauseItems!=null && orderByClauseItems.length>0) {
			buf.append(" order by ");
			for (int i=0; i<orderByClauseItems.length; i++) {
				if (i!=0) buf.append(", ");
				Object orderByClauseItem = orderByClauseItems[i];
				if (!(orderByClauseItem instanceof AbstractOrderByClause)) {
					buf.append(columnNamesDictionnary.getProperty((String) orderByClauseItem));
				}
				else {
					String attributeName = ((AbstractOrderByClause) orderByClauseItem).getAttributeName();
					buf.append(columnNamesDictionnary.getProperty(attributeName));
					if (orderByClauseItem instanceof AscendantOrderByClause)
						buf.append(" asc");
					else if (orderByClauseItem instanceof DescendantOrderByClause)
						buf.append(" desc");
					else
						throw new DbClassMappingException("Unknown order by clause item type : '" + orderByClauseItem + "'", null);
				}
			}
		}
		// Ajout de la clause limit
		if (maxRows>0)
			buf.append(" limit ?");
		String request = buf.toString();
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("customSelectRequest=" + request);
		return request;
	}
	
	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#select(java.sql.Connection, java.lang.String[], java.lang.Object[], java.lang.Object[], int)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TYPE[] select(Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues, Object[] orderByClauseItems, int maxRows) throws SQLException {
		String request = builSelectRequest(whereClauseAttributeNames, whereClauseAttributeValues, orderByClauseItems, maxRows);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(request);
			// Binding de la clause where
			int parametersCount = bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			// Binding de la clause limit
			if (maxRows>0)
				pStmt.setInt(parametersCount, maxRows);
			ResultSet rs = pStmt.executeQuery();
			List<Object> result = new ArrayList<Object>();
			while (rs.next()) {
				TYPE newInstance = descriptor.newInstance();
				result.add(newInstance);
				if (log.isDebugEnabled())
					log.debug("newInstance=" + newInstance);
				resultSetToInstanceAttributes(rs, newInstance, true);
			}
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return result.toArray((TYPE[]) Array.newInstance(descriptor.getDescribedClass(), result.size()));
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#update(java.sql.Connection, TYPE)
	 */
	@Override
	public TYPE update(Connection con, TYPE value) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(updateRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(updateRequest);
			int mappedParametersNb = instanceAttributesToStatement(value, pStmt, false, false);
			int parameterIdx = mappedParametersNb + 1;
			for (int i=0; i<pkAttributeNames.length; i++) {
				String attributeName = pkAttributeNames[i];
				instanceAttributeToStatementColumn(value, attributeName, pStmt, parameterIdx);
				parameterIdx++;
			}
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new DbClassMappingException("Row update failed");
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return value;
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#insert(java.sql.Connection, TYPE)
	 */
	@Override
	public TYPE insert(Connection con, TYPE value) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(insertRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(insertRequest, Statement.RETURN_GENERATED_KEYS);
			instanceAttributesToStatement(value, pStmt, true, false);
			int updated = pStmt.executeUpdate();
			if (updated!=1)
				throw new DbClassMappingException("Row insertion failed");
			getAutoGeneratedKey(pStmt, value);
			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return value;
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#countAll(java.sql.Connection)
	 */
	@Override
	public long countAll(Connection con) throws SQLException {
		if (sqlLog.isDebugEnabled())
			sqlLog.debug(countAllRequest);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(countAllRequest);
			ResultSet rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbClassMappingException("Nothing returned form this count query!");
			long count = rs.getLong(1); 

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return count;
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.activitymgr.core.orm.impl.IDbClassMapper#count(java.sql.Connection, java.lang.String[], java.lang.Object[])
	 */
	@Override
	public long count(Connection con, String[] whereClauseAttributeNames, Object[] whereClauseAttributeValues) throws SQLException {
		StringBuffer buf = new StringBuffer(countAllRequest).append(" ");
		appendCustomWhereClause(buf, whereClauseAttributeNames, whereClauseAttributeValues);
		String request = buf.toString();
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("customCountRequest=" + request);
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement(request);
			bindAttributeValueToStatement(pStmt, whereClauseAttributeNames, whereClauseAttributeValues);
			ResultSet rs = pStmt.executeQuery();
			if (!rs.next())
				throw new DbClassMappingException("Nothing returned form this count query!");
			long count = rs.getLong(1); 

			// Fermeture du statement
			pStmt.close();
			pStmt = null;

			// Retour du r�sultat
			return count;
		}
		catch (ClassDescriptorException e) {
			log.error("Error while accessing instance attribute", e);
			throw new DbClassMappingException("Error while accessing instance attribute", e); 
		}
		finally {
			if (pStmt!=null) try { pStmt.close(); } catch (SQLException ignored) {}
		}
	}

	private int instanceAttributesToStatement(TYPE instance, PreparedStatement pStmt, boolean includePK, boolean includeAutoGenerated) throws ClassDescriptorException, SQLException {
		if (log.isDebugEnabled())
			log.debug("instanceAttributesToStatement(" + instance + ", " + pStmt + ", " + includePK + ", " + includeAutoGenerated + ")");
		List<?> pkList = Arrays.asList(pkAttributeNames);
		int parameterIdx = 1;
		for (int i=1; i<=attributeNames.length; i++) {
			String attributeName = attributeNames[i-1];
			if ((includePK || !pkList.contains(attributeName)) 
					&& (includeAutoGenerated ||!attributeName.equals(autoGeneratedAttributeName))) {
				instanceAttributeToStatementColumn(instance, attributeName, pStmt, parameterIdx);
				parameterIdx++;
			}
		}
		return parameterIdx-1;
	}

	private void instanceAttributeToStatementColumn(TYPE instance, String attributeName, PreparedStatement pStmt, int parameterIdx) throws ClassDescriptorException, SQLException {
		Object attributeValue = descriptor.getInstanceAttributeValue(instance, attributeName);
		attributeValueToStatementColumn(attributeName, attributeValue, pStmt, parameterIdx);
	}

	private int bindAttributeValueToStatement(PreparedStatement pStmt, String[] attributeNames, Object[] attributeValues) throws ClassDescriptorException, SQLException {
		int parameterIdx = 1;
		if (attributeNames!=null) {
			for (int i=0; i<attributeNames.length; i++) {
				String attributeName = attributeNames[i];
				Object attributeValue = attributeValues[i];
				if (attributeValue instanceof AbstractStatement) {
					if (attributeValue instanceof InStatement) {
						for (Object value : ((InStatement) attributeValue).getValues()) {
							attributeValueToStatementColumn(attributeName, value, pStmt, parameterIdx++);
						}
					}
					else if (attributeValue instanceof BetweenStatement) {
						BetweenStatement bs = (BetweenStatement) attributeValue;
						attributeValueToStatementColumn(attributeName, bs.getLow(), pStmt, parameterIdx++);
						attributeValueToStatementColumn(attributeName, bs.getHigh(), pStmt, parameterIdx++);
					}
					else if (attributeValue instanceof GreaterThanStatement) {
						GreaterThanStatement gts = (GreaterThanStatement) attributeValue;
						attributeValueToStatementColumn(attributeName, gts.getValue(), pStmt, parameterIdx++);
					}
					else if (attributeValue instanceof LowerThanStatement) {
						LowerThanStatement lts = (LowerThanStatement) attributeValue;
						attributeValueToStatementColumn(attributeName, lts.getValue(), pStmt, parameterIdx++);
					}
					else if (attributeValue instanceof LikeStatement) {
						LikeStatement lts = (LikeStatement) attributeValue;
						attributeValueToStatementColumn(attributeName, lts.getValue(), pStmt, parameterIdx++);
					}
					else 
						throw new DbClassMappingException("Unknown statement type : " + attributeValue);
				}
				else {
					attributeValueToStatementColumn(attributeName, attributeValue, pStmt, parameterIdx++);
				}
			}
			
		}
		return parameterIdx;
	}

	private void attributeValueToStatementColumn(String attributeName, Object attributeValue, PreparedStatement pStmt, int parameterIdx) throws ClassDescriptorException, SQLException {
		if (log.isDebugEnabled())
			log.debug("  - attributeName='" + attributeName + "'");
		Class<?> attributeType = descriptor.getAttributeType(attributeName);
		if (sqlLog.isDebugEnabled())
			sqlLog.debug("    +-> attributeValue='" + attributeValue + "'");
		// Par d�faut le param�tre est mapp� sur la valeur directe de l'attribut
		Object parameterValue = attributeValue;
		// Dans certains cas, une conversion de type est effectu�e
		if (attributeType.equals(Calendar.class)) {
			String format = columnFormats.getProperty(attributeName);
			Calendar calendar = (Calendar) attributeValue;
			calendar.setTimeZone(timeZone);
			if (format==null) {
				parameterValue = new Date(calendar.getTimeInMillis());
			}
			else {
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				sdf.setTimeZone(calendar.getTimeZone());
				parameterValue = sdf.format(calendar.getTime());
			}
		}
		// Enregistrement du param�tre
		pStmt.setObject(parameterIdx, parameterValue);
	}

	private void resultSetToInstanceAttributes(ResultSet rs, TYPE instance, boolean includePK) throws SQLException, ClassDescriptorException, DbClassMappingException {
		List<?> pkList = Arrays.asList(pkAttributeNames);
		int parameterIdx = 1;
		for (int i=0; i<attributeNames.length; i++) {
			String attributeName = attributeNames[i];
			if (includePK || !pkList.contains(attributeName)) {
				resultSetColumnToInstanceAttribute(rs, parameterIdx, instance, attributeName);
				parameterIdx++;
			}
		}
	}
	
	private void resultSetColumnToInstanceAttribute(ResultSet rs, int rsColumnIdx, TYPE instance, String attributeName) throws SQLException, ClassDescriptorException, DbClassMappingException {
		Class<?> attributeType = descriptor.getAttributeType(attributeName);
		Object attributeValue = null;
		if (attributeType.equals(String.class))
			attributeValue = rs.getString(rsColumnIdx);
		else if (attributeType.equals(BigDecimal.class))
			attributeValue = rs.getBigDecimal(rsColumnIdx);
		else if (attributeType.equals(Integer.class) || attributeType.equals(int.class))
			attributeValue = new Integer(rs.getInt(rsColumnIdx));
		else if (attributeType.equals(Long.class) || attributeType.equals(long.class))
			attributeValue = new Long(rs.getLong(rsColumnIdx));
		else if (attributeType.equals(Float.class) || attributeType.equals(float.class))
			attributeValue = new Float(rs.getFloat(rsColumnIdx));
		else if (attributeType.equals(Double.class) || attributeType.equals(double.class))
			attributeValue = new Double(rs.getFloat(rsColumnIdx));
		else if (attributeType.equals(Short.class) || attributeType.equals(short.class))
			attributeValue = new Short(rs.getShort(rsColumnIdx));
		else if (attributeType.equals(Byte.class) || attributeType.equals(byte.class))
			attributeValue = new Byte(rs.getByte(rsColumnIdx));
		else if (attributeType.equals(Character.class) || attributeType.equals(char.class))
			attributeValue = new Character(rs.getString(rsColumnIdx).charAt(0));
		else if (attributeType.equals(Boolean.class) || attributeType.equals(boolean.class))
			attributeValue = new Boolean(rs.getBoolean(rsColumnIdx));
		else if (attributeType.equals(Calendar.class)) {
			String format = columnFormats.getProperty(attributeName);
			if (format==null) {
				GregorianCalendar calendar = new GregorianCalendar(timeZone);
				attributeValue = calendar;
				calendar.setTimeInMillis(rs.getDate(rsColumnIdx).getTime());
			}
			else {
				String date = rs.getString(rsColumnIdx);
				if (log.isDebugEnabled())
					log.debug("date=" + date);
				try { 
					SimpleDateFormat sdf = new SimpleDateFormat(format);
					sdf.setTimeZone(timeZone);
					Calendar cal = new GregorianCalendar(timeZone);
					cal.setTime(sdf.parse(date));
					attributeValue = cal; 
				}
				catch (ParseException e) {
					log.error("Mauvais format du param�tre", e);
					throw new DbClassMappingException("Mauvais format du param�tre", e);
				}
			}
		}
		else 
			throw new DbClassMappingException("Unknown attribute type : " + attributeType);
		if (log.isDebugEnabled())
			log.debug("    +-> '" + attributeName + "'='" + attributeValue + "'");
		descriptor.setInstanceAttributeValue(
				instance, attributeName, attributeValue);
	}

	private void appendColumnNames(StringBuffer buf, boolean includePK, boolean includeAutoGenerated, boolean addStamtementParameter) {
		List<?> pkList = Arrays.asList(pkAttributeNames);
		boolean firstItem = true;
		for (int i=0; i<attributeNames.length; i++) {
			String attributeName = attributeNames[i];
			if ((includePK || !pkList.contains(attributeName))
					&& (includeAutoGenerated || !attributeName.equals(autoGeneratedAttributeName))) {
				if (!firstItem)
					buf.append(", ");
				String columnName = columnNamesDictionnary.getProperty(attributeName);
				buf.append(columnName);
				if (addStamtementParameter) {
					buf.append("=?");
				}
				firstItem = false;
			}
		}
	}
	
	private void appendWherePK(StringBuffer buf) {
		buf.append(" where ");
		for (int i=0; i<pkAttributeNames.length; i++) {
			String pkAttributeName = pkAttributeNames[i];
			String pkColumnName = columnNamesDictionnary.getProperty(pkAttributeName);
			if (i!=0) buf.append(" and ");
			buf.append(pkColumnName).append("=?");
		}
	}

	private void appendCustomWhereClause(StringBuffer buf, String[] whereClauseAttributeNames, Object[] whereClauseAttributesValues) throws DbClassMappingException {
		if (whereClauseAttributeNames!=null) {
			if (whereClauseAttributesValues==null || whereClauseAttributeNames.length!=whereClauseAttributesValues.length)
				throw new DbClassMappingException("Wrong argument number", null);
			buf.append(" where ");
			for (int i=0; i<whereClauseAttributeNames.length; i++) {
				String whereClauseAttributeName = whereClauseAttributeNames[i];
				Object whereClauseAttributeValue = whereClauseAttributesValues[i];
				String whereClausColumnName = columnNamesDictionnary.getProperty(whereClauseAttributeName);
				if (i!=0) buf.append(" and ");
				buf.append(whereClausColumnName);
				if (whereClauseAttributeValue instanceof AbstractStatement) {
					if (whereClauseAttributeValue instanceof InStatement) {
						InStatement stmt = (InStatement) whereClauseAttributeValue;
						if (stmt.getValues().length == 1) {
							buf.append("=?");
						}
						else {
							buf.append(" in (");
							boolean first = true;
							for (@SuppressWarnings("unused") Object value : stmt.getValues()) {
								buf.append(first ? "?" : ", ?");
								first = false;
							}
							buf.append(")");
						}
					}
					else if (whereClauseAttributeValue instanceof BetweenStatement) {
						buf.append(" between ? and ?");
					}
					else if (whereClauseAttributeValue instanceof GreaterThanStatement) {
						GreaterThanStatement gts = (GreaterThanStatement) whereClauseAttributeValue;
						buf.append(">");
						if (gts.getOrEquals())
							buf.append("=");
						buf.append("?");
					}
					else if (whereClauseAttributeValue instanceof LowerThanStatement) {
						LowerThanStatement lts = (LowerThanStatement) whereClauseAttributeValue;
						buf.append("<");
						if (lts.getOrEquals())
							buf.append("=");
						buf.append("?");
					}
					else if (whereClauseAttributeValue instanceof LikeStatement) {
						buf.append(" like ?");
					}
					else 
						throw new DbClassMappingException("Unknown statement type : " + whereClauseAttributeValue);
				}
				else {
					buf.append("=?");
				}
			}
		}
	}
	
	/**
	 * Retourne l'identifiant g�n�r� automatiquement par la base de donn�es.
	 * @param pStmt le statement SQL.
	 * @return l'identifiant g�n�r�.
	 * @throws SQLException lev� en cas d'incident technique d'acc�s � la base.
	 * @throws ClassDescriptorException lev� en cas d'incident lors de l'acc�s
	 * 		aux attributs de l'instance. 
	 */
	private void getAutoGeneratedKey(PreparedStatement pStmt, TYPE instance) throws SQLException, ClassDescriptorException {
		PreparedStatement pStmt1 = null;
		try {
			// Pas de g�n�ration si aucun attribut auto g�n�r� n'est sp�cifi�
			if (autoGeneratedAttributeName!=null) {
				// R�cup�ration de la connexion
				Connection con = pStmt.getConnection();
				// Cas de HSQLDB
				if (isHSQLDB(con)) {
					if (log.isDebugEnabled())
						log.debug("HSQL Database detected");
					pStmt1 = con.prepareStatement("call identity()");
					ResultSet rs = pStmt1.executeQuery();
					if (!rs.next())
						throw new DbClassMappingException("Error while retrieving auto generated key");
					resultSetColumnToInstanceAttribute(rs, 1, instance, autoGeneratedAttributeName);
					
					// Fermeture du statement
					pStmt1.close();
					pStmt1 = null;
				}
				else {
					if (log.isDebugEnabled())
						log.debug("Generic Database detected");
					// R�cup�ration de l'identifiant g�n�r�
					ResultSet rs = pStmt.getGeneratedKeys();
					if (!rs.next())
						throw new DbClassMappingException("Error while retrieving auto generated key");
					resultSetColumnToInstanceAttribute(rs, 1, instance, autoGeneratedAttributeName);
				}
				// Retour du r�sultat
				if (log.isDebugEnabled())
					log.debug("Generated id=" + descriptor.getInstanceAttributeValue(instance, autoGeneratedAttributeName));
			}
		}
		finally {
			if (pStmt1!=null) try { pStmt1.close(); } catch (Throwable ignored) { }
		}
	}

	/**
	 * Indique si la BDD de donn�es est une base HSQLDB.
	 * @param con la connexion SQL.
	 * @return un bool�en indiquant si la BDD est de type HSQLDB.
	 * @throws SQLException lev� en cas d'incident technique d'acc�s � la base.
	 */
	private static boolean isHSQLDB(Connection con) throws SQLException {
		// R�cup�ration du nom de la base de donn�es
		String dbName = con.getMetaData().getDatabaseProductName();
		if (log.isDebugEnabled())
			log.debug("DbName=" + dbName);
		return "HSQL Database Engine".equals(dbName);
	}

}
