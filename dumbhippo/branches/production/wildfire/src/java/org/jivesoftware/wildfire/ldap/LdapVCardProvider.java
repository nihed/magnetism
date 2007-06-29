/**
 * $Revision: 1217 $
 * $Date: 2005-04-11 14:11:06 -0700 (Mon, 11 Apr 2005) $
 *
 * Copyright (C) 2005 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.ldap;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jivesoftware.wildfire.vcard.VCardProvider;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.JID;

/**
 * Read-only LDAP provider for vCards.Configuration consists of adding a provider:<p/>
 *
 * <pre>
 * &lt;provider&gt;
 *   &lt;vcard&gt;
 *  	&lt;className&gt;org.jivesoftware.wildfire.ldap.LdapVCardProvider&lt;/className&gt;
 *    &lt;/vcard&gt;
 * &lt;/provider&gt;
 * </pre><p/>
 *
 * and an xml vcard-mapping to wildfire.xml.<p/>
 *
 * The vcard attributes can be configured by adding an <code>attrs="attr1,attr2"</code>
 * attribute to the vcard elements.<p/>
 *
 * Arbitrary text can be used for the element values as well as <code>MessageFormat</code>
 * style placeholders for the ldap attributes. For example, if you wanted to map the LDAP
 * attribute <code>displayName</code> to the vcard element <code>FN</code>, the xml
 * nippet would be:<br><pre>&lt;FN attrs=&quot;displayName&quot;&gt;{0}&lt;/FN&gt;</pre><p/>
 *
 * The vCard XML must be escaped in CDATA and must also be well formed. It is the exact
 * XML this provider will send to a client after after stripping <code>attr</code> attributes
 * and populating the placeholders with the data retrieved from LDAP. This system should
 * be flexible enough to handle any client's vCard format. An example mapping follows.<br>
 * <pre>
 *    &lt;ldap&gt;
 *      &lt;vcard-mapping&gt;
 *        &lt;![CDATA[
 *    		&lt;vCard xmlns='vcard-temp'&gt;
 *    			&lt;FN attrs=&quot;displayName&quot;&gt;{0}&lt;/FN&gt;
 *    			&lt;NICKNAME attrs=&quot;uid&quot;&gt;{0}&lt;/NICKNAME&gt;
 *    			&lt;BDAY attrs=&quot;dob&quot;&gt;{0}&lt;/BDAY&gt;
 *    			&lt;ADR&gt;
 *    				&lt;HOME/&gt;
 *    				&lt;EXTADR&gt;Ste 500&lt;/EXTADR&gt;
 *    				&lt;STREET&gt;317 SW Alder St&lt;/STREET&gt;
 *    				&lt;LOCALITY&gt;Portland&lt;/LOCALITY&gt;
 *    				&lt;REGION&gt;Oregon&lt;/REGION&gt;
 *    				&lt;PCODE&gt;97204&lt;/PCODE&gt;
 *    				&lt;CTRY&gt;USA&lt;/CTRY&gt;
 *    			&lt;/ADR&gt;
 *    			&lt;TEL&gt;
 *    				&lt;HOME/&gt;
 *    				&lt;VOICE/&gt;
 *    				&lt;NUMBER attrs=&quot;telephoneNumber&quot;&gt;{0}&lt;/NUMBER&gt;
 *    			&lt;/TEL&gt;
 *    			&lt;EMAIL&gt;
 *    				&lt;INTERNET/&gt;
 *    				&lt;USERID attrs=&quot;mail&quot;&gt;{0}&lt;/USERID&gt;
 *    			&lt;/EMAIL&gt;
 *    			&lt;TITLE attrs=&quot;title&quot;&gt;{0}&lt;/TITLE&gt;
 *    			&lt;ROLE attrs=&quot;&quot;&gt;{0}&lt;/ROLE&gt;
 *    			&lt;ORG&gt;
 *    				&lt;ORGNAME attrs=&quot;o&quot;&gt;{0}&lt;/ORGNAME&gt;
 *    				&lt;ORGUNIT attrs=&quot;&quot;&gt;{0}&lt;/ORGUNIT&gt;
 *    			&lt;/ORG&gt;
 *    			&lt;URL attrs=&quot;labeledURI&quot;&gt;{0}&lt;/URL&gt;
 *    			&lt;DESC attrs=&quot;uidNumber,homeDirectory,loginShell&quot;&gt;
 *    				uid: {0} home: {1} shell: {2}
 *    			&lt;/DESC&gt;
 *    		&lt;/vCard&gt;
 *        ]]&gt;
 *      &lt;/vcard-mapping&gt;
 *    &lt;/ldap&gt;
 * </pre><p>
 * <p/>
 * An easy way to get the vcard format your client needs, assuming you've been
 * using the database store, is to do a <code>SELECT value FROM jivevcard WHERE
 * username='some_user'</code> in your favorite sql querier and paste the result
 * into the <code>vcard-mapping</code> (don't forget the CDATA).
 *
 * @author rkelly
 */
