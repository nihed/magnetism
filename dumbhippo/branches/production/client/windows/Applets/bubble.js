// bubble.js: Shared handling of bubbles that display notifications to the
//   user of posts, MySpace comments ,and so forth
// Copyright Red Hat, Inc. 2006

dh.bubble = {}

//////////////////////////////////////////////////////////////////////////////
// Generic display code for a single notification bubble
//////////////////////////////////////////////////////////////////////////////
    
// Extra amount of height to add when we are using the list images rather than 
// the standalone images. The difference here is the difference between a 3
// pixel and 5 pixel white border for the top and bottom of the bubble
    
// Create a new bubble object
// @param isStandaloneBubble if true, then this is the standalone notification bubble,
//        and should get the navigation arrows and close button, and the appropriate
//        images for the sides.
dh.bubble.Bubble = function(isStandaloneBubble) {
    // Whether to include the quit button and previous/next arrows
    this._isStandaloneBubble = isStandaloneBubble

    // The notification currently being displayed
    this._data = null
    
    // The "page" of the swarm area
    this._page = null
    
    ///////////// Callbacks /////////////
    
    // After creating the Bubble object, the caller can assign to these fields
    // to override the default empty handling with real callbacks
    
    // Called when the close button is clicked
    this.onClose = function() {}
    
    // Called when the 'next' arrow is clicked
    this.onNext = function() {}
    
    // Called when the 'previous' arrow is clicked
    this.onPrevious = function() {}
    
    // Called when the user clicks on a DumbHippo post link
    // @param postId the ID of the post that was clicked on
    // @param url the URL of the link that was shared with the post (this is not
    //      the URL we take the user to; we redirect them through our
    //      site instead.)
    this.onLinkClick = function(postId, url) {}
    
    // Called when the size of the bubble changes
    this.onSizeChange = function() {}
    
    // Called when the display of the bubble should be updated (without a size change)
    this.onUpdateDisplay = function() {}
    
    // Build the DOM tree for the bubble
    // @return the DOM node of the top node for the bubble
    this.create = function() {
        function append(parent, elementName, className) {
            var element = document.createElement(elementName)
            element.setAttribute("className", className)
            parent.appendChild(element)
            
            return element
        }
        function appendDiv(parent, className) {
            return append(parent, "div", className)
        }
        function createDecoratedDiv(className) {
            var div = document.createElement("div")
            div.setAttribute("className", className)
            div.tl = appendDiv(div, className + "-tl dh-tl")
            div.tr = appendDiv(div, className + "-tr dh-tr")
            div.bl = appendDiv(div, className + "-bl dh-bl")
            div.br = appendDiv(div, className + "-br dh-br")
            appendDiv(div, className + "-t dh-t")
            appendDiv(div, className + "-b dh-b")
            appendDiv(div, className + "-l dh-l")
            appendDiv(div, className + "-r dh-r")
            
            return div
        }
        function appendDecoratedDiv(parent, className) {
            var div = createDecoratedDiv(className)
            parent.appendChild(div)
            
            return div
        }
    
        var bubble = this  // for callback closures

        this._topDiv = createDecoratedDiv("dh-notification-top")
        
        if (this._isStandaloneBubble) {
            var navDiv = appendDiv(this._topDiv, "dh-notification-navigation")
                this._navText = appendDiv(navDiv, "dh-notification-navigation-text")
                this._navButtons = appendDiv(navDiv, "dh-notification-navigation-buttons")
            appendDiv(this._topDiv, "dh-notification-shadow")
        }

        var mainDiv = appendDiv(this._topDiv, "dh-notification-main")
        
        this._colorDiv = appendDecoratedDiv(mainDiv, "dh-notification-color")
        var colorDiv = this._colorDiv
                // The absolutely positioned corner divs vanish unless we add this div
                // here; it isn't one of the well-known IE bugs, but probably a more
                // obscure bug; the div is styled to 0 width/height
                var hackDiv = appendDiv(colorDiv, "dh-fix-position-absolute-hack")
                hackDiv.appendChild(document.createTextNode(" "))
                
                var leftSide = appendDiv(colorDiv, "dh-notification-leftside")
                    this._photoDiv = appendDiv(leftSide, "dh-notification-photo-div")
                    this._photoLinkDiv = appendDiv(leftSide, "dh-notification-photolink")
        
                this._rightsideDiv = appendDiv(colorDiv, "dh-notification-rightside")
                    if (this._isStandaloneBubble)
                        this._headerDiv = appendDiv(this._rightsideDiv, "dh-notification-logo")
                    this._titleDiv = appendDiv(this._rightsideDiv, "dh-notification-title")
                    this._bodyDiv = appendDiv(this._rightsideDiv, "dh-notification-body")
                    
                var metaOuterDiv = appendDiv(colorDiv, "dh-notification-meta-outer")
                    this._metaSpan = append(metaOuterDiv, "span", "dh-notification-meta")
                    
                appendDiv(colorDiv, "dh-clear")
                // Standard IE hack to fix up for off-by-one positioning of bottom/right floats
                appendDiv(colorDiv, "dh-notification-color-whiteout")
                
                if (this._isStandaloneBubble) {
                    this._closeButton = appendDiv(this._rightsideDiv, "dh-tr dh-close-button")
                    this._closeButton.onclick = dh.util.dom.stdEventHandler(function (e) {
                        bubble.onClose();
                        e.stopPropagation();
                        return false;
                    })
                }
        
            this._swarmNavDiv = appendDiv(mainDiv, "dh-notification-swarm-nav")
            this._swarmDiv = appendDiv(mainDiv, "dh-notification-swarm")
        
        
        return this._topDiv
    }
    
    // Set the notification that this bubble is currently displaying.
    // @param data object, such as a dh.bubble.PostData or dh.bubble.MySpaceData
    //    While there isn't actually a base class, all data objects must export
    //    a set of methods forming a common interface.
    this.setData = function(data) {
        this._data = data
        this._render()
    }
    
    // Update the navigation arrows
    // @param position position of current notification in a list of notifications (first is 0)
    // @param numNotifications total number of notifications
    this.updateNavigation = function(position, numNotifications) {
        if (!this._isStandaloneBubble)
            return
    
        var bubble = this // for callback closures
        
        dh.util.dom.replaceContents(this._navText, document.createTextNode((position + 1) + " of " + numNotifications))
        
        dh.util.dom.clearNode(this._navButtons)
        
        var button = document.createElement("span")
        if (position == 0)
            button.className = "dh-notification-arrow dh-notification-left-arrow-inactive"
        else
            button.className = "dh-notification-arrow dh-notification-left-arrow-active"
        this._navButtons.appendChild(button)
        button.onclick = dh.util.dom.stdEventHandler(function (e) {
            bubble.onPrevious()
            return false;
        })
        
        button = document.createElement("span")
        if (position == numNotifications - 1)
            button.className = "dh-notification-arrow dh-notification-right-arrow-inactive"
        else
            button.className = "dh-notification-arrow dh-notification-right-arrow-active"
        this._navButtons.appendChild(button)
        button.onclick = dh.util.dom.stdEventHandler(function (e) {
            bubble.onNext()
            return false;
        })
    }
    
    // Set which "page" we are currently showing in the bottom tab; the
    // @param page the name of the page ("whosThere", "someoneSaid", etc.)
    this.setPage = function(page) {
        this._page = page
        if (this._swarmPages && this._swarmPages.length > 0)
            this._updateSwarmPage()
                        
        this.onSizeChange()
        this.onUpdateDisplay()
    }
    
    // Helper function for data object display routines; create a link to a DumbHippo post
    this.createSharedLinkLink = function(linkTitle, postId, url) {
        var a = document.createElement("a")
        a.setAttribute("href", "javascript:true")
        
        var bubble = this
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            bubble.onLinkClick(postId, url)
            return false;
        })
        dh.util.dom.appendSpanText(a, linkTitle, "dh-notification-link-title")
        return a
    }

    // Helper function for data object display routines; render a list of recipients
    // @param node parent node
    // @param arr array of recipients
    // @param normalCssClass CSS class to use for display of a different user
    // @param cssCssClass CSS class to use for display of the current user
    this.renderRecipients = function (node, arr, normalCssClass, selfCssClass) {
        for (var i = 0; i < arr.length; i++) {
            var recipientNode = this.renderPerson(arr[i], normalCssClass, selfCssClass)
            node.appendChild(recipientNode)
            if (i < arr.length - 1) {
                node.appendChild(document.createTextNode(", "))
            }
        }  
    }
    
    // Get the width of the bubble's desired area
    // @return the desired width, in pixels
    this.getWidth = function() {
        return this._topDiv.offsetWidth
    }

    // Get the height of the bubble's desired area
    // @return the desired height, in pixels
    this.getHeight = function() {
        var height = this._topDiv.offsetHeight
        if (height % 2 == 1) // force even, because of IE off-by-one bugs in positioning of the bottom elements
           height -= 1
        return height
    }

    // Render a person
    this.renderPerson = function (person, normalCssClass, selfCssClass) {  
        dh.util.debug("rendering person with id=" + person.Id + ", name=" + person.Name)
        var cssClass = normalCssClass
        var name = person.Name
        if (person.Id == dh.selfId) {
            name = "you"
            cssClass = selfCssClass
        }
        var node = document.createElement("span")
        node.setAttribute("className", cssClass)
        node.appendChild(document.createTextNode(name))
        return node
    }
     
    this.createPngElement = function(src) {
        var img = document.createElement("div")
        img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale')"
        
        return img
    }

    // Update the image for the photo on the left of the bubble
    this._setPhotoImage = function (src, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photo")
        img = this.createPngElement(src)
        img.setAttribute("className", "dh-notification-photo")
        a.appendChild(img)
        dh.util.dom.replaceContents(this._photoDiv, a)
    }
    
    // Update the title underneath the photo on the left
    this._setPhotoTitle = function (text, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photolink")
        a.appendChild(document.createTextNode(text))
        dh.util.dom.replaceContents(this._photoLinkDiv, a)
    }
    
    // Update the contents of the bubble to match this._data
    this._render = function() {
        this._setPhotoImage(this._data.getPhotoSrc(), this._data.getPhotoLink())
        this._setPhotoTitle(this._data.getPhotoTitle(), this._data.getPhotoLink())
        
        if (this._isStandaloneBubble) {
            dh.util.swapLastCssClass(this._headerDiv, "dh-notification-logo-", this._data.getCssClassSuffix())
            dh.util.swapLastCssClass(this._closeButton, "dh-close-button-", this._data.getCssClassSuffix()) 
        }
           
        dh.util.swapLastCssClass(this._colorDiv, "dh-notification-color-", this._data.getCssClassSuffix())   
                
        dh.util.swapLastCssClass(this._colorDiv.tl, "dh-notification-color-tl-", this._data.getCssClassSuffix())
        dh.util.swapLastCssClass(this._colorDiv.bl, "dh-notification-color-bl-", this._data.getCssClassSuffix())
        dh.util.swapLastCssClass(this._colorDiv.br, "dh-notification-color-br-", this._data.getCssClassSuffix())                                
        
        dh.util.dom.clearNode(this._titleDiv)
        this._data.appendTitleContent(this, this._titleDiv)

        dh.util.dom.clearNode(this._bodyDiv)
        this._data.appendBodyContent(this, this._bodyDiv)
        
        dh.util.dom.clearNode(this._metaSpan)
        this._data.appendMetaContent(this, this._metaSpan)

        dh.util.dom.clearNode(this._swarmDiv)
        this._swarmPages = this._data.appendSwarmContent(this, this._swarmDiv)
        
        if (this._swarmPages.length > 0) {
            this._updateSwarmPage()
            this._setSwarmDisplay(true)
        } else {
            this._setSwarmDisplay(false)
        }
        
        this._fixupLayout()
        this.onSizeChange()
        this.onUpdateDisplay()
    }
    
    // Adjust various sizes that we can't make the CSS handle, nothing for now
    this._fixupLayout = function() {
    }
    
    // Set whether the viewer bubble is currently showing
    this._setSwarmDisplay = function(swarmDisplay) {
        swarmDisplay = !!swarmDisplay
        if (this._swarmDisplay != swarmDisplay) {
            this._swarmDisplay = swarmDisplay
            
            if (swarmDisplay) {
                this._swarmDiv.style.display = "block"
                this._swarmNavDiv.style.display = "block"
            } else {
                this._swarmDiv.style.display = "none"
                this._swarmNavDiv.style.display = "none"
            }
        }
    }   
    
    // Update the currently showing page and the navigation links for the swarm area
    this._updateSwarmPage = function() {
        var bubble = this
        dh.util.dom.clearNode(this._swarmNavDiv)
        var activePage = null
        for (i = 0; i < this._swarmPages.length; i++) {
            var page = this._swarmPages[i]
            if (page.name == this._page) {
                activePage = page
            }
        }
        
        if (activePage == null) {
            activePage = this._swarmPages[0]
        }
        
        for (i = 0; i < this._swarmPages.length; i++) {
            var page = this._swarmPages[i]
            page.div.style.display = (page == activePage) ? "block" : "none"
            var link = this._createSwarmNavLink(page, page == activePage)
            this._swarmNavDiv.appendChild(link)
            if (i < this._swarmPages.length - 1)
                this._swarmNavDiv.appendChild(document.createTextNode(" | "));
        }
        var swarmControls = document.createElement("span")
        this._swarmNavDiv.appendChild(swarmControls)
        swarmControls.className = "dh-notification-swarm-controls"
        
        var createControl = function (iconName, text, handler, tooltip) {
            var span = document.createElement("span")
            span.className = "dh-notification-swarm-control"
            var img = dh.util.createPngElement(dh.appletUrl + iconName, 10, 1)
            span.appendChild(img)
            img.className = "dh-notification-swarm-control-img"
            if (handler != null) {            
                var eventCallback = dh.util.dom.stdEventHandler(function (e) {
                    handler()
                    return false
                })
                img.onclick = eventCallback
            }
            var link;
            if (handler != null)
                link = document.createElement("a")
            else
                link = document.createElement("span")
            span.appendChild(link)
            if (handler != null) {
                link.href = "javascript:true"
                link.onclick = eventCallback
                link.className = "dh-notification-swarm-nav-link"
            } else {
                link.className = "dh-notification-swarm-nav-link-current"
            }
            if (tooltip)
                span.title = tooltip
            link.appendChild(document.createTextNode(text))
            return span      
        }
        
        if (this._data.canInvite()) {
            var inviteDoneControl = createControl("add.png", "Invited", null)
            var inviteControl = createControl("add.png", "Invite",
                               function () {
                                 bubble._data.doInvite()
                                 swarmControls.replaceChild(inviteDoneControl, inviteControl)
                                 bubble.onNext();
                               },
                               "Invite this person to join the group")
            swarmControls.appendChild(inviteControl)
        }
        var chatControl = createControl("chaticon.gif", "Join chat", 
                        function () { window.external.application.ShowChatWindow(bubble._data.getChatId()) });
        var chatCount = document.createElement("span")
        chatCount.appendChild(document.createTextNode(" [" + this._data.getChattingUserCount() + "]"))
        chatControl.appendChild(chatCount)
        swarmControls.appendChild(chatControl)
        var alreadyIgnoredControl = createControl("ignoreicon.png", this._data.getIgnoreText().ignored, null)        
        var ignoreControl = createControl("ignoreicon.png", this._data.getIgnoreText().ignore, 
                        function () { 
                                      bubble._data.setIgnored();
                                      swarmControls.replaceChild(alreadyIgnoredControl, ignoreControl)
                                      bubble.onNext();
                                      },
                                      this._data.getIgnoreText().tooltip);
        ignoreControl.appendChild(document.createTextNode(this._data.getIgnoreText().details))
        if (!this._data.getIgnored())
            swarmControls.appendChild(ignoreControl)
        else
            swarmControls.appendChild(alreadyIgnoredControl)
    }
    
    // Create a link that goes in the navigation area
    this._createSwarmNavLink = function(page, isActive) {
        var link
        
        if (isActive) {
            link = document.createElement("span")
            link.className = "dh-notification-swarm-nav-link-current"
        } else {
            link = document.createElement("a")
            link.className = "dh-notification-swarm-nav-link"
            link.href = "javascript:void(0)"
            var bubble = this
            var pageName = page.name
            link.onclick = function() { bubble.setPage(pageName) }
        }

        link.appendChild(document.createTextNode(page.title))
        
        return link
    }
}

