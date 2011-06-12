package utils.webclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;

public class WebClientResponse
{
	public enum WebClientCallStatus
	{
		Complete,
		InProgress,
		Failed
	}
	
	private ByteArrayOutputStream copyOfStream = null;
	private boolean isReponseAvailable = false;
	
	public String response;
	public WebClientCallStatus callStatus;
	public int status;
	public HttpUriRequest requestUri;
	public HttpHost requestHost;
	public Header[] headers;
	public List<Cookie> responseCookies;
	public Exception occuredException = null;
	
	public void SetResponseStream(InputStream input)
	{
		try
		{
			if (this.copyOfStream == null)
			{
				this.copyOfStream = new ByteArrayOutputStream();
				
				int read = 0;
				int chunk = 0;
				byte[] data = new byte[256];
				
				while(-1 != (chunk = input.read(data)))
				{
					read += data.length;
					this.copyOfStream.write(data, 0, chunk);
				}
				
				this.copyOfStream.flush();
				this.copyOfStream.close();
				
				this.isReponseAvailable = true;
			}
		}
		catch (Exception e)
		{
			this.occuredException = e;
		}
	}
	
	public String ReadResposeAsString()
	{
		if (this.isReponseAvailable)
		{
			return this.copyOfStream.toString();
		}
		else
		{
			return null;
		}
	}
	
	public InputStream ReadResponseStream()
	{
		if (this.isReponseAvailable)
		{
			try
			{
				return (InputStream)new ByteArrayInputStream(this.copyOfStream.toByteArray());
			}
			catch (Exception e)
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}
}
