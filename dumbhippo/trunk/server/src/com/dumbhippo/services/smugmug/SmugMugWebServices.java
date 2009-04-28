package com.dumbhippo.services.smugmug;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import com.dumbhippo.*;
import com.dumbhippo.services.smugmug.rest.bind.*;
import com.dumbhippo.services.smugmug.rest.bind.types.RspStatType;

public class SmugMugWebServices
{
	private static final Object HTTP_PROTOCOL = "http";
	private static final Object SMUGMUG_URL = "api.smugmug.com/hack";
	private static final Object REQUEST_PROTOCOL = "rest";
	private static final Object API_VERSION = "1.2.0";
	private int timeoutMilliseconds = 8000;
	private String apiKey = null;

	public SmugMugWebServices(int timeout, String apiKey) 
	{
		timeoutMilliseconds = timeout;
		this.apiKey = apiKey;
	}
	
	private boolean validateResponse(Rsp response)
	{
		boolean result = false;
		if (response != null && response.getStat() == RspStatType.OK)
		  result = true;
		return result;
	}
	
	private URL buildRequestUrl(String method, Set<Entry<String, String>> args) throws MalformedURLException
	{
		String urlString = String.format("%1$s://%2$s/%3$s/%4$s?method=%5$s", 
				new Object[]{HTTP_PROTOCOL, SMUGMUG_URL, REQUEST_PROTOCOL, API_VERSION, method});
		StringBuilder params = new StringBuilder();
		for(Entry<String, String> en:args)
		{
			params.append("&");
			params.append(en.getKey());
			params.append("=");
			params.append(StringUtils.urlEncode(en.getValue()));
		}
		urlString = urlString + params.toString();;
		URL url = new URL(urlString);
		return url; 
	}
	
	private Rsp doSmugMugCall(SmugMugMethod method) throws Exception 
	{
  		URL requestURL = buildRequestUrl(method.getName(), method.getParams());
		URLConnection connection = URLUtils.openConnection(requestURL);
		connection.setConnectTimeout(timeoutMilliseconds);
		connection.setReadTimeout(timeoutMilliseconds);			
		Rsp response = new Rsp();
		
		InputStream input = connection.getInputStream();
		response = Rsp.unmarshal(new InputStreamReader(input));
		
		return response;
	}
	
	private Exception doException(SmugMugMethod method, Err error, Hashtable<String, String> params)
	{
		Set<Entry<String, String>> paramSet = params.entrySet();
		StringBuffer paramList = new StringBuffer("Param list:");
		for(Entry<String, String> en:paramSet)
		{
			paramList.append(" name=");
			paramList.append(en.getKey());
			paramList.append(" value=");
			paramList.append(en.getValue());
			paramList.append(";");
		}
		
		String message = String.format("Method %1$s has returned error (code = %2$s, msg=%3$s), %4$s", 
				new Object[]{method.getName().toString(), error.getCode(), error.getMsg(), paramList.toString()});
		return new Exception(message);
	}

	public Login loginAnonymously() throws Exception
	{
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("APIKey", apiKey);
		
		SmugMugMethod method = new SmugMugMethod(SmugMugMethodsEnum.smugmug_login_anonymously);
		method.addAll(params);
		Rsp response = doSmugMugCall(method);
		
		if (validateResponse(response))
		  return response.getRspChoice().getLogin();
		else
		  throw doException(method, response.getRspChoice().getErr(), params);
	}
	
	public Login loginWithPassword(String emailAddress, String password) throws Exception
	{
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("APIKey", apiKey);
		params.put("EmailAddress", emailAddress);
		params.put("Password", password);
		
		SmugMugMethod method = new SmugMugMethod(SmugMugMethodsEnum.smugmug_login_withPassword);
		method.addAll(params);
		Rsp response = doSmugMugCall(method);
		
		if (validateResponse(response))
		  return response.getRspChoice().getLogin();
		else
		  throw doException(method, response.getRspChoice().getErr(), params);
	}
	
	public boolean ping(String sessionId, String nickName) throws Exception
	{
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("SessionID", sessionId);
		params.put("NickName", nickName);
		
		SmugMugMethod method = new SmugMugMethod(SmugMugMethodsEnum.smugmug_albums_get);
		method.addAll(params);
		Rsp response = doSmugMugCall(method);
		
		return validateResponse(response);
	}

	public Album[] getAlbums(String sessionId, String nickName) throws Exception
	{
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("SessionID", sessionId);
		params.put("NickName", nickName);

		SmugMugMethod method = new SmugMugMethod(SmugMugMethodsEnum.smugmug_albums_get);
		method.addAll(params);
		Rsp response = doSmugMugCall(method);
		
		if (validateResponse(response))
		  return response.getRspChoice().getAlbums().getAlbum();
		else
 	      throw doException(method, response.getRspChoice().getErr(), params);
	}

	public Image[] getImages(String sessionId, String nickName, boolean heavy, 
			String albumId, String albumKey) throws Exception
	{
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("SessionID", sessionId);
		params.put("NickName", nickName);
		params.put("Heavy", (heavy)?"1":"0");
		params.put("AlbumID", albumId);
		params.put("AlbumKey", albumKey);

		SmugMugMethod method = new SmugMugMethod(SmugMugMethodsEnum.smugmug_images_get);
		method.addAll(params);
		Rsp response = doSmugMugCall(method);
		
		if (validateResponse(response))
		  return response.getRspChoice().getImages().getImage();
		else
		  throw doException(method, response.getRspChoice().getErr(), params);
	}

	public Image[] getLastImages(String nickName, int imgCount) throws Exception
	{
		Login login = loginAnonymously();
		Image[] images = getAllImages(login.getSession().getId(), nickName);
		Arrays.sort(images, new java.util.Comparator<Image>()
				{
			       public int compare(Image i1, Image i2)
			       {
			    	   return i1.getId().compareTo(i2.getId());
			       }
			       
			       public boolean equals(Object img)
			       {
			    	   return this == img;
			       }
				});
		List<Image> imgList = Arrays.asList(images);
		java.util.Collections.reverse(imgList);
		images = imgList.toArray(images);
		if (imgCount < images.length)
		  images = Arrays.copyOfRange(images, 0, imgCount);
		return images;
	}
	
	public Image[] getAllImages(String sessionId, String nickName) throws Exception
	{
		Album[] albums = getAlbums(sessionId, nickName);
		ArrayList<Image> imageList = new ArrayList<Image>(); 
		for(Album a:albums)
		{
			Image[] images = getImages(sessionId, nickName, true, a.getId(), a.getKey());
			List<Image> list = Arrays.asList(images);
			imageList.addAll(list);
		}
		Image[] res = new Image[0];
		return imageList.toArray(res);
	}
}