//////////////////////////////////////////////////////////////////////////////
// Notification data object for DumbHippo posts
//////////////////////////////////////////////////////////////////////////////
    
dh.bubble.BubbleData = function() {
    this.getId = function() {
        throw Error("not implemented");
    }
    
    this.getChatId = function () {
        return this.getId()
    }
    
    this.getTimeout = function() {
        throw Error("not implemented"); 
    }
    
    this.getBubbleType = function() {
        throw Error("not implemented"); 
    }
    
    this.getChattingUserCount = function() {
        return -1
    }
    
    this.getIgnoreText = function () {
        return {ignore: "Ignore", details: "", ignored: "Ignored", tooltip: "" }
    }
    
    this.getIgnored = function() {
        throw Error("not implemented"); 
    }
    
    this.getPhotoLink = function() {
        throw Error("not implemented");
    }
    
    this.getPhotoSrc = function() {
        throw Error("not implemented");
    }
    
    this.getPhotoTitle = function() {
        throw Error("not implemented");
    }
    
    this.getCssClassSuffix = function() {
        throw Error("not implemented");
    }
    
    this.appendTitleContent = function(bubble, parent) {
        throw Error("not implemented");
    }
        
    this.appendBodyContent = function(bubble, parent) {
        throw Error("not implemented");
    }
    
    this.appendMetaContent = function(bubble, parent) {
        throw Error("not implemented");
    }
            
    this.appendSwarmContent = function(bubble, parent) {
        throw Error("not implemented");
    }
    
    this.canInvite = function() {
        return false;
    }
    
    this.doInvite = function() {
        throw Error("not implemented");
    }
    
    this.setIgnored = function() {
        throw Error("not implemented");        
    }

}    

