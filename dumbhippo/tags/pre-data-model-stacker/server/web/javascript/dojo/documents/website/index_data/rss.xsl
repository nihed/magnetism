<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output omit-xml-declaration="yes" method="html" />
	<xsl:template match="/">
		<h2>
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="//channel/link" />
				</xsl:attribute>
				<xsl:value-of select="//channel/title" />
			</a>
		</h2>
		<dl>
			<xsl:apply-templates select="//item[position()&lt;4]" />
		</dl>
	</xsl:template>
	<xsl:template match="item">
		<dt>
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="link" />
				</xsl:attribute>
				<xsl:value-of select="title" disable-output-escaping="yes" />
			</a>
		</dt>
		<dd class="date">
			<xsl:value-of select="substring(pubDate, 6, 11)" />
		</dd>
		<!-- xsl:if test="position()=1" -->
			<dd>
				<xsl:choose>
					<xsl:when test="contains(description,'[...]')">
						<xsl:value-of select="substring(substring-before(description, ' [...]'), 1, 128)" disable-output-escaping="yes" />...
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="substring(description, 1, 128)" disable-output-escaping="yes" />...
					</xsl:otherwise>
				</xsl:choose>
				<a>
					<xsl:attribute name="href">
						<xsl:value-of select="link" />
					</xsl:attribute>
					<xsl:attribute name="class">more</xsl:attribute>
					more &amp;raquo;
				</a>
			</dd>
		<!-- /xsl:if -->
	</xsl:template>
</xsl:stylesheet>
