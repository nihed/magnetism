package com.dumbhippo.server.blocks;


/** usually for access controls we just use NotFoundException, but in this case 
 * populateBlockContents isn't just a "lookup function" and doesn't naturally 
 * have anything to "not find" - also it calls lots of 
 * other things that throw NotFoundException, some of which should not be 
 * passed out as they don't indicate that we can't see the block
 * @author Havoc Pennington
 */
public class BlockNotVisibleException extends Exception {
	private static final long serialVersionUID = 1L;

	public BlockNotVisibleException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public BlockNotVisibleException(String message) {
		super(message);
	}
}
	