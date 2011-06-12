package utils.webclient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.DateUtils;

public class WebClientCookieExtractor
{
	public List<Cookie> GetCookies(Header[] allHeaders)
	{
		List<Cookie> cookieList = new ArrayList<Cookie>();
		
		for (Header h : allHeaders)
		{
			try
			{
				BasicClientCookie cookie = ExtractCookie(h);
				if (cookie != null)
					cookieList.add(cookie);
			}
			catch (Exception e)
			{
			}
		}
		
		return cookieList;
	}
	
	private BasicClientCookie ExtractCookie(Header cookieHeader) throws Exception
	{
		if (cookieHeader.getName().toLowerCase().compareTo("set-cookie") == 0)
		{
			String[] cookieKeyPairs = cookieHeader.getValue().split(";");
			BasicClientCookie cookie = null;
			
			for (String cookieKeyPair : cookieKeyPairs)
			{
				int equalToIndex = cookieKeyPair.indexOf("=");
				String[] keyValue = null;
				
				if (equalToIndex == -1)
				{
					keyValue = new String[1];
					keyValue[0] = cookieKeyPair.trim();
				}
				else
				{
					keyValue = new String[2];
					keyValue[0] = cookieKeyPair.substring(0, equalToIndex).trim();
					keyValue[1] = cookieKeyPair.substring(equalToIndex + 1);
				}
				
				if (keyValue.length == 2)
				{
					if (keyValue[0].toLowerCase().compareTo(BasicClientCookie.DOMAIN_ATTR) == 0)
					{
						cookie.setDomain(keyValue[1]);
					}
					else if (keyValue[0].toLowerCase().compareTo(BasicClientCookie.EXPIRES_ATTR) == 0)
					{
						cookie.setExpiryDate(DateUtils.parseDate(keyValue[1]));
					}
					else if (keyValue[0].toLowerCase().compareTo(BasicClientCookie.MAX_AGE_ATTR) == 0)
					{
						Calendar curr = Calendar.getInstance();
						curr.add(Calendar.SECOND, Integer.parseInt(keyValue[1]));
						cookie.setExpiryDate(curr.getTime());
					}
					else if (keyValue[0].toLowerCase().compareTo(BasicClientCookie.PATH_ATTR) == 0)
					{
						cookie.setPath(keyValue[1]);
					}
					else if (keyValue[0].toLowerCase().compareTo(BasicClientCookie.VERSION_ATTR) == 0)
					{
						cookie.setVersion(Integer.parseInt(keyValue[1]));
					}
					else
					{
						cookie = new BasicClientCookie(keyValue[0], keyValue[1]);
					}
				}
				else
				{
					if (keyValue[0].toLowerCase().compareTo(BasicClientCookie.SECURE_ATTR) == 0)
					{
						cookie.setSecure(true);
					}
					else if (keyValue[0].toLowerCase().compareTo("httponly") == 0)
					{
						cookie.setAttribute("httponly", "true");
					}
				}
			}
			
			return cookie;
		}
		else
		{
			return null;
		}
	}
}
