<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.TitleBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<c:set var="hasDescription" value="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.TitleDescriptionBlockView') && block.description != ''}"/>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${block.queuedMovies != null}">
    <td class="dh-stacker-block-with-image-left" align="left" valign="top" width="75%">
		<table cellspacing="0" cellpadding="0" width="100%">
			<tr>
			    <td valign="top" class="dh-music-block-icon" width="65">
                    <img src="${block.imageUrl}" width="65" height="90"/>
				</td>
				<td valign="top">
					<div class="dh-stacker-block-with-image-beside">	
						<dh:png klass="dh-stacker-block-icon" src="${block.icon}" style="width: 16; height: 16; border: none;"/>
						<dht3:simpleBlockTitle block="${block}" oneLine="false" homeStack="false" spanClass="dh-stacker-block-title-generic"/>
						<div class="dh-stacker-block-header-description">
						    ${block.descriptionAsHtml}
						</div>    
					</div>
				</td>
			</tr>
		</table>	
		<dht3:blockContent blockId="${blockId}">
		    Movies in the Queue:
		    <c:forEach items="${block.queuedMovies.movies}" var="movie">
		        <br/><c:out value="${movie.priority}"/> <a href="${movie.url}"><c:out value="${movie.title}"/></a>
			</c:forEach>
		</dht3:blockContent>	
	</td>
	<td width="0%">&nbsp;</td>
	<dht3:blockRight blockId="${blockId}" from="${block.entitySource}" showFrom="${showFrom}">
		<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
		<dht3:blockControls blockId="${blockId}">
			&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>
