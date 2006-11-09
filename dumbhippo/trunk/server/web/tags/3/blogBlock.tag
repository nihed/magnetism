<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlogBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}">
	<dht3:blockLeft block="${block}">
		<dht3:blockTitle>
		    <c:if test="${!oneLine}"> 
			    <span class="dh-stacker-block-title-type">Blog post:</span>
			</c:if>    	
			<span class="dh-stacker-block-title-blog-post">
				<jsp:element name="a">
					<jsp:attribute name="class">dh-underlined-link</jsp:attribute>
					<jsp:attribute name="href"><c:out value="${block.entry.link.url}"/></jsp:attribute>
					<jsp:body><c:out value="${block.entry.title}"/></jsp:body>
				</jsp:element>
			</span>
		</dht3:blockTitle>
		<dht3:blockDescription blockId="${blockId}">
		</dht3:blockDescription>			
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
		<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
	</dht3:blockRight>
</dht3:blockContainer>
