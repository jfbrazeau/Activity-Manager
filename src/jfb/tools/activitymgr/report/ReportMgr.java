/*
 * Copyright (c) 2004-2012, Jean-Francois Brazeau. All rights reserved.
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
package jfb.tools.activitymgr.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.core.util.Strings;
import jfb.tools.activitymgr.ui.util.CfgMgr;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.SimpleLog4JLogSystem;

/**
 * G�n�rateur de rapports.
 * 
 * <p>
 * Consulter le fichier de configuration <code>reports.properties</code> pour
 * plus de d�tails sur la configuration des rapports.
 * </p>
 */
public class ReportMgr {

	/** Logger */
	private static Logger log = Logger.getLogger(ReportMgr.class);

	/**
	 * M�thode principale.
	 * 
	 * @param args
	 *            argument de la m�thode principale.
	 */
	public static void main(String[] args) {
		try {
			// Initialisation des logs et chargement de la config
			PropertyConfigurator.configure("cfg/log4j.properties"); //$NON-NLS-1$
			CfgMgr.load();

			// Chargement du fichier de config
			Properties reportProps = new Properties();
			reportProps.load(new FileInputStream("cfg/reports.properties")); //$NON-NLS-1$

			// Initialisation de la connexion � la base de donn�es
			String jdbcDriver = CfgMgr.get(CfgMgr.JDBC_DRIVER);
			String jdbcUrl = CfgMgr.get(CfgMgr.JDBC_URL);
			String jdbcUser = CfgMgr.get(CfgMgr.JDBC_USER);
			String jdbcPassword = CfgMgr.get(CfgMgr.JDBC_PASSWORD);
			ModelMgr.initDatabaseAccess(jdbcDriver, jdbcUrl, jdbcUser,
					jdbcPassword);

			// Quels sont les identifiants des rapports � g�n�rer ?
			String reportList = reportProps.getProperty("reports.list"); //$NON-NLS-1$
			String[] reportIds = reportList.split(","); //$NON-NLS-1$
			// It�ration sur les rapports
			for (int i = 0; i < reportIds.length; i++) {
				// R�cup�ration de l'ID du rapport et de son impl�mentation
				String reportId = reportIds[i].trim();
				log.info("Processing report '" + reportId + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!"".equals(reportId)) { //$NON-NLS-1$
					// G�n�ration du fichier
					build(reportId, reportProps);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Lance la g�n�ration d'un rapport.
	 * 
	 * @param reportId
	 *            l'identifiant du rapport.
	 * @param props
	 *            le dictionnaire de proprit�t�s.
	 * @throws ReportException
	 *             lev� en cas d'incident inattendu lors de la g�n�ration du
	 *             rapport.
	 * @throws IOException
	 *             lev� en cas d'incident I/O en �criture sur le fichier.
	 */
	public static void build(String reportId, Properties props)
			throws ReportException, IOException {
		PropertiesHelper propsHelper = new PropertiesHelper(reportId, props);
		// R�cup�ration du nom du template
		String reportType = propsHelper.getProperty("type"); //$NON-NLS-1$
		String reportTemplate = propsHelper.getProperty("template"); //$NON-NLS-1$
		// Absence des 2 propri�t�s => erreur
		if (reportType == null && reportTemplate == null)
			throw new ReportException(
					Strings.getString(
							"ReportMgr.errors.REQUIRED_TEMPLATE_TYPE_OR_PATH", reportId), null); //$NON-NLS-1$ //$NON-NLS-2$
		// Si le type est sp�cifi�, utilisation du r�pertoire par d�faut
		if (reportType != null)
			reportTemplate = "templates/" + reportType + ".vm"; //$NON-NLS-1$ //$NON-NLS-2$
		// V�rification du type de rapport
		if (!new File(reportTemplate).exists())
			throw new ReportException(
					Strings.getString(
							"ReportMgr.errors.TEMPLATE_NOT_FOUND", reportTemplate), null); //$NON-NLS-1$ //$NON-NLS-2$

		// R�cup�ration du nom de fichier de sortie
		String outputFileName = propsHelper.getProperty("outputFileName"); //$NON-NLS-1$
		if (outputFileName == null)
			throw new ReportException(
					Strings.getString(
							"ReportMgr.errors.OUTPUT_FILENAME_REQUIRED", reportId), null); //$NON-NLS-1$ //$NON-NLS-2$
		PrintWriter out = openOutputFile(outputFileName);

		// Initialisation du contexte Velocity
		VelocityContext context = new VelocityContext();
		context.put("model", new ModelMgr()); //$NON-NLS-1$
		context.put("dates", new DateHelper()); //$NON-NLS-1$
		context.put("sums", new SumHelper()); //$NON-NLS-1$
		context.put("fmt", new FormatHelper()); //$NON-NLS-1$
		context.put("test", new TestHelper()); //$NON-NLS-1$
		context.put("props", propsHelper); //$NON-NLS-1$

		// Initialisation du moteur Velocity
		VelocityEngine engine = new VelocityEngine();
		// D�sactivation du chargement dans le CLASSPATH
		// engine.setProperty(VelocityEngine.RESOURCE_LOADER, "classpath");
		// engine.setProperty(
		// "classpath." + VelocityEngine.RESOURCE_LOADER + ".class",
		// ClasspathResourceLoader.class.getName());
		// D�finition de la politique de gestion des traces (afin que les
		// logs Velocity soient avec ceux de l'appli)
		engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
				SimpleLog4JLogSystem.class.getName());
		engine.setProperty("runtime.log.logsystem.log4j.category", //$NON-NLS-1$
				ReportMgr.class.getName());
		// Patch pour �viter le log indiquant que le fichier
		// VM_global_library.vm
		// n'a pas �t� trouv�
		engine.setProperty(VelocityEngine.VM_LIBRARY, ""); //$NON-NLS-1$
		try {
			// Initialisation
			engine.init();
			// Chargement du template
			Template t = engine.getTemplate(reportTemplate);
			// Merge
			t.merge(context, out);
		} catch (Exception e) {
			log.error("Unexpected error", e); //$NON-NLS-1$
			throw new ReportException(
					Strings.getString("ReportMgr.errors.UNEXPECTED_ERROR"), e); //$NON-NLS-1$
		}
		// Fermeture du fichier g�n�r�
		out.close();
	}

	/**
	 * Ouvre en �criture le fichier de sortie du rapport.
	 * 
	 * @param fileName
	 *            le nom du fichier � ouvrir.
	 * @return le flux d'�criture.
	 * @throws ReportException
	 *             lev� en cas d'incident inattendu lors de la g�n�ration du
	 *             rapport.
	 * @throws IOException
	 *             lev� en cas d'incident I/O en �criture sur le fichier.
	 */
	private static PrintWriter openOutputFile(String fileName)
			throws ReportException, IOException {
		fileName = fileName.replace('\\', '/');
		log.info(" opening file '" + fileName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		int idx = fileName.lastIndexOf('/');
		if (idx > 0) {
			String dir = fileName.substring(0, idx);
			File _dir = new File(dir);
			if (!_dir.exists() && !_dir.mkdirs())
				throw new IOException(Strings.getString(
						"ReportMgr.errors.DIRECTORY_CREATION_FAILURE", dir)); //$NON-NLS-1$ //$NON-NLS-2$

		}
		FileOutputStream fout = new FileOutputStream(fileName);
		PrintWriter out = new PrintWriter(fout);
		return out;
	}

}
