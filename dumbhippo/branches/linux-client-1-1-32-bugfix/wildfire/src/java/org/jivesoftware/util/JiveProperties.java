/**
 * $RCSfile$
 * $Revision: 1217 $
 * $Date: 2005-04-11 17:11:06 -0400 (Mon, 11 Apr 2005) $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.*;

import org.jivesoftware.database.DbConnectionManager;

/**
 * Retrieves and stores Jive properties. Properties are stored in the database.
 *
 * @author Matt Tucker
 */
public class JiveProperties implements Map {

    private static final String LOAD_PROPERTIES = "SELECT name, propValue FROM jiveProperty";
    private static final String INSERT_PROPERTY = "INSERT INTO jiveProperty(name, propValue) VALUES(?,?)";
    private static final String UPDATE_PROPERTY = "UPDATE jiveProperty SET propValue=? WHERE name=?";
    private static final String DELETE_PROPERTY = "DELETE FROM jiveProperty WHERE name LIKE ?";

    private static JiveProperties instance;

    private Map<String, String> properties;

    /**
     * Returns a singleton instance of JiveProperties.
     *
     * @return an instance of JiveProperties.
     */
    public static synchronized JiveProperties getInstance() {
        if (instance == null) {
            instance = new JiveProperties();
        }
        return instance;
    }

    private JiveProperties() {
        init();
    }

    /**
     * For internal use only. This method allows for the reloading of all properties from the
     * values in the datatabase. This is required since it's quite possible during the setup
     * process that a database connection will not be available till after this class is
     * initialized. Thus, if there are existing properties in the database we will want to reload
     * this class after the setup process has been completed.
     */
    public void init() {
        if (properties == null) {
            properties = new ConcurrentHashMap<String, String>();
        }
        else {
            properties.clear();
        }

        loadProperties();
    }

    public int size() {
        return properties.size();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    public Collection values() {
        return Collections.unmodifiableCollection(properties.values());
    }

    public void putAll(Map t) {
        for (Iterator i=t.entrySet().iterator(); i.hasNext();  ) {
            Map.Entry entry = (Map.Entry)i.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set entrySet() {
        return Collections.unmodifiableSet(properties.entrySet());
    }

    public Set keySet() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public Object get(Object key) {
        return properties.get(key);
    }

    /**
     * Return all children property names of a parent property as a Collection
     * of String objects. For example, given the properties <tt>X.Y.A</tt>,
     * <tt>X.Y.B</tt>, and <tt>X.Y.C</tt>, then the child properties of
     * <tt>X.Y</tt> are <tt>X.Y.A</tt>, <tt>X.Y.B</tt>, and <tt>X.Y.C</tt>. The method
     * is not recursive; ie, it does not return children of children.
     *
     * @param parentKey the name of the parent property.
     * @return all child property names for the given parent.
     */
    public Collection<String> getChildrenNames(String parentKey) {
        Collection<String> results = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.startsWith(parentKey + ".")) {
                if (key.equals(parentKey)) {
                    continue;
                }
                int dotIndex = key.indexOf(".", parentKey.length()+1);
                if (dotIndex < 1) {
                    if (!results.contains(key)) {
                        results.add(key);
                    }
                }
                else {
                    String name = parentKey + key.substring(parentKey.length(), dotIndex);
                    results.add(name);
                }
            }
        }
        return results;
    }

    /**
     * Returns all property names as a Collection of String values.
     *
     * @return all property names.
     */
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    public synchronized Object remove(Object key) {
        Object value = properties.remove(key);
        // Also remove any children.
        Collection propNames = getPropertyNames();
        for (Iterator i=propNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (name.startsWith((String)key)) {
                properties.remove(name);
            }
        }
        deleteProperty((String)key);

        // Generate event.
        PropertyEventDispatcher.dispatchEvent((String)key,
                PropertyEventDispatcher.EventType.property_deleted, Collections.emptyMap());

        return value;
    }

    public synchronized Object put(Object key, Object value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key or value cannot be null. Key=" +
                    key + ", value=" + value);
        }
        if (!(key instanceof String) || !(value instanceof String)) {
            throw new IllegalArgumentException("Key and value must be of type String.");
        }
        if (((String)key).endsWith(".")) {
            key = ((String)key).substring(0, ((String)key).length()-1);
        }
        key =((String)key).trim();
        if (properties.containsKey(key)) {
            if (!properties.get(key).equals(value)) {
                updateProperty((String)key, (String)value);
            }
        }
        else {
            insertProperty((String)key, (String)value);
        }

        // Generate event.
        Map params = new HashMap();
        params.put("value", value);
        PropertyEventDispatcher.dispatchEvent((String)key,
                PropertyEventDispatcher.EventType.property_set, params);

        return properties.put((String)key, (String)value);
    }

    private void insertProperty(String name, String value) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(INSERT_PROPERTY);
            pstmt.setString(1, name);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    private void updateProperty(String name, String value) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(UPDATE_PROPERTY);
            pstmt.setString(1, value);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    private void deleteProperty(String name) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_PROPERTY);
            pstmt.setString(1, name + "%");
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    private void loadProperties() {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_PROPERTIES);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                properties.put(name, value);
            }
            rs.close();
        }
        catch (Exception e) {
            Log.error(e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }
}