dh.core.inherits(dh.bubble.BubbleData, Object)
 
// Extension point for specific post types
dh.bubble.postExtensions = {}
    
dh.bubble.PostData = function(post) {
    
    dh.bubble.BubbleData.call(this)
    this.post = post
    var postInfo = post.Info
    if (postInfo != null && !postInfo.match(/^\s*$/)) {
        this.info = dh.parseXML(postInfo)
    } else {
        this.info = null
    }

    this.getId = function() {
        return this.post.Id
    }
    
    this.getTimeout = function() {
        return this.post.Timeout
    }
    
    this.getBubbleType = function() {
        return 'linkShare'
    }
    
    this.getChattingUserCount = function() {
        return this.post.ChattingUserCount
    }
    
    this.getIgnored = function() {
        return this.post.Ignored
    }
    
    this.getPhotoLink = function() {
        return this.post.Sender.HomeUrl
    }
    
    this.getPhotoSrc = function() {
        return dh.serverUrl + this.post.Sender.SmallPhotoUrl
    }
    
    this.getPhotoTitle = function() {
        return this.post.Sender.Name
    }
    
    this.getCssClassSuffix = function() {
        return "link-swarm"
    }
    
    this.appendTitleContent = function(bubble, parent) {
        var a = bubble.createSharedLinkLink(this.post.Title, this.post.Id, this.post.Url)
        parent.appendChild(a)
    }
        
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.post.Description))
        
        for (extension in dh.bubble.postExtensions) {
            var ext = dh.bubble.postExtensions[extension]
            if (ext.accept(this))
                ext.drawContent(this, bubble._bodyDiv)
        }
    }
    
    this.appendMetaContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode("Sent to "))
       
        var personRecipients = []
        var groupRecipients = []        
        var i;
        var recipients = this.post.Recipients
        for (i = 0; i < recipients.length; i++) {
            var recipient = recipients.item(i)
            var type = recipient.type
            if (type == 2) // GROUP
                groupRecipients.push(recipient)
            else
                personRecipients.push(recipient)
        }
        
        if (this.post.ToWorld) {
            dh.util.dom.appendSpanText(parent, "The World", "dh-notification-the-world")
            if (personRecipients.length > 0 || groupRecipients.length > 0)
                parent.appendChild(document.createTextNode(", "))
        }
        
        // FIXME this is all hostile to i18n
        bubble.renderRecipients(parent, groupRecipients, "dh-notification-group-recipient")
        if (personRecipients.length > 0 && groupRecipients.length > 0) {
            parent.appendChild(document.createTextNode(", "))
        }        
        bubble.renderRecipients(parent, personRecipients, "dh-notification-recipient", "dh-notification-self-recipient")
    }
            
    this.appendSwarmContent = function(bubble, parent) {
        var pages = []
        
        if (this.post.ToWorld && post.TotalViewers > 0) {
            var viewersCountDiv = document.createElement("div")
            viewersCountDiv.className  = "dh-notification-whos-there"
            parent.appendChild(viewersCountDiv)
            var viewersCount = this.post.TotalViewers
            var text = viewersCount > 1 ? "people viewed this share" : "person viewed this share"
            dh.util.dom.appendSpanText(viewersCountDiv, "" + viewersCount + " " + text, "dh-notification-viewer-count")
            
            pages.push({ name: "viewers", title: "Viewers", div: viewersCountDiv })
        }
        
        // Only add the Who's There if we have something interesting to show
        if (this.post.CurrentViewers.length > 0
            && !(this.post.CurrentViewers.length == 1 && this.post.CurrentViewers.item(0).Id == dh.selfId)) {
            var whosThereDiv = document.createElement("div")
            whosThereDiv.className  = "dh-notification-whos-there"
            parent.appendChild(whosThereDiv)
        
            var viewersArray = []
            var viewers = this.post.CurrentViewers
            for (var i = 0; i < viewers.length; i++) {
                viewersArray.push(viewers.item(i))
            }
            bubble.renderRecipients(whosThereDiv, viewersArray, "dh-notification-viewer", "dh-notification-self-viewer")
            
            pages.push({ name: "whosThere", title: "Who's there", div: whosThereDiv })
        }
        
        if (this.post.LastChatSender != null) {
            var someoneSaidDiv = document.createElement("div")
            someoneSaidDiv.className  = "dh-notification-someone-said"
            parent.appendChild(someoneSaidDiv)
            
            var senderPhoto = bubble.createPngElement(dh.serverUrl + this.post.LastChatSender.SmallPhotoUrl)
            senderPhoto.className = "dh-notification-chat-sender-photo"
            someoneSaidDiv.appendChild(senderPhoto)
            
            var messageSpan = document.createElement("span")
            messageSpan.className = "dh-notification-chat-message"
            messageSpan.appendChild(document.createTextNode('"' + this.post.LastChatMessage + '"'))
            someoneSaidDiv.appendChild(messageSpan)            
            
            var sender = this.post.LastChatSender
            var senderDiv = document.createElement("div")
            senderDiv.className = "dh-notification-chat-sender"
            senderDiv.appendChild(bubble.renderPerson(sender, "dh-notification-sender"))
            someoneSaidDiv.appendChild(senderDiv)
            
            pages.push({ name: "someoneSaid", title: "Recent Comments", div: someoneSaidDiv })
        }
        
        return pages
    }
    
    this.setIgnored = function() {
        window.external.application.IgnorePost(this.getId());
    }
}

