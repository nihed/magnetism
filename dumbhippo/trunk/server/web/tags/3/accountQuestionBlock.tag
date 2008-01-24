<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:script modules="dh.util,dh.server"/>
<script type="text/javascript">
	function dhAnswerAccountQuestion(blockId, response) {
	   	dh.server.doPOST("answeraccountquestion",
					     { "blockId" : blockId,
					       "response" : response },
			  	    	 function(type, data, http) {
						     dh.util.refresh();
			  	    	 },
			  	    	 function(type, error, http) {
			  	    	     alert("Couldn't update your response.");
			  	    	 });
   	}
</script>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.AccountQuestionBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<c:set var="hasDescription" value="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.TitleDescriptionBlockView') && block.description != ''}"/>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" title="${block.title}" expandable="false">
	<dht3:blockLeft block="${block}">
		<dht3:simpleBlockTitle block="${block}" oneLine="${oneLine}" homeStack="false" spanClass="dh-stacker-block-title-generic"/>
		<dht3:blockDescription blockId="${blockId}">${block.descriptionAsHtml}</dht3:blockDescription>
        <c:if test="${block.moreLink != null}"> 
		    <div class="dh-thumbnail-block-more">
	            <jsp:element name="a">
				    <jsp:attribute name="href"><c:out value="${block.moreLink}"/></jsp:attribute>
				    <jsp:body>Read More</jsp:body>
			    </jsp:element>
		    </div>
		</c:if>
        <c:if test="${!oneLine && !chatHeader}">
			<dht3:stackReason block="${block}" blockId="${blockId}"/>
			<dht3:blockContent blockId="${blockId}">
				<dht3:quipper block="${block}" blockId="${blockId}"/>
				<dht3:chatPreview block="${block}" blockId="${blockId}"/>
			</dht3:blockContent>		    
		</c:if>
		<form>
		    <c:forEach items="${block.question.buttons}" var="button">
		        <input type="button" value="${button.text}" onclick="dhAnswerAccountQuestion(${blockId},${button.response})"></input>
		    </c:forEach>    
		</form>
	</dht3:blockLeft>
</dht3:blockContainer>