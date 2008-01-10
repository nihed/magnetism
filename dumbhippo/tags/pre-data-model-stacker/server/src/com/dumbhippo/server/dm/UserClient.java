package com.dumbhippo.server.dm;

import com.dumbhippo.dm.DMClient;
import com.dumbhippo.identity20.Guid;

public interface UserClient extends DMClient {
	public Guid getUserId();
}