dh.core.inherits(dh.bubble.PostData, dh.bubble.BubbleData)
    
dh.bubble.GroupData = function(group) {
  
    dh.bubble.BubbleData.call(this)
    
    this.group = group

    this.getTimeout = function() {
        // 0 signifies to use the default timeout
        return 0
    }

    this.getBubbleType = function() {
        return 'groupUpdate'
    }
            
    this.getChattingUserCount = function() {
        return this.group.ChattingUserCount
    }
        
    this._getGroupLink = function() {
        return dh.serverUrl + "group?who=" + this.group.Id
    }

    this.getPhotoLink = function() {
        return this._getGroupLink()
    }
    
    this.getPhotoSrc = function() {
        return dh.serverUrl + this.group.SmallPhotoUrl
    }
    
    this.getPhotoTitle = function() {
        return ""
    }

    this.getCssClassSuffix = function() {
        return "group-update"
    }
       
    this.appendTitleContent = function(bubble, parent) {
        var a = document.createElement("a")
        a.appendChild(document.createTextNode(this.group.Name))
        a.setAttribute("href", this._getGroupLink())
        parent.appendChild(a)
    }
    
    this.appendMetaContent = function(bubble, parent) {
    }
}

dh.core.inherits(dh.bubble.GroupData, dh.bubble.BubbleData)

