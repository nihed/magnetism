dojo.provide("dh.admin");

dojo.require("dh.server");

dh.admin.sendRepairEmail = function(userId) {
   	dh.server.doPOST("sendrepairemail",
				     { "userId" : userId },
		  	    	 function(type, data, http) {
		  	    	     alert("Repair email sent");
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't send repair email");
		  	    	 });
}

dh.admin.reindexAll = function() {
   	dh.server.doPOST("reindexall",
				     {},
		  	    	 function(type, data, http) {
		  	    	     alert("Started reindexing process for indices");
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Failure reindexing");
		  	    	 });
}

dh.admin.setAdminDisabled = function(userId, disabled) {
   	dh.server.doPOST("setadmindisabled",
				     { "userId" : userId,
				       "disabled" : disabled },
		  	    	 function(type, data, http) {
		  	    	     document.location.reload()
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't " + (disabled ? "disable" : "enable") +  " user")
		  	    	 });
}

dh.admin.setNewFeatures = function(flag) {
   	dh.server.doPOST("setnewfeatures",
				     { "newFeaturesFlag" : flag },
		  	    	 function(type, data, http) {
		  	    	     document.location.reload()
		  	    	 },
		  	    	 function(type, error, http) {
		  	    	     alert("Couldn't change new features flag")
		  	    	 });
}

dh.admin.shell = {}

dh.admin.shell.executing = false

dh.admin.shell.parseCheckQueued = false
dh.admin.shell.parseCheckNeeded = false

dh.admin.shell.setNotification = function (text, isError) {
	var notify = document.getElementById("dhAdminShellMessage")
	dh.util.clearNode(notify)
	notify.appendChild(document.createTextNode(text))
	if (isError) {
		notify.style.color = "red"
	} else {
		notify.style.color = "black";
	}
}

dh.admin.shell.parseResult = function (childNodes) {
	for (var i = 0; i < childNodes.length; i++) {
		var node = childNodes[i]
		if (node.nodeName == "result") {
			var resultType = node.getAttribute("type")
			if (resultType == "success") {
				var resultValue = null
				var resultOutput = null
				var resultReflection = null
				for (var j = 0; j < node.childNodes.length; j++) {
					var subnode = node.childNodes[j]
					var text = ""
					if (subnode.firstChild && subnode.firstChild.nodeType == 3)
						text = subnode.firstChild.nodeValue
						
					if (subnode.nodeName == "retval") {
						resultValue = text
					} else if (subnode.nodeName == "output") {
						resultOutput = text
					} else if (subnode.nodeName == "retvalReflection") {
						resultReflection = subnode.childNodes
					}
				}
				return { "type" : "success",
				         "value" : resultValue,
				         "reflection" : resultReflection,
				         "output" : resultOutput }
			} else if (resultType == "exception") {
				var resultMessage = null
				var resultOutput = null
				var resultTrace = null				
				for (var j = 0; j < node.childNodes.length; j++) {
					var subnode = node.childNodes[j]
					var text = ""
					if (subnode.firstChild && subnode.firstChild.nodeType == 3)
						text = subnode.firstChild.nodeValue
						
					if (subnode.nodeName == "message") {
						resultMessage = text
					} else if (subnode.nodeName == "output") {
						resultOutput = text
					} else if (subnode.nodeName == "trace") {
						resultTrace = text
					}
				}
				return { "type" : "exception",
				         "message" : resultMessage,
				         "trace" : resultTrace,
				         "output" : resultOutput }			
			}
		}
	}
	return null
}

dh.admin.shell.queueParseCheck = function () {
	if (dh.admin.shell.parseCheckQueued) {
		dh.admin.shell.parseCheckNeeded = true
		return;
	}
	dh.admin.shell.parseCheckQueued = true	
	dh.admin.shell.parseTimeout = setTimeout(function () { 
		dh.admin.shell.parseCheckQueued = false	
		dh.admin.shell.doParseCheck(); 
		if (dh.admin.shell.parseCheckNeeded) {
			dh.admin.shell.parseCheckNeeded = false
			dh.admin.shell.queueParseCheck()
		}		
	}, 2000)
}

dh.admin.shell.exec = function (parseOnly, cb) {
	var text = document.getElementById("dhAdminShellInput").value

	dh.server.doXmlMethod("adminshellexec",
						  { "parseOnly": "" + parseOnly,
						  	"command" : text },
			  	    	  function(childNodes, http) {
							var result = dh.admin.shell.parseResult(childNodes)
							cb(result)
						  },
		  	    	 	  function(code, msg, http) {
		  	    	 		 dh.admin.shell.setNotification("HumanVisibleException: " + msg, true)
	  		  	    	  },
	 		  	    	  function(type, error, http) {
		  	    	 		 dh.admin.shell.setNotification("Server error: type=" + type + ", error=" + error, true)
			  	    	  });							  
}

dh.admin.shell.doParseCheck = function () {
	dh.admin.shell.exec(true, function (result) {	
							if (dh.admin.shell.executing)
								return
							if (result.type == "success")
								dh.admin.shell.setNotification("Waiting")
							else
								dh.admin.shell.setNotification("Parsing: " + result.message)
	  	  	    	 	  })					  
}

dh.admin.shell.doEval = function () {
	if (dh.admin.shell.executing)
		return
	dh.admin.shell.executing = true
	if (dh.admin.shell.parseCheckQueued) {
		clearTimeout(dh.admin.shell.parseTimeout)
		dh.admin.shell.parseCheckQueued = false
	}
	dh.admin.shell.setNotification("Evaluating...")
	var evalButton = document.getElementById("dhAdminShellEvalButton")
	evalButton.style.disabled = true
	dh.admin.shell.exec(false, function (result) {
							dh.admin.shell.executing = false
							evalButton.style.disabled = false
							var outputDiv = document.getElementById("dhAdminShellOutput")
							var traceDiv = document.getElementById("dhAdminShellTrace")							
							outputDiv.value = result.output
							traceDiv.value = ""
							if (result.type == "success") {
								dh.admin.shell.setNotification("Evaluation complete")
								var resultDiv = document.getElementById("dhAdminShellResult")
								var reflectionDiv = document.getElementById("dhAdminShellResultReflection")
								dh.util.clearNode(resultDiv)
								dh.util.clearNode(reflectionDiv)								
								resultDiv.appendChild(document.createTextNode(result.value))
								if (result.reflection != null) {
								for (var i = 0; i < result.reflection.length; i++) {
									var node = result.reflection[i]
									if (node.nodeName == "method") {
										var p = document.createElement("div")
										p.appendChild(document.createTextNode(node.getAttribute("return") + " "))
										p.appendChild(document.createTextNode(node.getAttribute("name") + "("))									
										for (var j = 0; j < node.childNodes.length; j++) {
											var subnode = node.childNodes[j]
											if (subnode.nodeName == "param") {
												p.appendChild(document.createTextNode(subnode.firstChild.nodeValue))
												if (j < node.childNodes.length - 1)
													p.appendChild(document.createTextNode(", "))
											}
										}
										p.appendChild(document.createTextNode(")"))
										reflectionDiv.appendChild(p)
									}
								}
								}
							} else {
								dh.admin.shell.setNotification("Exception: " + result.message, true)
								traceDiv.style.display = "block"
								traceDiv.value = result.trace
							}
	  	  	    	 	  })			
}

dh.admin.shell.insertUser = function(id) {
	document.getElementById("dhAdminShellInput").value += ("user(\"" + id + "\")")
}