/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.2</a>, using an XML
 * Schema.
 * $Id$
 */

package com.dumbhippo.services.smugmug.rest.bind.types;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.util.Hashtable;

/**
 * Class AccountType.
 * 
 * @version $Revision$ $Date$
 */
public class AccountType implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * The Standard type
     */
    public static final int STANDARD_TYPE = 0;

    /**
     * The instance of the Standard type
     */
    public static final AccountType STANDARD = new AccountType(STANDARD_TYPE, "Standard");

    /**
     * The Power type
     */
    public static final int POWER_TYPE = 1;

    /**
     * The instance of the Power type
     */
    public static final AccountType POWER = new AccountType(POWER_TYPE, "Power");

    /**
     * The Pro type
     */
    public static final int PRO_TYPE = 2;

    /**
     * The instance of the Pro type
     */
    public static final AccountType PRO = new AccountType(PRO_TYPE, "Pro");

    /**
     * Field _memberTable.
     */
    private static java.util.Hashtable _memberTable = init();

    /**
     * Field type.
     */
    private final int type;

    /**
     * Field stringValue.
     */
    private java.lang.String stringValue = null;


      //----------------/
     //- Constructors -/
    //----------------/

    private AccountType(final int type, final java.lang.String value) {
        super();
        this.type = type;
        this.stringValue = value;
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method enumerate.Returns an enumeration of all possible
     * instances of AccountType
     * 
     * @return an Enumeration over all possible instances of
     * AccountType
     */
    public static java.util.Enumeration enumerate(
    ) {
        return _memberTable.elements();
    }

    /**
     * Method getType.Returns the type of this AccountType
     * 
     * @return the type of this AccountType
     */
    public int getType(
    ) {
        return this.type;
    }

    /**
     * Method init.
     * 
     * @return the initialized Hashtable for the member table
     */
    private static java.util.Hashtable init(
    ) {
        Hashtable members = new Hashtable();
        members.put("Standard", STANDARD);
        members.put("Power", POWER);
        members.put("Pro", PRO);
        return members;
    }

    /**
     * Method readResolve. will be called during deserialization to
     * replace the deserialized object with the correct constant
     * instance.
     * 
     * @return this deserialized object
     */
    private java.lang.Object readResolve(
    ) {
        return valueOf(this.stringValue);
    }

    /**
     * Method toString.Returns the String representation of this
     * AccountType
     * 
     * @return the String representation of this AccountType
     */
    public java.lang.String toString(
    ) {
        return this.stringValue;
    }

    /**
     * Method valueOf.Returns a new AccountType based on the given
     * String value.
     * 
     * @param string
     * @return the AccountType value of parameter 'string'
     */
    public static com.dumbhippo.services.smugmug.rest.bind.types.AccountType valueOf(
            final java.lang.String string) {
        java.lang.Object obj = null;
        if (string != null) {
            obj = _memberTable.get(string);
        }
        if (obj == null) {
            String err = "" + string + " is not a valid AccountType";
            throw new IllegalArgumentException(err);
        }
        return (AccountType) obj;
    }

}