dh.bubble.GroupChatData = function(group) {
    dh.bubble.GroupData.call(this, group)

    this.getId = function() {
        return this.group.Id
    }
    
    this.getIgnored = function() {
        return this.group.ChatIgnored
    }
    
    this.getIgnoreText = function () {
        return {ignore: "Hush Chat", details: " (2hrs)", ignored: "Hushed", tooltip: "Hush group chat notifications for 2 hours" }
    }
    
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode("New chat activity."));
    }
          
    this.appendSwarmContent = function(bubble, parent) {
        var pages = []

        if (this.group.ChattingUsers.length > 0) {
            var whosThereDiv = document.createElement("div")
            whosThereDiv.className  = "dh-notification-whos-there"
            parent.appendChild(whosThereDiv)
        
            var chattingUsersArray = []
            var chattingUsers = this.group.ChattingUsers
            for (var i = 0; i < chattingUsers.length; i++) {
                chattingUsersArray.push(chattingUsers.item(i))
            }
            bubble.renderRecipients(whosThereDiv, chattingUsersArray, "dh-notification-viewer", "dh-notification-self-viewer")
            
            pages.push({ name: "whosThere", title: "Who's there", div: whosThereDiv })
        }

        if (this.group.LastChatSender != null) {                
            var someoneSaidDiv = document.createElement("div")
            someoneSaidDiv.className  = "dh-notification-someone-said"
            parent.appendChild(someoneSaidDiv)
            
            var senderPhoto = bubble.createPngElement(dh.serverUrl + this.group.LastChatSender.SmallPhotoUrl)
            senderPhoto.className = "dh-notification-chat-sender-photo"
            someoneSaidDiv.appendChild(senderPhoto)
            
            var messageSpan = document.createElement("span")
            messageSpan.className = "dh-notification-chat-message"
            messageSpan.appendChild(document.createTextNode('"' + this.group.LastChatMessage + '"'))
            someoneSaidDiv.appendChild(messageSpan)            
            
            var sender = this.group.LastChatSender
            var senderDiv = document.createElement("div")
            senderDiv.className = "dh-notification-chat-sender"
            senderDiv.appendChild(bubble.renderPerson(sender, "dh-notification-sender"))
            someoneSaidDiv.appendChild(senderDiv)
            
            pages.push({ name: "someoneSaid", title: "Recent Comments", div: someoneSaidDiv })
        }
        
        return pages
    }
    
     this.setIgnored = function() {    
        window.external.application.IgnoreChat(this.group.Id);
    }
}

