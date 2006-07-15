<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<dht:body>
	<jsp:doBody/><%-- expecting this to be empty, just relocate stuff into the top and bottom areas --%>
	<div id="dhMain">
		<div id="dhCenteredRectangle">
			<div id="dhGrayBorder">
				<div id="dhWhiteBorder">
					<div id="dhContentArea">
						<div id="dhGreenBackground">
							<dh:relocateDest where="insideSmallBoxTopArea"/>
						</div>
						<div id="dhLogoPicture">
							<dht:logo/>
						</div>
						<dh:whenRelocateDestNeeded where="insideSmallBoxBottomArea">
							<div id="dhMessageArea">
								<dh:relocateDest where="insideSmallBoxBottomArea"/>
							</div>
						</dh:whenRelocateDestNeeded>
					</div>
				</div>
			</div>
		</div>
		<dht:bottom/>
	</div>
</dht:body>
