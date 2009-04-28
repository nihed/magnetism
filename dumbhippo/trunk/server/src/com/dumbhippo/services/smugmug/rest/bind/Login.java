/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.2</a>, using an XML
 * Schema.
 * $Id$
 */

package com.dumbhippo.services.smugmug.rest.bind;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class Login.
 * 
 * @version $Revision$ $Date$
 */
public class Login implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _passwordHash.
     */
    private java.lang.String _passwordHash;

    /**
     * Field _accountType.
     */
    private com.dumbhippo.services.smugmug.rest.bind.types.AccountType _accountType;

    /**
     * Field _fileSizeLimit.
     */
    private java.lang.Integer _fileSizeLimit;

    /**
     * Field _smugVault.
     */
    private java.lang.Short _smugVault;

    /**
     * Field _session.
     */
    private com.dumbhippo.services.smugmug.rest.bind.Session _session;

    /**
     * Field _user.
     */
    private com.dumbhippo.services.smugmug.rest.bind.User _user;


      //----------------/
     //- Constructors -/
    //----------------/

    public Login() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'accountType'.
     * 
     * @return the value of field 'AccountType'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.types.AccountType getAccountType(
    ) {
        return this._accountType;
    }

    /**
     * Returns the value of field 'fileSizeLimit'.
     * 
     * @return the value of field 'FileSizeLimit'.
     */
    public java.lang.Integer getFileSizeLimit(
    ) {
        return this._fileSizeLimit;
    }

    /**
     * Returns the value of field 'passwordHash'.
     * 
     * @return the value of field 'PasswordHash'.
     */
    public java.lang.String getPasswordHash(
    ) {
        return this._passwordHash;
    }

    /**
     * Returns the value of field 'session'.
     * 
     * @return the value of field 'Session'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.Session getSession(
    ) {
        return this._session;
    }

    /**
     * Returns the value of field 'smugVault'.
     * 
     * @return the value of field 'SmugVault'.
     */
    public java.lang.Short getSmugVault(
    ) {
        return this._smugVault;
    }

    /**
     * Returns the value of field 'user'.
     * 
     * @return the value of field 'User'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.User getUser(
    ) {
        return this._user;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, handler);
    }

    /**
     * Sets the value of field 'accountType'.
     * 
     * @param accountType the value of field 'accountType'.
     */
    public void setAccountType(
            final com.dumbhippo.services.smugmug.rest.bind.types.AccountType accountType) {
        this._accountType = accountType;
    }

    /**
     * Sets the value of field 'fileSizeLimit'.
     * 
     * @param fileSizeLimit the value of field 'fileSizeLimit'.
     */
    public void setFileSizeLimit(
            final java.lang.Integer fileSizeLimit) {
        this._fileSizeLimit = fileSizeLimit;
    }

    /**
     * Sets the value of field 'passwordHash'.
     * 
     * @param passwordHash the value of field 'passwordHash'.
     */
    public void setPasswordHash(
            final java.lang.String passwordHash) {
        this._passwordHash = passwordHash;
    }

    /**
     * Sets the value of field 'session'.
     * 
     * @param session the value of field 'session'.
     */
    public void setSession(
            final com.dumbhippo.services.smugmug.rest.bind.Session session) {
        this._session = session;
    }

    /**
     * Sets the value of field 'smugVault'.
     * 
     * @param smugVault the value of field 'smugVault'.
     */
    public void setSmugVault(
            final java.lang.Short smugVault) {
        this._smugVault = smugVault;
    }

    /**
     * Sets the value of field 'user'.
     * 
     * @param user the value of field 'user'.
     */
    public void setUser(
            final com.dumbhippo.services.smugmug.rest.bind.User user) {
        this._user = user;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled
     * com.dumbhippo.services.smugmug.rest.bind.Login
     */
    public static com.dumbhippo.services.smugmug.rest.bind.Login unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (com.dumbhippo.services.smugmug.rest.bind.Login) Unmarshaller.unmarshal(com.dumbhippo.services.smugmug.rest.bind.Login.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}