dh.core.inherits(dh.bubble.GroupChatData, dh.bubble.GroupData)

dh.bubble.GroupMembershipChangeData = function(group, user, status) {
    dh.bubble.GroupData.call(this, group)
    
    this.user = user
    this.status = status
    this.description = ""
    this.swarmTitle = ""
    this._showInvite = false
    
    if (this.status == "FOLLOWER") {
        this._showInvite = true
        this.description = "There is a new group follower."
        this.swarmTitle = "New Follower"
    } else if (this.status == "ACTIVE") {
        this.description = "There is a new group member."
        this.swarmTitle = "New Member"
    }
           
    this.getId = function() {
        // different group membership changes are treated as differnet activities,
        // so we do not want the previous bubbles with membership changes to be replaced, 
        // and this id is used as a key in notification.js for bubble identity
        return this.group.Id + "-" + this.user.Id
    }
        
    this.getChatId = function () {
        return this.group.Id
    }
    
    this.getIgnored = function() {
        return this.group.Ignored
    }    
    
    this.getIgnoreText = function () {
        return {ignore: "Hush Updates", details: " (2hrs)", ignored: "Hushed", tooltip: "Hush group membership update notifications for 2 hours" }
    }
    
    this.appendBodyContent = function(bubble, parent) {
       parent.appendChild(document.createTextNode(this.description))
    }
                
    this.appendSwarmContent = function(bubble, parent) {
        var pages = []

        // TODO replace someone-said with something more applicable
         
        var someoneSaidDiv = document.createElement("div")
        someoneSaidDiv.className  = "dh-notification-someone-said"
        parent.appendChild(someoneSaidDiv)
        
        var senderPhoto = bubble.createPngElement(dh.serverUrl + this.user.SmallPhotoUrl)
        senderPhoto.className = "dh-notification-chat-sender-photo"
        someoneSaidDiv.appendChild(senderPhoto)
        
        var messageSpan = document.createElement("span")
        messageSpan.className = "dh-notification-chat-message"
        var a = document.createElement("a")
        a.appendChild(document.createTextNode(this.user.Name))
        a.setAttribute("href", this.user.HomeUrl)
        messageSpan.appendChild(a)
        someoneSaidDiv.appendChild(messageSpan)            
        
        pages.push({ name: "someoneSaid", title: this.swarmTitle, div: someoneSaidDiv })
    
        return pages
    }

     this.setIgnored = function() {    
        window.external.application.IgnoreEntity(this.group.Id);
    }
    
    this.canInvite = function() {
        return this._showInvite
    }
    
    this.doInvite = function() {
        window.external.application.DoGroupInvite(this.group.Id, this.user.Id)
    }    
}

