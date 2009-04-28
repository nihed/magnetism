package com.dumbhippo.services.smugmug;

import java.util.Hashtable;
import java.util.Set;
import java.util.Map.Entry;

public class SmugMugMethod 
{
	private Hashtable<String, String> params = new Hashtable<String, String>();
	private SmugMugMethodsEnum name = null;
	
	public SmugMugMethod(SmugMugMethodsEnum en) throws IllegalArgumentException
	{
		if (en == null)
			throw new IllegalArgumentException("SmugMug method name cannot be null");
		name = en;
	}
	
	public void addParam(String name, String value)
	{
		params.put(name, value);
	}

	public void addAll(Hashtable<String, String> tab)
	{
		params.putAll(tab);
	}
	
	public Set<Entry<String, String>> getParams()
	{
		return params.entrySet();
	}
	
	public String getName()
	{
		return name.toString();
	}

}
