/**
 * $RCSfile$
 * $Revision: 1759 $
 * $Date: 2005-08-09 19:32:51 -0300 (Tue, 09 Aug 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.wildfire.container.BasicModule;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Private storage for user accounts (JEP-0049). It is used by some XMPP systems
 * for saving client settings on the server.
 *
 * @author Iain Shigeoka
 */
public class PrivateStorage extends BasicModule {

    private static final String LOAD_PRIVATE =
        "SELECT value FROM jivePrivate WHERE username=? AND namespace=?";
    private static final String INSERT_PRIVATE =
        "INSERT INTO jivePrivate (value,name,username,namespace) VALUES (?,?,?,?)";
    private static final String UPDATE_PRIVATE =
        "UPDATE jivePrivate SET value=?, name=? WHERE username=? AND namespace=?";

    // Currently no delete supported, we can detect an add of an empty element and
    // use that to signal a delete but that optimization doesn't seem necessary.
    // private static final String DELETE_PRIVATE =
    //     "DELETE FROM jivePrivate WHERE userID=? AND name=? AND namespace=?";

    private boolean enabled = JiveGlobals.getBooleanProperty("xmpp.privateStorageEnabled", true);

    /**
     * Pool of SAX Readers. SAXReader is not thread safe so we need to have a pool of readers.
     */
    private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<SAXReader>();

    /**
     * Constructs a new PrivateStore instance.
     */
    public PrivateStorage() {
        super("Private user data storage");
    }

    /**
     * Returns true if private storage is enabled.
     *
     * @return true if private storage is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether private storage is enabled.
     *
     * @param enabled true if this private store is enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        JiveGlobals.setProperty("xmpp.privateStorageEnabled", Boolean.toString(enabled));
    }

    /**
     * Stores private data. If the name and namespace of the element matches another
     * stored private data XML document, then replace it with the new one.
     *
     * @param data the data to store (XML element)
     * @param username the username of the account where private data is being stored
     */
    public void add(String username, Element data) {
        if (enabled) {
            java.sql.Connection con = null;
            PreparedStatement pstmt = null;
            try {
                StringWriter writer = new StringWriter();
                data.write(writer);
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(LOAD_PRIVATE);
                pstmt.setString(1, username);
                pstmt.setString(2, data.getNamespaceURI());
                ResultSet rs = pstmt.executeQuery();
                boolean update = false;
                if (rs.next()) {
                    update = true;
                }
                rs.close();
                pstmt.close();

                if (update) {
                    pstmt = con.prepareStatement(UPDATE_PRIVATE);
                }
                else {
                    pstmt = con.prepareStatement(INSERT_PRIVATE);
                }
                pstmt.setString(1, writer.toString());
                pstmt.setString(2, data.getName());
                pstmt.setString(3, username);
                pstmt.setString(4, data.getNamespaceURI());
                pstmt.executeUpdate();
            }
            catch (Exception e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
            finally {
                try { if (pstmt != null) { pstmt.close(); } }
                catch (Exception e) { Log.error(e); }
                try { if (con != null) { con.close(); } }
                catch (Exception e) { Log.error(e); }
            }
        }
    }

    /**
     * Returns the data stored under a key corresponding to the name and namespace
     * of the given element. The Element must be in the form:<p>
     *
     * <code>&lt;name xmlns='namespace'/&gt;</code><p>
     *
     * If no data is currently stored under the given key, an empty element will be
     * returned.
     *
     * @param data an XML document who's element name and namespace is used to
     *      match previously stored private data.
     * @param username the username of the account where private data is being stored.
     * @return the data stored under the given key or the data element.
     */
    public Element get(String username, Element data) {
        if (enabled) {
            Connection con = null;
            PreparedStatement pstmt = null;
            SAXReader xmlReader = null;
            try {
                // Get a sax reader from the pool
                xmlReader = xmlReaders.take();
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(LOAD_PRIVATE);
                pstmt.setString(1, username);
                pstmt.setString(2, data.getNamespaceURI());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    data.clearContent();
                    String result = rs.getString(1).trim();
                    Document doc = xmlReader.read(new StringReader(result));
                    data = doc.getRootElement();
                }
                rs.close();
            }
            catch (Exception e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
            finally {
                // Return the sax reader to the pool
                if (xmlReader != null) xmlReaders.add(xmlReader);
                try { if (pstmt != null) { pstmt.close(); } }
                catch (Exception e) { Log.error(e); }
                try { if (con != null) { con.close(); } }
                catch (Exception e) { Log.error(e); }
            }
        }
        return data;
    }

    public void start() throws IllegalStateException {
        super.start();
        // Initialize the pool of sax readers
        for (int i=0; i<10; i++) {
            xmlReaders.add(new SAXReader());
        }
    }

    public void stop() {
        super.stop();
        // Clean up the pool of sax readers
        xmlReaders.clear();
    }
}