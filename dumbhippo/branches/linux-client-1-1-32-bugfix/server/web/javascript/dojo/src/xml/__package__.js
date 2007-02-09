dojo.require("dojo.xml.Parse");
dojo.hostenv.conditionalLoadModule({
	common:		["dojo.xml.domUtil"],
    browser: 	["dojo.xml.htmlUtil"],
    svg: 		["dojo.xml.svgUtil"]
});
dojo.hostenv.moduleLoaded("dojo.xml.*");
