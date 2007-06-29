/**
 * $RCSfile$
 * $Revision: 1722 $
 * $Date: 2005-07-28 19:19:16 -0300 (Thu, 28 Jul 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.plugin;

import org.jivesoftware.wildfire.container.Plugin;
import org.jivesoftware.wildfire.container.PluginManager;
import org.jivesoftware.wildfire.user.UserManager;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.jivesoftware.wildfire.user.User;
import org.jivesoftware.wildfire.PresenceManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.Presence;
import org.xmpp.packet.JID;

import java.io.File;

/**
 * Plugin that includes a servlet that provides information about the presence type of the
 * users in the server. For security reasons, the XMPP spec does not allow anyone to see
 * the presence of any user. Only the users that are subscribed to the presence of other
 * users may see their presences.<p>
 *
 * However, in order to make the servlet more useful it is possible to configure this plugin
 * so that anyone or only the users that are subscribed to a user presence may see the presence
 * of other users.<p>
 *
 * Currently, the servlet provides information about user presences in two formats. In XML format
 * or using images.
 *
 * @author Gaston Dombiak
 */
public class PresencePlugin implements Plugin {

    private UserManager userManager;
    private PresenceManager presenceManager;
    private String hostname;

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        XMPPServer server = XMPPServer.getInstance();
        userManager = server.getUserManager();
        presenceManager = server.getPresenceManager();
        hostname = server.getServerInfo().getName();
    }

    public void destroyPlugin() {
        userManager = null;
        presenceManager = null;
    }

    /**
     * Returns true if anyone is allowed to see the presence of other users. False means that
     * only the users that are subscribed to a user presence will be able to get information
     * about the user. By default, presence information is not publicly available.
     *
     * @return true if anyone is allowed to see the presence of other users.
     */
    public boolean isPresencePublic() {
        return JiveGlobals.getBooleanProperty("plugin.presence.public", false);
    }

    /**
     * Sets if anyone is allowed to see the presence of other users. A false value means that
     * only the users that are subscribed to a user presence will be able to get information
     * about the user. By default, presence information is not publicly available.
     *
     * @param presencesPublic if anyone is allowed to see the presence of other users.
     */
    public void setPresencePublic(boolean presencesPublic) {
        JiveGlobals.setProperty("plugin.presence.public", presencesPublic ? "true" : "false");
    }

    /**
     * Returns the presence of the requested user or <tt>null</tt> if the user is offline. If
     * presences are not public then the user presence will be returned if and only if the sender
     * of the request is subscribed to the user presence.
     *
     * @param sender the bare JID of the user making the request.
     * @param jid the bare JID of the entity whose presence is being probed.
     * @return the presence of the requested user.
     * @throws UserNotFoundException If presences are not public and the sender is null or the
     *         sender cannot probe the presence of the requested user. Or if the requested user
     *         does not exist in the local server.
     */
    public Presence getPresence(String sender, String jid) throws UserNotFoundException {
        JID targetJID = new JID(jid);
        // Check that the sender is not requesting information of a remote server entity
        if (targetJID.getDomain() == null || XMPPServer.getInstance().isRemote(targetJID)) {
            throw new UserNotFoundException("Domain does not matches local server domain");
        }
        if (!hostname.equals(targetJID.getDomain())) {
            // Sender is requesting information about component presence
            // TODO Implement this
            throw new UserNotFoundException("Presence of components not supported yet!");
        }
        if (targetJID.getNode() == null) {
            // Sender is requesting presence information of an anonymous user
            throw new UserNotFoundException("Username is null");
        }
        if (!isPresencePublic()) {
            if (sender == null) {
                throw new UserNotFoundException("Sender is null");
            }
            else if (!presenceManager.canProbePresence(new JID(sender), targetJID.getNode())) {
                throw new UserNotFoundException("Sender is not allowed to probe this user");
            }
        }
        User user = userManager.getUser(targetJID.getNode());
        return presenceManager.getPresence(user);
    }
}
