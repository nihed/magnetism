// Pop up menu implementation 
// Copyright Red Hat, Inc. 2006

dh.menu = {}

dh.menu.Menu = function() {
    this.exit = function() {
        window.close()
        window.external.application.Exit()
    }

    this.hush = function() {
        window.close()
        window.external.application.Hush()
    }

    this._openSiteLink = function(page) {
        // This is a little tricky ... we don't actually want to 
        // open it in _self, but doing so will cause HippoIE to
        // open in a new window anyways, and in that code path
        // we are a bit smarter about reusing existing windows
        // on our site. If we just did window.open() IE would
        // handle the request directly bypassing us.
        var base = window.external.application.GetServerBaseUrl()
        window.open(base + page, "_self")
    }

    this.showHome = function() {
        window.close()
        this._openSiteLink("")
    }

    this.showRecent = function() {
        window.close()
        window.external.application.ShowRecent()
    }
    
    this.setRecentCount = function(count) {
        var linkCountSpan = document.getElementById("dhRecentLinkCount")
        dh.util.dom.clearNode(linkCountSpan)
        linkCountSpan.appendChild(document.createTextNode(count))
        
        var linkSpan = document.getElementById("dhMenuRecent")
        if (count == 0) {
            linkSpan.className = "dh-menu-link-disabled"
            linkSpan.disabled = true
        }
        else {
            linkSpan.className = "dh-menu-link"
            linkSpan.disabled = false
        }
    }    
    
    // Compute our desired size
    this.resize = function() {
        table = document.getElementById("dhMenuTable")
        window.external.application.Resize(table.offsetWidth + 6, table.offsetHeight + 6)
    }
    
    this.insertActivePost = function(position, post) {
        var menuTableBody = document.getElementById("dhMenuTableBody")
        
        var row = document.createElement("tr")
        row.className = "dh-menu-post-row"

        row.onmouseenter = dh.util.dom.stdEventHandler(function (e) {
            row.className = "dh-menu-post-row dh-menu-post-row-hover"
            return true
        })
        row.onmouseleave = dh.util.dom.stdEventHandler(function (e) {
            row.className = "dh-menu-post-row"
            return true
        })
        row.onclick = dh.util.dom.stdEventHandler(function (e) {
            dh.display._openSiteLink("visit?post=" + post.Id)
            return false
        })

        var titleCell = document.createElement("td")
        titleCell.className = "dh-menu-title-column dh-menu-title-cell"
        row.appendChild(titleCell)
        
        var chattingUserCount = post.ChattingUserCount
        var viewingUserCount = post.ViewingUserCount
        
        var titleDiv = document.createElement("div")
        titleDiv.className = "dh-menu-post-title"
        titleDiv.appendChild(document.createTextNode(post.Title))
        titleCell.appendChild(titleDiv)
        
        var metaCell = document.createElement("td")
        metaCell.className = "dh-menu-meta-column dh-menu-meta-cell"
        row.appendChild(metaCell)
        
        metaCell.appendChild(document.createTextNode("Sent by " + post.Sender.Name + " "))
        
        if (chattingUserCount > 0 || viewingUserCount > 0)  {
            var userCountSpan = document.createElement("span")
            userCountSpan.className = "dh-menu-post-user-count"
            metaCell.appendChild(userCountSpan)
            
            var userCountText
            
            if (chattingUserCount > 0)
                userCountText = "(" + chattingUserCount + " chatting)"
            else if (viewingUserCount > 0)
                userCountText = "(" + chattingUserCount + " looking)"
                
            userCountSpan.appendChild(document.createTextNode(userCountText))
        }

        menuTableBody.insertBefore(row, menuTableBody.childNodes[position + 1])
        
        this.resize()
    }
    
    this.removeActivePost = function(position) {
        var menuTableBody = document.getElementById("dhMenuTableBody")
        menuTableBody.removeChild(menuTableBody.childNodes[position + 1])
        
        this.resize()
    }
}

// *** The parameters below must be kept in sync with HippoMenu.cpp ***

// Global function called immediately after document.write
var dhInit = function(appletUrl) {
    // Set some global parameters
    dh.appletUrl = appletUrl // Base URL to local content files
    dh.display = new dh.menu.Menu()
    dh.display.resize()
}

function dhMenuInsertActivePost(position, post) {
    dh.display.insertActivePost(position, post)
}

function dhMenuRemoveActivePost(position) {
    dh.display.removeActivePost(position)
}

function dhMenuUpdatePost(position, post) {
    dh.display.removeActivePost(position)
    dh.display.insertActivePost(position, post)
}

function dhMenuSetRecentCount(count) {
    dh.display.setRecentCount(count)
}