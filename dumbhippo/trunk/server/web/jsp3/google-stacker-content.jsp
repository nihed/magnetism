<%-- NOTE this is the Google Gadget content, it is not supposed to include html, head, or body (?) --%>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:choose>
	<c:when test="${!signin.valid}">
	
		<div>
			Try <a href="${baseUrl}/who-are-you" target="_top">logging in here</a>.
		</div>
		
		<div>
		<%-- See http://www.google.com/apis/gadgets/fundamentals.html#Cookies 
		     I think we need to do a P3P policy instead of making this questionable 
		     recommendation, for IE6. Firefox works right now but there is an open 
		     bug or three where they are considering just blocking all third party 
		     cookies (maybe without adding the P3P stuff, I don't know). Safari probably
		     isn't fixable afaik. --%>
		     
			<span style="font-size: 11px;"> <i>If you are already logged in to <a href="${baseUrl}" target="_top">${baseUrl}</a>,
			your browser may be incompatible with this site as configured. If you are using Microsoft
			Internet Explorer, you can change your security settings by choosing
			<b>Tools &gt; Internet Options</b>. Open the <b>Privacy</b> tab, click <b>Advanced</b>,
			and then check <b>Override automatic cookie handling</b>. Under <b>Third-party Cookies</b>,
			click <b>Accept</b>. Alternatively, you can try another web browser, such as
			<a href="http://mozilla.com">Firefox</a>. To learn more, 
			<a href="http://en.wikipedia.org/wiki/HTTP_cookie#Privacy_and_third-party_cookies">this
			Wikipedia article</a> may be helpful.</i> </span>
		</div>
	
	</c:when>
	<c:otherwise>
		<dht3:requireStackedPersonBean/>
		
		<c:set var="blocks" value="${person.pageableStack.results}" scope="page"/>
		
		<div>
			<c:forEach items="${blocks}" var="block" varStatus="status">
				<div>
					<b>
						<c:out value="${block.summaryHeading}"/>
						<c:if test="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.EntitySourceBlockView')}">
							by <c:out value="${block.entitySource.name}"/>
						</c:if>
					</b>
					<jsp:element name="a">
						<jsp:attribute name="target">_top</jsp:attribute>
						<jsp:attribute name="href"><c:out value="${block.summaryLink}"/></jsp:attribute>
						<jsp:body>
							<c:out value="${block.summaryLinkText}"/>
						</jsp:body>
					</jsp:element>			
					<c:out value="${block.summaryTimeAgo}"/>
				</div>
			</c:forEach>
		</div>
	</c:otherwise>
</c:choose>
