// Pop up menu implementation
// Copyright Red Hat, Inc. 2006

dh.menu = {}

dh.menu.WIDTH = 200
// Border around the entire menu
dh.menu.BORDER = 1
// Margin around individual items (note that the margins collapse between items)
dh.menu.MARGIN = 3
// Number of rows of standard menu items
dh.menu.NUM_STANDARD = 2

dh.menu.Menu = function() {
    // List of the currently displayed posts; we use this mostly to get references
    // to the post objects when computing our desired size
    this.posts = []

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
        this._openSiteLink("home")
    }

//    this.showHot = function() {
//        window.close()
//        this._openSiteLink("home")
//    }

    this.showRecent = function() {
        window.close()
        window.external.application.ShowRecent()
    }
    
    this.setRecentCount = function(count) {
        var linkCountSpan = document.getElementById("recentLinkCount")
        dh.util.dom.clearNode(linkCountSpan)
        dh.util.debug("recent link count: " + count)
        linkCountSpan.appendChild(document.createTextNode(count))
    }    
    
    // Compute our desired size
    this.resize = function() {
        // Margins and border
        var height = 2 * (dh.menu.MARGIN + dh.menu.BORDER)
        
        // Standard menu items; we assume they are all the same
        var homeDiv = document.getElementById("dhMenuHome")
        height += dh.menu.NUM_STANDARD * homeDiv.clientHeight + (dh.menu.NUM_STANDARD - 1) * dh.menu.MARGIN
        
        // Active posts; again we assume they are all the same
        if (this.posts.length > 0) {
            var post = this.posts[0] 
            var postHeight = post.titleDiv.clientHeight + post.metaDiv.clientHeight
            if (post.image.clientHeight > postHeight)
                postHeight = post.image.clientHeight
                
            height += this.posts.length * (postHeight + dh.menu.MARGIN)
        }
        
        window.external.application.Resize(dh.menu.WIDTH, height)
    }
    
    this.insertActivePost = function(position, id, title, senderName, chattingUserCount, viewingUserCount) {
        var activePostsDiv = document.getElementById("dhActivePosts")
        
        var postDiv = document.createElement("div")
        postDiv.className = "dh-active-post"
        
        postDiv.onclick = dh.util.dom.stdEventHandler(function (e) {
            dh.display._openSiteLink("visit?post=" + id)
            return false
        })
        postDiv.onmouseenter = dh.util.dom.stdEventHandler(function (e) {
            postDiv.className = "dh-active-post dh-active-post-hover"
            return true
        })
        postDiv.onmouseleave = dh.util.dom.stdEventHandler(function (e) {
            postDiv.className = "dh-active-post"
            return true
        })
        
        var post = {}
        
        post.image = dh.util.createPngElement(dh.appletUrl + "groupChat.png", 24, 24)
        post.image.className = "dh-active-post-icon"
        postDiv.appendChild(post.image)
        
        if (chattingUserCount == 0)
            post.image.style.visibility = "hidden"
            
        post.titleDiv = document.createElement("div")
        post.titleDiv.className = "dh-active-post-title"
        post.titleDiv.appendChild(document.createTextNode(title))
        postDiv.appendChild(post.titleDiv)
        
        post.metaDiv = document.createElement("div")
        post.metaDiv.className = "dh-active-post-meta"
        if (chattingUserCount > 1)
            post.metaDiv.appendChild(document.createTextNode("(" + chattingUserCount + ") people chatting right now"))
        else if (chattingUserCount > 0)
            post.metaDiv.appendChild(document.createTextNode("(" + chattingUserCount + ") person chatting right now"))
        else if (viewingUserCount > 1)
            post.metaDiv.appendChild(document.createTextNode("(" + viewingUserCount + ") people looking at this now"))
        else if (viewingUserCount > 0)
            post.metaDiv.appendChild(document.createTextNode("(" + viewingUserCount + ") person looking at this now"))
        else
            post.metaDiv.appendChild(document.createTextNode("Sent by " + senderName))
        postDiv.appendChild(post.metaDiv)

        var before
        if (position < activePostsDiv.childNodes.length)
            before = activePostsDiv.childNodes[position]
        else
            before = null
            
        activePostsDiv.insertBefore(postDiv, before)

        // Fix up widths to deal with limitations of CSS in doing 2-D layout without tables
        // the '4' is a mysterious fudge factor, probably having to do with some extra padding
        // around the floated image
        var textWidth = dh.menu.WIDTH - post.image.offsetWidth - 2 * (dh.menu.MARGIN + dh.menu.BORDER) - 4
        post.titleDiv.style.width = textWidth + "px"
        post.metaDiv.style.width = textWidth + "px"
        
        this.posts.splice(position, 0, post)
        
        this.resize()
    }
    
    this.removeActivePost = function(position) {
        var activePostsDiv = document.getElementById("dhActivePosts")
        activePostsDiv.removeChild(activePostsDiv.childNodes[position])
        
        this.posts.splice(position, 1)
        
        this.resize()
    }
}

// Global function called immediately after document.write
var dhInit = function(appletUrl) {
    // Set some global parameters
    dh.appletUrl = appletUrl // Base URL to local content files
    dh.display = new dh.menu.Menu()
    dh.display.resize()
}

// The parameters must be kept in sync with HippoMenu.cpp
function dhMenuInsertActivePost(position, id, title, senderName, chattingUserCount, viewingUserCount) {
    dh.display.insertActivePost(position, id, title, senderName, chattingUserCount, viewingUserCount)
}

// The parameters must be kept in sync with HippoMenu.cpp
function dhMenuRemoveActivePost(position) {
    dh.display.removeActivePost(position)
}

function dhMenuSetRecentCount(count) {
    dh.display.setRecentCount(count)
}