dojo.require("dojo.uri.*");
dj_eval(dojo.hostenv.getText(new dojo.uri.dojoUri("testtools/JsTestManager/jsunit_wrap.js")));
var _jum = jum;

var jum = {
	isBrowser: true, // so dojo can easily differentiate

	debug: function() {
		var dbg = dojo.hostenv.is_debug_;
		dojo.hostenv.is_debug_ = true;
		dj_debug.apply(dj_global, arguments);
		dojo.hostenv.is_debug_ = dbg;
	},

	assertTrue: function() {
		try {
			_jum.assertTrue.apply(_jum, arguments);
		} catch(e) {
			jum.debug(e.message);
		}
	},

	assertFalse: function() {
		try {
			_jum.assertFalse.apply(_jum, arguments);
		} catch(e) {
			jum.debug(e.message);
		}
	},

	assertEquals: function() {
		try {
			_jum.assertEquals.apply(_jum, arguments);
		} catch(e) {
			jum.debug(e.message);
		}
	}
};