public class LdapVCardProvider implements VCardProvider {

    private LdapManager manager;
    private VCardTemplate template;

    public LdapVCardProvider() {
        manager = LdapManager.getInstance();
        String property = JiveGlobals.getXMLProperty("ldap.vcard-mapping");
        Log.debug("Found vcard mapping: '" + property);
        try {
            Document document = DocumentHelper.parseText(property);
            template = new VCardTemplate(document);
        }
        catch (Exception e) {
            Log.error("Error loading vcard mapping: " + e.getMessage());
        }

        Log.debug("attributes size==" + template.getAttributes().length);
    }

    private Map<String, String> getLdapAttributes(String username) {
        HashMap<String, String> map = new HashMap<String, String>();

        DirContext ctx = null;
        try {
            String userDN = manager.findUserDN(username);

            ctx = manager.getContext();
            Attributes attrs = ctx.getAttributes(userDN, template.getAttributes());

            for (String attribute : template.getAttributes()) {
                javax.naming.directory.Attribute attr = attrs.get(attribute);
                String value;
                if (attr == null) {
                    Log.debug("No ldap value found for attribute '" + attribute
                            + "'");
                    value = "";
                }
                else {
                    value = (String) attrs.get(attribute).get();
                }
                Log.debug("Ldap attribute '" + attribute + "'=>'" + value + "'");
                map.put(attribute, value);
            }
            return map;
        }
        catch (Exception e) {
            Log.error(e);
            return null;
        }
        finally {
            try {
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception e) {
                // Ignore.
            }
        }
    }

    public Element loadVCard(String username) {
        // Un-escape username.
        username = JID.unescapeNode(username);
        Map<String, String> map = getLdapAttributes(username);
        Log.debug("Getting mapped vcard for " + username);
        Element vcard = new VCard(template).getVCard(map);
        Log.debug("Returning vcard");
        return vcard;
    }

    public void createVCard(String username, Element vCardElement) throws AlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    public void updateVCard(String username, Element vCardElement) throws NotFoundException {
        throw new UnsupportedOperationException();
    }

    public void deleteVCard(String username) {
        throw new UnsupportedOperationException();
    }

    public boolean isReadOnly() {
        return true;
    }

    /**
     * Class to hold a <code>Document</code> representation of a vcard mapping
     * and unique attribute placeholders. Used by <code>VCard</code> to apply
     * a <code>Map</code> of ldap attributes to ldap values via
     * <code>MessageFormat</code>
     *
     * @author rkelly
     */
    private static class VCardTemplate {

        private Document document;

        private String[] attributes;

        public VCardTemplate(Document document) {
            Set<String> set = new HashSet<String>();
            this.document = document;
            treeWalk(this.document.getRootElement(), set);
            attributes = set.toArray(new String[set.size()]);
        }

        public String[] getAttributes() {
            return attributes;
        }

        public Document getDocument() {
            return document;
        }

        private void treeWalk(Element element, Set<String> set) {
            for (int i = 0, size = element.nodeCount(); i < size; i++) {
                Node node = element.node(i);
                if (node instanceof Element) {
                    Element emement = (Element) node;
                    Attribute attr = emement.attribute("attrs");
                    if (attr != null) {
                        String[] attrs = attr.getStringValue().split(",");
                        for (String string : attrs) {
                            Log.debug("VCardTemplate: found attribute "
                                    + string);
                            set.add(string);
                        }
                    }
                    treeWalk(emement, set);
                }
            }
        }
    }

    /**
     * vCard class that converts vcard data using a template.
     */
    private static class VCard {

        private VCardTemplate template;

        public VCard(VCardTemplate template) {
            this.template = template;
        }

        public Element getVCard(Map<String, String> map) {
            Document document = (Document) template.getDocument().clone();
            Element element = document.getRootElement();
            return treeWalk(element, map);
        }

        private Element treeWalk(Element element, Map<String, String> map) {
            for (int i = 0, size = element.nodeCount(); i < size; i++) {
                Node node = element.node(i);
                if (node instanceof Element) {
                    Element emement = (Element) node;
                    Attribute attr = emement.attribute("attrs");
                    if (attr != null) {
                        String[] attrs = attr.getStringValue().split(",");
                        Object[] values = new String[attrs.length];
                        for (int j = 0; j < attrs.length; j++) {
                            values[j] = map.get(attrs[j]);
                        }
                        emement.remove(attr);
                        emement.setText(MessageFormat.format(emement
                                .getStringValue(), values));
                    }
                    treeWalk(emement, map);
                }
            }
            return element;
        }
    }
}