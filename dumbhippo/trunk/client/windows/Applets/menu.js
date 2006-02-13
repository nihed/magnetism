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

// The parameters must be kept in sync with HippoMenu.cpp
function dhMenuInsertActivePost(position, id, title, senderName, chattingUserCount) {
    var activePostsDiv = document.getElementById("dhActivePosts")
    
    var postDiv = document.createElement("div")
    postDiv.className = "dh-menu-div"
    
    var postAnchor = document.createElement("a")
    postDiv.appendChild(postAnchor)
    
    postAnchor.className = "dh-menu-link"
    postAnchor.href = window.external.application.GetServerBaseUrl() + "visit?post=" + id
    
    postAnchor.appendChild(document.createTextNode(title))
    
    var before
    if (position < activePostsDiv.childNodes.length)
        before = activePostsDiv.childNodes[position]
    else
        before = null
        
    activePostsDiv.insertBefore(postDiv, before)
}

// The parameters must be kept in sync with HippoMenu.cpp
function dhMenuRemoveActivePost(position) {
    var activePostsDiv = document.getElementById("dhActivePosts")
    activePostsDiv.removeChild(activePostsDiv.childNodes[position])
}
