<?xml version="1.0" encoding="ISO-8859-1" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:import href="common.xsl"/>
	<xsl:output encoding="ISO-8859-1" method="html" indent="no"/>
	
	<xsl:template match="book">
		JSP-HEADER
		<tiles:insert template="/WEB-INF/jsp/homepage/template.jsp">
			<tiles:put name="pageTitle"><xsl:value-of select="title"/></tiles:put>
			<tiles:put name="pageBody">
				<xsl:call-template name="buildBody"/>
			</tiles:put>
		</tiles:insert>
	</xsl:template>

	<xsl:template match="img">
		<xsl:choose>
			<xsl:when test="@thumbnail!=''">
				<html:link target="newWin">
					<xsl:attribute name="page">
						<xsl:text>/img/homepage/activitymgr/</xsl:text>
						<xsl:value-of select="@src"/>
					</xsl:attribute>
					<html:img border="0">
						<xsl:attribute name="page">
							<xsl:text>/img/homepage/activitymgr/</xsl:text>
							<xsl:value-of select="@thumbnail"/>
						</xsl:attribute>
					</html:img>
					<br/>
					Click to enlarge
				</html:link>
			</xsl:when>
			<xsl:otherwise>
				<html:img border="0">
					<xsl:attribute name="page">
						<xsl:text>/img/homepage/activitymgr/</xsl:text>
						<xsl:value-of select="@src"/>
					</xsl:attribute>
				</html:img>
			</xsl:otherwise>
		</xsl:choose>
		<br/>
	</xsl:template>

	<xsl:template match="a">
		<a>
			<xsl:if test="@target != ''">
				<xsl:attribute name="target">
					<xsl:value-of select="@target"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:attribute name="href">
				<xsl:choose>
					<!-- Cas book-ref -->
					<xsl:when test="@type='book-ref'">
						<xsl:choose>
							<xsl:when test="starts-with(@href, '#')">
								<xsl:value-of select="@href"/>
							</xsl:when>
							<xsl:when test="contains(@href, '#')">
								<xsl:value-of select="substring-before(@href, '#')"/>
								<xsl:text>.do#</xsl:text>
								<xsl:value-of select="substring-after(@href, '#')"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="@href"/>
								<xsl:text>.do</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<!-- Cas local-ref -->
					<xsl:when test="@type='local-ref'">
						<xsl:text><![CDATA[<%= request.getContextPath() %>/html/homepage/activitymgr/]]></xsl:text>
						<xsl:value-of select="@href"/>
					</xsl:when>
					<!-- Cas external-ref -->
					<xsl:otherwise><xsl:value-of select="@href"/></xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:apply-templates/>
		</a>
	</xsl:template>

</xsl:stylesheet>