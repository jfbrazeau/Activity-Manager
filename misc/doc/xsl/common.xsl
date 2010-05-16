<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE xsl:stylesheet [<!ENTITY nbsp "&#160;">]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	
	<xsl:template match="p">
		<p><xsl:apply-templates/></p>
	</xsl:template>

	<xsl:template match="center">
		<center><xsl:apply-templates/></center>
	</xsl:template>

	<xsl:template match="bold">
		<b><xsl:apply-templates/></b>
	</xsl:template>

	<xsl:template match="italic">
		<i><xsl:apply-templates/></i>
	</xsl:template>

	<xsl:template match="underlined">
		<u><xsl:apply-templates/></u>
	</xsl:template>

	<xsl:template match="text">
		<xsl:value-of select="."/>
	</xsl:template>

	<xsl:template match="br">
		<br/>
	</xsl:template>

	<xsl:template match="code">
		<code><xsl:value-of select="."/></code>
	</xsl:template>

	<xsl:template match="ul">
		<ul>
			<xsl:apply-templates/>
		</ul>
	</xsl:template>

	<xsl:template match="li">
		<li>
			<xsl:apply-templates/>
		</li>
	</xsl:template>

	<xsl:template match="ol">
		<ol>
			<xsl:apply-templates/>
		</ol>
	</xsl:template>

	<xsl:template match="img">
		<xsl:choose>
			<xsl:when test="@thumbnail!=''">
				<a target="newWin">
					<xsl:attribute name="href">
						<xsl:text>res/img/</xsl:text>
						<xsl:value-of select="@src"/>
					</xsl:attribute>
					<img border="0">
						<xsl:attribute name="src">
							<xsl:text>res/img/</xsl:text>
							<xsl:value-of select="@thumbnail"/>
						</xsl:attribute>
					</img>
					<br/>
					Click to enlarge
				</a>
			</xsl:when>
			<xsl:otherwise>
				<img border="0">
					<xsl:attribute name="src">
						<xsl:text>res/img/</xsl:text>
						<xsl:value-of select="@src"/>
					</xsl:attribute>
				</img>
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
								<xsl:text>.html#</xsl:text>
								<xsl:value-of select="substring-after(@href, '#')"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="@href"/>
								<xsl:text>.html</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<!-- Cas local-ref -->
					<xsl:when test="@type='local-ref'">
						<xsl:text>res/html/</xsl:text>
						<xsl:value-of select="@href"/>
					</xsl:when>
					<!-- Cas external-ref -->
					<xsl:otherwise><xsl:value-of select="@href"/></xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:apply-templates/>
		</a>
	</xsl:template>

	<xsl:template name="buildBody">
		<!-- Sommaire -->
		<xsl:for-each select="chapter">
			<xsl:if test="title != ''">
				<a>
					<xsl:attribute name="href">#CHAPTER<xsl:value-of select="position()"/></xsl:attribute>
					<xsl:value-of select="position()"/><xsl:text>. </xsl:text><xsl:value-of select="title"/>
				</a>
				<br/>
			</xsl:if>
		</xsl:for-each>
		<!-- Séparation entre sommaire et chapitres si au moins un titre est spécifié -->
		<xsl:if test="count(chapter/title)>0">
			<br/>
		</xsl:if>
		<!-- Paragraphes -->
		<xsl:for-each select="chapter">
			<xsl:if test="title != ''">
				<!-- Ancre basée sur le N° du paragraphe -->
				<a class="textTitle">
					<xsl:attribute name="name">CHAPTER<xsl:value-of select="position()"/></xsl:attribute>
					<xsl:value-of select="position()"/><xsl:text>. </xsl:text><xsl:value-of select="title"/>
				</a>
				<!-- Ancre basée sur l'identifiant si l'identifiant du chapitre a été spécifié -->
				<xsl:if test="id != ''">
					<a><xsl:attribute name="name"><xsl:value-of select="id"/></xsl:attribute></a>
				</xsl:if>
			</xsl:if>
			<!-- Corps du paragraphe -->
			<xsl:apply-templates select="body"/>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>