package com.dumbhippo.persistence;

public enum StackInclusion {
	/** Block is always in getBlocks() query results */
	IN_ALL_STACKS,
	/** Block is only in getBlocks() query result when block.data1 is the id of the Viewpoint user */
	ONLY_WHEN_VIEWING_SELF,
	/** Block is only in getBlocks() query result when block.data1 is not the id of the Viewpoint user */
	ONLY_WHEN_VIEWED_BY_OTHERS;
}
