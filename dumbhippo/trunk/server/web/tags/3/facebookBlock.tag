<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.FacebookBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>

<%-- this tag displays a single facebook event, the first one in the list of facebook events --%>
<%-- in FacebookBlockView; currently all FacebookBlockViews should have a single event associated --%>
<%-- with them --%>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${(block.photos.size > 0) && !oneLine}">
	<dht3:blockLeft block="${block}">
		<%-- FIXME is this table layout different from simpleBlockTitle on purpose or just for historical reasons? --%>
		<table cellspacing="0" cellpadding="0">
		<tr>
		<td class="dh-stacker-block-title">
		    <c:if test="${!oneLine}">  
		        <span class="dh-stacker-block-title-type"><c:out value="${block.typeTitle}"/>:</span>
		    </c:if>     
		</td>    
		<td>
		<div class="dh-stacker-block-title-facebook-event">
			<a class="dh-underlined-link" href="${block.link}">
			    <c:choose>
			        <c:when test="${homeStack}">
			            <c:out value="${block.titleForHome}"/>
			        </c:when>
			        <c:otherwise>
			            <%-- if you see updates about your own Facebook not on your own homepage, it --%>
			            <%-- is less confusing if they are in the third person --%>
			            <c:out value="${block.title}"/>           
			        </c:otherwise>
			    </c:choose>         
			</a>
		</div>
		</td>
		</tr>
		</table>
	<dht3:blockDescription blockId="${blockId}">
	</dht3:blockDescription>
	<dht3:blockContent blockId="${blockId}">
	    <c:if test="${block.photos.size > 0}">
	        photos go here!
	    </c:if>    
	</dht3:blockContent>			
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
		<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
	</dht3:blockRight>
</dht3:blockContainer>
