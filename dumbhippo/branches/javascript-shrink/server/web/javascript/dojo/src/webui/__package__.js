dojo.hostenv.conditionalLoadModule({
	common: ["dojo.xml.Parse", 
			 "dojo.webui.Widget", 
			 "dojo.webui.widgets.Parse", 
			 // "dojo.webui.DragAndDrop", 
			 "dojo.webui.WidgetManager"],
	browser: ["dojo.webui.DomWidget",
			  "dojo.webui.HtmlWidget"],
	svg: 	 ["dojo.webui.SvgWidget"]
});
dojo.hostenv.moduleLoaded("dojo.webui.*");
