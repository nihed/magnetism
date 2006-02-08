// Pop up menu implementation
// Copyright Red Hat, Inc. 2006

dh.menu = {}

dh.menu.exit = function() {
    window.close()
    window.external.application.Exit()
}

dh.menu.hush = function() {
    window.close()
    window.external.application.Hush()
}

dh.menu._openSiteLink = function(page) {
    var base = window.external.application.GetServerBaseUrl()
    window.open(base + page)
}

dh.menu.showHome = function() {
    window.close()
    this._openSiteLink("/home")
}

dh.menu.showHot = function() {
    window.close()
    this._openSiteLink("/home")
}

dh.menu.showRecent = function() {
    window.close()
    this._openSiteLink("/home")
}
