<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="page" required="false" type="java.lang.String" %>

<c:if test="${signin.active || !signin.valid}"> <%-- Skip the whole thing when disabled --%>

<table id="dhLearnMoreNextStep" cellspacing="0" cellpadding="0">
<tr>
<c:choose>
	<c:when test="${signin.valid}">
		<td>
		    <dh:bean id="download" class="com.dumbhippo.web.DownloadBean" scope="request"/>
			<c:if test="${!empty download.download}">
				<div class="dh-learnmore-summary">
					Get the Mugshot download to use all the Mugshot features<c:out value="${option}"/> It's easy and free!
				</div>    
			</c:if>
		    <c:choose>
			    <c:when test="${download.windowsRequested}">					
					<div class="dh-download-buttons">
						<a id="dhDownloadProduct" class="dh-download-product" href="${download.download.url}"><img id="dhDownloadImage" src="/images3/${buildStamp}/download_now_button.gif"/></a>
					</div>
			    </c:when>
			    <c:when test="${download.linuxRequested}">					
				    <dh:script module="dh.util"/>
			    	<script type="text/javascript">
			    		dhDownloadDistributions = [
			    			<c:forEach items="${download.platform.distributions}" var="distribution">
								[
					    			<c:forEach items="${distribution.downloads}" var="d">
					    			{
										architecture: <dh:jsString value="${d.architecture}"/>,
										url: <dh:jsString value="${d.url}"/>
					    			},
					    			</c:forEach>
								],
				    		</c:forEach>
				    	];
				    	
				    	function dhDistributionOnChange() {
				    		var distroSelect = document.getElementById("dhDownloadDistribution");
				    		var downloadIndex = distroSelect.options[distroSelect.selectedIndex].value;
				    		if (downloadIndex >= 0) {
				    			if (distroSelect.options[0].value < 0)
					    			distroSelect.options[0] = null;
					    			
					    		var downloads = dhDownloadDistributions[downloadIndex];
				    		
					    		var downloadSelect = document.getElementById("dhDownloadDownload");
					    		downloadSelect.options.length = 0;
					    		for (var i = 0; i < downloads.length; i++)
						   			downloadSelect.add(new Option(downloads[i].architecture, downloads[i].url), null);
						   			
						   		dhDownloadOnChange();
				   			}
				    	}
				    	
				    	function dhDownloadOnChange() {
							var downloadSelect = document.getElementById("dhDownloadDownload");
							var downloadUrl = downloadSelect.options[downloadSelect.selectedIndex].value;

							if (downloadUrl != null) {
								var downloadButton = document.getElementById("dhDownloadProduct");
								downloadButton.href = downloadUrl;
								
								var downloadImage = document.getElementById("dhDownloadImage");
						    	downloadImage.src = dhImageRoot3 + "download_now_button.gif"
					    	}
				    	}
			    	</script>
					<c:if test="${empty download.download}">
						<div class="dh-learnmore-summary">
							We don't have an official binary of the Mugshot client for your version of Linux yet.
						</div>    
						<div class="dh-learnmore-subheading">
							Downloads for other Linux versions
						</div>
					</c:if>
					<div class="dh-download-buttons">
						<c:choose>
							<c:when test="${!empty download.download}">
								<a id="dhDownloadProduct" class="dh-download-product" href="${download.download.url}"><img id="dhDownloadImage" src="/images3/${buildStamp}/download_now_button.gif"/></a>
							</c:when>
							<c:otherwise>
								<a id="dhDownloadProduct" class="dh-download-product"><img id="dhDownloadImage" src="/images3/${buildStamp}/download_now_disabled.gif"/></a>
							</c:otherwise>
						</c:choose>
					    <select id="dhDownloadDistribution" onChange="dhDistributionOnChange()">
					    	<c:if test="${empty download.download}">
					    		<option value="-1" selected="1">&lt;Choose Version&gt;</option>
				    		</c:if>
			    			<c:forEach items="${download.platform.distributions}" var="distribution" varStatus="status">
				    			<c:if test="${!empty distribution.name}">
					    			<c:choose>
					    				<c:when test="${!empty download.download && distribution == download.download.distribution}">
					    				    <option value="${status.index}" selected="1"><c:out value="${distribution.name}"/> <c:out value="${distribution.osVersion}"/></option>,
					    				</c:when>
					    				<c:otherwise>
					    				    <option value="${status.index}"><c:out value="${distribution.name}"/> <c:out value="${distribution.osVersion}"/></option>,
					    				</c:otherwise>
				    				</c:choose>
								</c:if> 
				    		</c:forEach>
					    </select>
					    <select id="dhDownloadDownload" onChange="dhDownloadOnChange()">
					    	<c:if test="${!empty download.download}">
					    		<c:forEach items="${download.download.distribution.downloads}" var="d">
					    			<c:choose>
					    				<c:when test="${d == download.download}">
					    					<option value="${d.url}" selected="1"><c:out value="${d.architecture}"/></option>
					    				</c:when>
					    				<c:otherwise>
					    					<option value="${d.url}"><c:out value="${d.architecture}"/></option>
					    				</c:otherwise>
				    				</c:choose>
					    		</c:forEach>
					    	</c:if>
					    </select>
					</div>
					<c:if test="${empty download.download}">
						<div class="dh-learnmore-subheading">
						    <a href="http://developer.mugshot.org/wiki/Downloads">Source code and contributed binaries</a>
						</div>    
					</c:if>
				</c:when>
		        <c:when test="${download.macRequested}">
					<div class="dh-learnmore-summary">
			            We're still working on Mac OS X support. However, you can view <a href="/">your Mugshot Stacker</a> on the web.
		            </div>
		        </c:when>
			    <c:otherwise>
					<div class="dh-learnmore-summary">
				        We don't have a Mugshot Client for your computer yet. However, you can view <a href="/">your Mugshot Stacker</a> on the web.
			        </div>
			    </c:otherwise>
			</c:choose>	   
		</td>
		<td class="dh-download-details">
			<div class="dh-download-more-details">
				Download for:
				<c:choose>
					<c:when test="${!download.windowsRequested}">
						<a href="/${page}?platform=windows">Windows</a>
				    </c:when>
					<c:otherwise>
						<span class="dh-download-platform-selected">Windows</span>
				    </c:otherwise>
				</c:choose>
				|
				<c:choose>
					<c:when test="${!download.linuxRequested}">
						<a href="/${page}?platform=linux">Linux</a>
				    </c:when>
					<c:otherwise>
						<span class="dh-download-platform-selected">Linux</span>
				    </c:otherwise>
				</c:choose>
			</div>
			<c:if test="${!empty download.platform}">
			    <div class="dh-download-more-details">
	               Current version: <c:out value="${download.platform.version}, ${download.platform.date}."/>
	            </div>
            </c:if>
            <c:if test="${!(empty download.download && download.linuxRequested)}">
			    <a href="http://developer.mugshot.org/wiki/Downloads">Source code and contributed binaries for other platforms</a>.
		    </c:if>
		</td>
	</c:when>
	<c:otherwise>
		<td>
		    <table id="dhLearnMoreButtons" class="dh-download-buttons" cellspacing="0" cellpadding="0">
		    <tr>
		    <td><span class="dh-button"><a href="/signup"><img src="/images3/${buildStamp}/signup.gif"/></a></span></td>
		    <td valign="middle" align="center"><span class="dh-download-buttons-or">or</span></td>
		    <td><span class="dh-button"><a href="/who-are-you"><img src="/images3/${buildStamp}/login.gif"/></a></span></td>
		    </tr>
		    </table>
	    </td>
	</c:otherwise>
</c:choose>
</tr>
</table>	

</c:if>