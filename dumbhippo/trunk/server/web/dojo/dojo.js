(function(){
	var isRhino = ((typeof this["load"] == "function")&&(typeof this["Packages"] == "function"));
	var tmps = ["bootstrap1.js", "hostenv_"+((isRhino) ? "rhino" : "browser")+".js", "bootstrap2.js"];

	if( (this["djConfig"])&&(djConfig["baseScriptUri"]) ){
		var root = djConfig["baseScriptUri"];
	}else if((this["djConfig"])&&(djConfig["baseRelativePath"])){
		var root = djConfig["baseRelativePath"];
	}else{
		var root = ".";
		if(!this["djConfig"]){
			djConfig = { baseRelativePath: "." };
		}

		// attempt to figure out the path to dojo if it isn't set in the config
		if((this["document"])&&(this["document"]["getElementsByTagName"])){
			var scripts = document.getElementsByTagName("script");
			var rePkg = /(__package__|dojo)\.js$/i;
			for(var i = 0; i < scripts.length; i++) {
				var src = scripts[i].getAttribute("src");
				if( rePkg.test(src) ) {
					root = src.replace(rePkg, "");
					if(!this["djConfig"]) { djConfig = {}; }
					djConfig["baseScriptUri"] = djConfig["baseRelativePath"] = root;
					break;
				}
			}
		}
	}

	if((this["djConfig"])&&(djConfig["debugAtAllCosts"])&&(!isRhino)){
		tmps.push("browser_debug.js");
	}

	for(var x in tmps){
		var spath = root+"src/"+tmps[x];
		if(isRhino){
			load(spath);
		} else {
			try {
				document.write("<script type='text/javascript' src='"+spath+"'></script>");
			} catch (e) {
				var script = document.createElement("script");
				script.src = spath;
				document.getElementsByTagName("head")[0].appendChild(script);
			}
		}
	}
})();