dh.core.inherits(dh.bubble.GroupMembershipChangeData, dh.bubble.GroupData)

// dh.bubble.GroupMembershipChangeData.prototype = new dh.bubble.GroupData()
// dh.bubble.GroupMembershipChangeData.prototype.constructor = dh.bubble.GroupMembershipChangeData.init
    
//////////////////////////////////////////////////////////////////////////////
// Notification data object for MySpace blog comments
//////////////////////////////////////////////////////////////////////////////
    
// Create a new MySpace blog comment object
// @param myId ID of the current user (the user owning the blog)
// @param blogId ID of the blog *post* that was commented on
// @param commentId ID of the new comment on the post
// @param posterName Name of the comment poster
// @param posterImgUrl URL to a photo of the comment poster
// @param content the content of the post
dh.bubble.MySpaceData = function(myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    this.myId = myId,
    this.blogId = blogId
    this.commentId = commentId
    this.posterId = posterId
    this.posterName = posterName
    this.posterImgUrl = posterImgUrl
    this.content = content
    
    this.getPhotoLink = function() {
        return "http://myspace.com/" + this.posterId
    }
    
    this.getPhotoSrc = function() {
        return this.posterImgUrl
    }
    
    this.getPhotoTitle = function() {
        return this.posterName
    }

    this.appendTitleContent = function(bubble, parent) {
        var a = document.createElement("a")
        a.appendChild(document.createTextNode("New MySpace comment from " + this.posterName))
        a.setAttribute("href", "http://blog.myspace.com/index.cfm?fuseaction=blog.view&friendID=" + this.myId + "&blogID=" + this.blogId)
        
        parent.appendChild(a)
    }
            
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.content))
    }
    
    this.appendMetaContent = function(bubble, parent) {
    }
    
    this.appendSwarmContent = function(bubble, parent) {
        return []
    }
}

