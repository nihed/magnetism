/**
 * $RCSfile$
 * $Revision: 1751 $
 * $Date: 2005-08-07 19:08:47 -0400 (Sun, 07 Aug 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.roster;

import org.jivesoftware.wildfire.user.UserAlreadyExistsException;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Defines the provider methods required for creating, reading, updating and deleting roster
 * items.<p>
 *
 * Rosters are another user resource accessed via the user or chatbot's long ID. A user/chatbot
 * may have zero or more roster items and each roster item may have zero or more groups. Each
 * roster item is additionaly keyed on a XMPP jid. In most cases, the entire roster will be read
 * in from memory and manipulated or sent to the user. However some operations will need to retrive
 * specific roster items rather than the entire roster.
 *
 * @author Iain Shigeoka
 */
public class DefaultRosterItemProvider implements RosterItemProvider {

    private static final String CREATE_ROSTER_ITEM =
            "INSERT INTO jiveRoster (username, rosterID, jid, sub, ask, recv, nick) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_ROSTER_ITEM =
            "UPDATE jiveRoster SET sub=?, ask=?, recv=?, nick=? WHERE rosterID=?";
    private static final String DELETE_ROSTER_ITEM_GROUPS =
            "DELETE FROM jiveRosterGroups WHERE rosterID=?";
    private static final String CREATE_ROSTER_ITEM_GROUPS =
            "INSERT INTO jiveRosterGroups (rosterID, rank, groupName) VALUES (?, ?, ?)";
    private static final String DELETE_ROSTER_ITEM =
            "DELETE FROM jiveRoster WHERE rosterID=?";
    private static final String LOAD_USERNAMES =
            "SELECT DISTINCT username from jiveRoster WHERE jid=?";
    private static final String COUNT_ROSTER_ITEMS =
            "SELECT COUNT(rosterID) FROM jiveRoster WHERE username=?";
     private static final String LOAD_ROSTER =
             "SELECT jid, rosterID, sub, ask, recv, nick FROM jiveRoster WHERE username=?";
    private static final String LOAD_ROSTER_ITEM_GROUPS =
            "SELECT groupName FROM jiveRosterGroups WHERE rosterID=? ORDER BY rank";
    
    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.roster.RosterItemProvider#createItem(java.lang.String, org.jivesoftware.wildfire.roster.RosterItem)
     */
    public RosterItem createItem(String username, RosterItem item)
            throws UserAlreadyExistsException
    {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();

            long rosterID = SequenceManager.nextID(JiveConstants.ROSTER);
            pstmt = con.prepareStatement(CREATE_ROSTER_ITEM);
            pstmt.setString(1, username);
            pstmt.setLong(2, rosterID);
            pstmt.setString(3, item.getJid().toBareJID());
            pstmt.setInt(4, item.getSubStatus().getValue());
            pstmt.setInt(5, item.getAskStatus().getValue());
            pstmt.setInt(6, item.getRecvStatus().getValue());
            pstmt.setString(7, item.getNickname());
            pstmt.executeUpdate();

            item.setID(rosterID);
            insertGroups(rosterID, item.getGroups().iterator(), con);
        }
        catch (SQLException e) {
            throw new UserAlreadyExistsException(item.getJid().toBareJID());
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
        return item;
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.roster.RosterItemProvider#updateItem(java.lang.String, org.jivesoftware.wildfire.roster.RosterItem)
     */
    public void updateItem(String username, RosterItem item) throws UserNotFoundException {
        Connection con = null;
        PreparedStatement pstmt = null;
        long rosterID = item.getID();
        try {
            con = DbConnectionManager.getConnection();
            // Update existing roster item
            pstmt = con.prepareStatement(UPDATE_ROSTER_ITEM);
            pstmt.setInt(1, item.getSubStatus().getValue());
            pstmt.setInt(2, item.getAskStatus().getValue());
            pstmt.setInt(3, item.getRecvStatus().getValue());
            pstmt.setString(4, item.getNickname());
            pstmt.setLong(5, rosterID);
            pstmt.executeUpdate();
            // Close now the statement (do not wait to be GC'ed)
            pstmt.close();

            // Delete old group list
            pstmt = con.prepareStatement(DELETE_ROSTER_ITEM_GROUPS);
            pstmt.setLong(1, rosterID);
            pstmt.executeUpdate();

            insertGroups(rosterID, item.getGroups().iterator(), con);

        }
        catch (SQLException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.roster.RosterItemProvider#deleteItem(java.lang.String, long)
     */
    public void deleteItem(String username, long rosterItemID) {
        // Only try to remove the user if they exist in the roster already:
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            // Remove roster groups
            pstmt = con.prepareStatement(DELETE_ROSTER_ITEM_GROUPS);

            pstmt.setLong(1, rosterItemID);
            pstmt.executeUpdate();
            // Close now the statement (do not wait to be GC'ed)
            pstmt.close();

            // Remove roster
            pstmt = con.prepareStatement(DELETE_ROSTER_ITEM);

            pstmt.setLong(1, rosterItemID);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.roster.RosterItemProvider#getUsernames(java.lang.String)
     */
    public Iterator<String> getUsernames(String jid) {
        List<String> answer = new ArrayList<String>();
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_USERNAMES);
            pstmt.setString(1, jid);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                answer.add(rs.getString(1));
            }
        }
        catch (SQLException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
        return answer.iterator();
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.roster.RosterItemProvider#getItemCount(java.lang.String)
     */
    public int getItemCount(String username) {
        int count = 0;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(COUNT_ROSTER_ITEMS);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        }
        catch (SQLException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
        return count;
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.roster.RosterItemProvider#getItems(java.lang.String)
     */
    public Iterator<RosterItem> getItems(String username) {
        LinkedList<RosterItem> itemList = new LinkedList<RosterItem>();
        Connection con = null;
        Connection con2 = null;
        PreparedStatement pstmt = null;
        PreparedStatement gstmt = null;
        try {
            con2 = DbConnectionManager.getConnection();
            gstmt = con2.prepareStatement(LOAD_ROSTER_ITEM_GROUPS);
            // Load all the contacts in the roster
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_ROSTER);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Create a new RosterItem (ie. user contact) from the stored information
                RosterItem item = new RosterItem(rs.getLong(2),
                        new JID(rs.getString(1)),
                        RosterItem.SubType.getTypeFromInt(rs.getInt(3)),
                        RosterItem.AskType.getTypeFromInt(rs.getInt(4)),
                        RosterItem.RecvType.getTypeFromInt(rs.getInt(5)),
                        rs.getString(6),
                        null);
                // Load the groups for the loaded contact
                ResultSet gs = null;
                gstmt.setLong(1, item.getID());
                gs = gstmt.executeQuery();
                while (gs.next()) {
                    item.getGroups().add(gs.getString(1));
                }
                // Close the result set
                gs.close();
                // Add the loaded RosterItem (ie. user contact) to the result
                itemList.add(item);
            }
        }
        catch (SQLException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (gstmt != null) { gstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con2 != null) { con2.close(); } }
            catch (Exception e) { Log.error(e); }
        }
        return itemList.iterator();
    }

    /**
     * Insert the groups into the given roster item.
     *
     * @param rosterID The roster ID of the item the groups belong to
     * @param iter     An iterator over the group names to insert
     */
    private void insertGroups(long rosterID, Iterator<String> iter, Connection con) throws SQLException
    {
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(CREATE_ROSTER_ITEM_GROUPS);
            pstmt.setLong(1, rosterID);
            for (int i = 0; iter.hasNext(); i++) {
                pstmt.setInt(2, i);
                pstmt.setString(3, iter.next());
                try {
                    pstmt.executeUpdate();
                }
                catch (SQLException e) {
                    Log.error(e);
                }
            }
        }
        finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            }
            catch (Exception e) {
                Log.error(e);
            }
        }
    }
}
