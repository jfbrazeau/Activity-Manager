<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE xsl:stylesheet [<!ENTITY nbsp "&#160;">]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:import href="common.xsl"/>
	<xsl:output encoding="ISO-8859-1" method="html" indent="yes"/>
	
	<xsl:template match="book">
		<html>
			<head>
				<title>Activity Manager - <xsl:value-of select="title"/></title>
				<link href="res/style.css" rel="stylesheet" type="text/css"/>
			</head>
			<body>
				<table cellpadding="0" cellspacing="0" width="780" class="main" align="center">
					<tr>
						<td colspan="2" class="mainTitle" height="50" align="center"><img src="res/img/head-780x80.jpg"/></td>
		
					</tr>
					<tr>
						<td width="110" rowspan="2" valign="top">
							<table width="100%">
								<tr>
									<!-- Ligne permettan de définir la taille des colonnes de manière ferme -->
									<td width="10"><img src="res/img/transp.gif"/></td>
									<td><img src="res/img/transp.gif"/></td>
								</tr>
		
								<tr class="menuItem"><td colspan="2">Activity Manager&nbsp;</td></tr>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">overview</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Overview</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">screenshots</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Screenshots</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">installation</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Installation</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">userGuide</xsl:with-param>
										<xsl:with-param name="menuItemTitle">User guide</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">javadoc</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Javadoc</xsl:with-param>
										<xsl:with-param name="menuItemHref">res/html/javadoc/index.html</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">legal</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Legal</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">releaseNotes</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Release notes</xsl:with-param>
									</xsl:call-template>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">downloads</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Downloads</xsl:with-param>
										<xsl:with-param name="menuItemHref">http://www.jfbrazeau.fr/main/homepage/activitymgr/download.do</xsl:with-param>
									</xsl:call-template>
								<tr>
									<!-- Ligne permettan de définir la taille des colonnes de manière ferme -->
									<td width="10"><img src="res/img/transp.gif"/></td>
									<td><img src="res/img/transp.gif"/></td>
								</tr>
								<tr class="menuItem"><td colspan="2">Miscellaneaous&nbsp;</td></tr>
									<xsl:call-template name="buildMenuItem">
										<xsl:with-param name="menuItemId">contact</xsl:with-param>
										<xsl:with-param name="menuItemTitle">Contact</xsl:with-param>
									</xsl:call-template>
							</table>
						</td>
					</tr>
					<tr>
						<td width="670" class="bodyCell" valign="top">
		
							<table width="100%" height="500">
								<tr>
									<td width="10">&nbsp;</td>
									<td class="bodyTitle" height="30">
										<xsl:value-of select="title"/>
									</td>
									<td width="10">&nbsp;</td>
								</tr>
								<tr>
		
									<td>&nbsp;</td>
									<td valign="top">
										<xsl:call-template name="buildBody"/>
									</td>
									<td>&nbsp;</td>
								</tr>
		
								<tr>
									<td colspan="3">&nbsp;</td>
								</tr>
							</table>
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="buildMenuItem">
		<xsl:param name="menuItemId"/>
		<xsl:param name="menuItemTitle"/>
		<xsl:param name="menuItemHref"/>
		<tr>
			<td>&nbsp;</td>
			<td>
				<a>
					<xsl:if test="$menuItemHref != ''">
						<xsl:attribute name="target">newWin</xsl:attribute>
					</xsl:if>
					<xsl:attribute name="href">
						<xsl:choose>
							<xsl:when test="$menuItemHref = ''">
								<xsl:value-of select="$menuItemId"/>
								<xsl:text>.html</xsl:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$menuItemHref"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="class">
						<xsl:text>menuSubItem</xsl:text>
						<xsl:if test="$menuItemTitle=title">Selected</xsl:if>
					</xsl:attribute>
					<xsl:value-of select="$menuItemTitle"/>
				</a>
			</td>
		</tr>
	</xsl:template>
</xsl:stylesheet>