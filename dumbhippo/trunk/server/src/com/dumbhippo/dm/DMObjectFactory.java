package com.dumbhippo.dm;

public interface DMObjectFactory<ResourceType extends DMKey> {
	DMObject<ResourceType> create(ResourceType resource);
}
