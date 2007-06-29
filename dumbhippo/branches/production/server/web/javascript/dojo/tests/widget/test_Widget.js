dojo.require("dojo.widget.Widget");

function test_widget_ctor(){
	jum.debug("in widget.ctor");
	var obj1 = new dojo.widget.Widget();

	jum.assertTrue("test1", typeof obj1 == "object");
//	jum.assertTrue("test2", obj1.widgetType == "Widget");
}
