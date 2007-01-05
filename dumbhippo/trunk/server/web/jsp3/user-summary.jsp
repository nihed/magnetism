<%-- This page is used for iframe badges, it's not a regular page. Don't use any tags or stylesheets that assume a normal page. --%>
<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${empty person}">
	<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.UserSummaryPage"/>
	<jsp:setProperty name="person" property="needExternalAccounts" value="true"/>
</c:if>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Summary - Mugshot</title>
	<%-- Don't include the stylesheets for "normal" pages, makes things too confusing. Keep this standalone.
	     Also it's good to load in one http get since we can't assume someone browsing to a random blog 
	     has any of our css/js already cached. --%>
	<dht:faviconIncludes/>
	<style type="text/css"> <%-- borrowed from google-stacker-content.jsp - we might want to combine eventually --%>
		body, td, a, p, div, span {
			font-size:	13px;
			font-family: arial, sans-serif;
		}
		a:link {
			color: #0000CC;
		}
	</style>
</head>

<body>

<div>
       <table border="0" cellpadding="2px" cellspacing="0">
       <tr>
       <td rowspan="2"><dht:headshot person="${person.viewedPerson}" size="60" /></td>
       <td><img src="${baseUrl}${person.viewedPerson.onlineIcon}" border="0"/>&nbsp;<c:out value="${person.viewedPerson.name}"/>'s Mugshot</td>
	</tr>
	<tr>
       <td><a target="_top" href="${baseUrl}/person?who=${person.viewedUserId}">Visit My Mugshot Page</a></td>
       </tr>
       </table>

	<div>
       <c:forEach var="account" items="${person.viewedPerson.lovedAccounts.list}">

       <a target="_top" title="${account.externalAccount.siteName}" href="${account.link}"><img src="${baseUrl}/images3/${buildStamp}/${account.externalAccount.iconName}" border="0"/></a>&nbsp;
	</c:forEach>
	</div>

	<div>
		<c:forEach items="${person.pageableMugshot.results}" var="block">
			<div>
				<table cellspacing="0" cellpadding="0">
					<tbody>
						<tr>
							<td>
								<dh:png src="${block.icon}" style="width: 16; height: 16; border: none; margin-right: 3px;"/>
							</td>
							<td>
								<jsp:element name="a">
									<jsp:attribute name="target">_top</jsp:attribute>
									<jsp:attribute name="href"><c:out value="${block.summaryLink}"/></jsp:attribute>
									<jsp:body>
										<c:out value="${block.summaryLinkText}"/>
									</jsp:body>
								</jsp:element>
							</td>
						</tr>
						<tr>
							<td></td>
							<td>
								<span style="color: #6f6f6f;">
									<c:if test="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.EntitySourceBlockView')}">
										from <c:out value="${block.entitySource.name}"/>
									</c:if>
									(<c:out value="${block.summaryTimeAgo}"/>)
								</span>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</c:forEach>
	</div>

</div>

</body>

</html>
