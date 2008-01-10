<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.TitleBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" title="${block.title}" expandable="${(!empty block.chatId || block.messageCount > 0) && !chatHeader}">
    <td class="dh-stacker-block-with-image-left" align="left" valign="top" width="75%">
		<table cellspacing="0" cellpadding="0" width="100%">
			<tr>
			    <td valign="top" class="dh-block-image-cell" width="${block.imageWidth}">
                    <img src="${block.imageUrl}" width="${block.imageWidth}" height="${block.imageHeight}"/>
				</td>
				<td valign="top">
					<div class="dh-stacker-block-with-image-beside">	
						<dh:png klass="dh-stacker-block-icon" src="${block.icon}" style="width: 16; height: 16; border: none;"/>
						<dht3:simpleBlockTitle block="${block}" oneLine="false" homeStack="false" spanClass="dh-stacker-block-title-generic"/>
						<c:choose>
						    <c:when test="${dh:enumIs(block.blockType, 'AMAZON_REVIEW')}"> 
						        <div>
						            <c:if test="${block.reviewRating > 0}">
						                <c:forEach varStatus="loopStatus" begin="1" end="5" step="1">
						                    <c:choose>
						                        <c:when test="${loopStatus.count <= block.reviewRating}">
						                            <dh:png klass="dh-rating-star" src="/images3/${buildStamp}/rating_star.png" style="width: 13; height: 12; overflow: hidden;"/>
						                        </c:when>
						                        <c:otherwise>
		                                            <dh:png klass="dh-rating-star"src="/images3/${buildStamp}/rating_star_blank.png" style="width: 13; height: 12; overflow: hidden;"/>					                    
						                        </c:otherwise>    
						                    </c:choose>
	                                    </c:forEach>
						            </c:if>
						            <span class="dh-amazon-review-title">${block.reviewTitle}</span>
						        </div>
						    </c:when>
						    <c:when test="${dh:enumIs(block.blockType, 'AMAZON_WISH_LIST_ITEM')}">
						        <div class="dh-stacker-block-title">
						            has been added to ${block.entitySource.name}'s 
						            <a href="${block.listLink}" title="Check out this list">
						                ${block.listName}
						            </a>.
						        </div>
						        <c:if test="${block.listItemComment != ''}">
						            <div>
						                <span class="dh-amazon-description">${block.entitySource.name} says:</span> ${block.listItemComment}
						            </div>    
						        </c:if>						        
						    </c:when>
						</c:choose>    						
						<c:if test="${dh:enumIs(block.blockType, 'AMAZON_WISH_LIST_ITEM')}">
						    <c:set var="extraDescriptionClass" value="dh-amazon-list-item-description"/>
						</c:if>
						<div class="dh-stacker-block-header-description ${extraDescriptionClass}">
						    <c:if test="${dh:enumIs(block.blockType, 'AMAZON_WISH_LIST_ITEM') && (block.description != '')}">
						        <span class="dh-amazon-description">Editorial review:</span>
						    </c:if> 
						    ${block.description}
						</div>    
						<c:if test="${!chatHeader}">
							<dht3:quipper blockId="${blockId}" block="${block}"/>
							<dht3:stackReason block="${block}" blockId="${blockId}"/>
						</c:if>
					</div>
				</td>
			</tr>
		</table>
		<c:if test="${!chatHeader}">
			<dht3:blockContent blockId="${blockId}">
				<dht3:chatPreview block="${block}" blockId="${blockId}"/>
			</dht3:blockContent>	
		</c:if>
	</td>
	<td width="0%">&nbsp;</td>
	<dht3:blockRight blockId="${blockId}" from="${block.entitySource}" showFrom="${showFrom}" chatHeader="${chatHeader}">
		<c:choose>
			<c:when test="${chatHeader}">
				<dht3:blockSentTimeAgo chatHeader="true">${block.sentTimeAgo}</dht3:blockSentTimeAgo>
			</c:when>
			<c:otherwise>
				<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
			</c:otherwise>
		</c:choose>
		<dht3:blockControls blockId="${blockId}">
			&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>