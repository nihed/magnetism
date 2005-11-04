<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="log">
		<html>
			<body>
				<h1>JAIMBot Log</h1>
				<hr>
				</hr>
				<h2>Messages Received:</h2>
				<table border="1">
					<tr bgcolor="#0000FF">
						<th align="left">Time</th>
						<th align="left">Message</th>
					</tr>
					<xsl:for-each select="record[method='handleMessage']">
						<xsl:if test="not(starts-with(message,'Request(')) and not(starts-with(message,'Default Request'))">
							<tr>
								<td>
									<xsl:value-of select="date"/>
								</td>
								<td>
									<xsl:value-of select="message"/>
								</td>
							</tr>
						</xsl:if>
					</xsl:for-each>
				</table>
				<xsl:if test="not(count(record[level='SEVERE']) = 0)">
				<hr>
				</hr>
				<h2>Errors:</h2>
				<table border="1">
					<tr bgcolor="#FF0000">
						<th align="left">Time</th>
						<th align="left">Class</th>
						<th align="left">Method</th>
						<th align="left">Error</th>
					</tr>
					<xsl:for-each select="record[level='SEVERE']">
						<tr>
							<td>
								<xsl:value-of select="date"/>
							</td>
							<td>
								<xsl:value-of select="class"/>
							</td>
							<td>
								<xsl:value-of select="method"/>
							</td>
							<td>
								<xsl:value-of select="message"/>
							</td>
						</tr>
					</xsl:for-each>
				</table>
				</xsl:if>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
