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
 * Class RspStatType.
 * 
 * @version $Revision$ $Date$
 */
public class RspStatType implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * The ok type
     */
    public static final int OK_TYPE = 0;

    /**
     * The instance of the ok type
     */
    public static final RspStatType OK = new RspStatType(OK_TYPE, "ok");

    /**
     * The fail type
     */
    public static final int FAIL_TYPE = 1;

    /**
     * The instance of the fail type
     */
    public static final RspStatType FAIL = new RspStatType(FAIL_TYPE, "fail");

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

    private RspStatType(final int type, final java.lang.String value) {
        super();
        this.type = type;
        this.stringValue = value;
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method enumerate.Returns an enumeration of all possible
     * instances of RspStatType
     * 
     * @return an Enumeration over all possible instances of
     * RspStatType
     */
    public static java.util.Enumeration enumerate(
    ) {
        return _memberTable.elements();
    }

    /**
     * Method getType.Returns the type of this RspStatType
     * 
     * @return the type of this RspStatType
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
        members.put("ok", OK);
        members.put("fail", FAIL);
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
     * RspStatType
     * 
     * @return the String representation of this RspStatType
     */
    public java.lang.String toString(
    ) {
        return this.stringValue;
    }

    /**
     * Method valueOf.Returns a new RspStatType based on the given
     * String value.
     * 
     * @param string
     * @return the RspStatType value of parameter 'string'
     */
    public static com.dumbhippo.services.smugmug.rest.bind.types.RspStatType valueOf(
            final java.lang.String string) {
        java.lang.Object obj = null;
        if (string != null) {
            obj = _memberTable.get(string);
        }
        if (obj == null) {
            String err = "" + string + " is not a valid RspStatType";
            throw new IllegalArgumentException(err);
        }
        return (RspStatType) obj;
    }

}
