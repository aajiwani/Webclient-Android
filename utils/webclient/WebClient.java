package utils.webclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import utils.webclient.WebClientResponse.WebClientCallStatus;
import android.content.Context;
import android.os.AsyncTask;

public final class WebClient extends AsyncTask<Void, Void, Void>
{
	public enum RequestType
	{
		GET,
		POST
	}
	
	private class CallbackReciever
	{
		@SuppressWarnings("unused")
		public Class<?> Reciever;
		public boolean IsStatic;
		public Method CallbackMethod;
		public Object RecieverInstance;
	}
	
	private class FileParameterInfo
	{
		public String ParamName;
		public String FilePath;
		public boolean ShouldDelete;
		public Context contextOfFile;
		public String PostFileName;
		
		public FileParameterInfo(String paramName, String filePath, boolean toBeDeleted, Context context, String postFileName)
		{
			this.ParamName = paramName;
			this.FilePath = filePath;
			this.ShouldDelete = toBeDeleted;
			this.contextOfFile = context;
			this.PostFileName = postFileName;
		}
	}
	
	private String url;
	private List<NameValuePair> postDataParams;
	private List<FileParameterInfo> postFileParams;
	private RequestType reqType;
	private boolean isValidRequest;
	private ArrayList<CallbackReciever> reqCompleteCallbacks = new ArrayList<CallbackReciever>();
	private Object userState = null;			// Object to be returned on call back
	private WebClientResponse webClientReponse = null;
	private BasicCookieStore cookies = null;
	private ArrayList<NameValuePair> headers = null;
	private HttpClient httpClient = null;
	private boolean isHttpClientInitialized = false;
	private boolean isCookiesAttached = false;
	private boolean isHeadersAttached = false;
	private HttpContext httpCallContext = null;
	
	private HttpEntity GetRequestEntity()
	{
		HttpEntity entity = null;
		
		try
		{
			if (this.postFileParams.size() > 0)
			{
				entity = new MultipartEntity();
				MultipartEntity fileEntity = (MultipartEntity)entity;
				
				for (int i = 0; i < this.postFileParams.size(); i++)
				{
					if (this.postFileParams.get(i).PostFileName != null)
					{
						FileBody fb = new FileBody(new File(this.postFileParams.get(i).FilePath), this.postFileParams.get(i).PostFileName, "multipart/mixed", "ISO-8859-1");
						fileEntity.addPart(this.postFileParams.get(i).ParamName, fb);
					}
					else
					{
						fileEntity.addPart(this.postFileParams.get(i).ParamName, new FileBody(new File(this.postFileParams.get(i).FilePath)));
					}
				}
			}
			
			if (entity != null)
			{
				MultipartEntity fileEntity = (MultipartEntity)entity;
				
				if (this.postDataParams.size() > 0)
				{
					for (int i = 0; i < this.postDataParams.size(); i++)
					{
						fileEntity.addPart(this.postDataParams.get(i).getName(), new StringBody(this.postDataParams.get(i).getValue()));
					}
				}
			}
			else
			{
				entity = new UrlEncodedFormEntity(this.postDataParams);
			}
		}
		catch (Exception e)
		{
			this.webClientReponse.occuredException = e;
			this.isValidRequest = false;
		}
		
		return entity;
	}
	
	private void RemoveTemperaryFiles()
	{
		try
		{
			for (int i = 0; i < this.postFileParams.size(); i++)
			{
				if (this.postFileParams.get(i).ShouldDelete && this.postFileParams.get(i).contextOfFile != null)
				{
					File file = new File(this.postFileParams.get(i).FilePath);
					file.delete();
				}
			}
		}
		catch (Exception e)
		{
			
		}
	}	

	private String GeneratePath(Context context, String fileName)
	{
		return context.getFilesDir().getAbsolutePath() + "/" + fileName;
	}
	
	private void AddFileParameter(String name, String filePath, boolean shouldDelete, Context context, String postFileName)
	{
		this.postFileParams.add(new FileParameterInfo(name, filePath, shouldDelete, context, postFileName));
	}
	
	private void InitializeHttpClient()
	{
		try
		{
			//SETS UP PARAMETERS
			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, "utf-8");
			
			params.setBooleanParameter("http.protocol.expect-continue", false);
		
			//REGISTERS SCHEMES FOR BOTH HTTP AND HTTPS
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", new WebClientSSLSocketFactory(), 443));
	           
	        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);
			 
	        this.httpClient = new DefaultHttpClient(manager, params);
	        this.httpCallContext = new BasicHttpContext();
	        this.isHttpClientInitialized = true;
		}
		catch (Exception e)
		{
			this.isValidRequest = false;
			this.webClientReponse.occuredException = e;
		}
	}
	
	private void AttachCookies()
	{
		try
		{
			this.httpCallContext.setAttribute(ClientContext.COOKIE_STORE, this.cookies);
			
			this.isCookiesAttached = true;
		}
		catch (Exception e)
		{
			this.isValidRequest = false;
			this.webClientReponse.occuredException = e;
		}
	}
	
	private void AttachHeaders()
	{
		try
		{
			final ArrayList<NameValuePair> localHeaders = this.headers;
			
			((AbstractHttpClient) this.httpClient).addRequestInterceptor(new HttpRequestInterceptor()
			{   
				public void process(HttpRequest request, HttpContext context) throws HttpException, IOException
				{
					for (int i = 0; i < localHeaders.size(); i++)
					{
						request.addHeader(localHeaders.get(i).getName(), localHeaders.get(i).getValue());
						
					}
				}
			});
			
			this.isHeadersAttached = true;
		}
		catch (Exception e)
		{
			this.webClientReponse.occuredException = e;
			this.isValidRequest = false;
		}
	}
	
	public WebClient(String url, RequestType type)
	{
		this.url = url;
		this.reqType = type;
		this.postDataParams = new ArrayList<NameValuePair>();
		this.postFileParams = new ArrayList<WebClient.FileParameterInfo>();
		this.isValidRequest = true;
		this.webClientReponse = new WebClientResponse();
	}

	public void AddPostParameter(String name, String value)
	{
		this.postDataParams.add(new BasicNameValuePair(name, value));
	}
	
	public void AddFileParameter(String paramName, String filePath)
	{
		this.AddFileParameter(paramName, filePath, false, null, null);
	}
	
	public void AddFileParameter(String paramName, String filePath, String postFileName)
	{
		this.AddFileParameter(paramName, filePath, false, null, postFileName);
	}
	
	public void AddFileParameter(String paramName, InputStream fileStream, Context context)
	{
		this.AddFileParameter(paramName, fileStream, context, null);
	}
	
	public void AddFileParameter(String paramName, InputStream fileStream, Context context, String postFileName)
	{
		String fileName = UUID.randomUUID().toString();
		try
		{
			FileOutputStream stream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			byte[] buffer = new byte[1024];
			
			while (fileStream.read(buffer) != -1)
			{
				stream.write(buffer);
			}
			
			stream.close();
			
			String storedFilePath = this.GeneratePath(context, fileName);
			
			this.AddFileParameter(paramName, storedFilePath, true, context, postFileName);
		}
		catch (Exception e)
		{
			this.isValidRequest = false;
			this.webClientReponse.occuredException = e;
		}
	}
	
	public void AddFileParameter(String pathName, byte[] fileBytes, Context context)
	{
		this.AddFileParameter(pathName, fileBytes, context, null);
	}
	
	public void AddFileParameter(String pathName, byte[] fileBytes, Context context, String postFileName)
	{
		String fileName = UUID.randomUUID().toString();
		try
		{
			FileOutputStream stream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			stream.write(fileBytes);
			stream.close();
			
			String storedFilePath = this.GeneratePath(context, fileName);
			
			this.AddFileParameter(pathName, storedFilePath, true, context, postFileName);
		}
		catch (Exception e)
		{
			this.isValidRequest = false;
			this.webClientReponse.occuredException = e;
		}
	}
	
	public void AddRequestCompleteCallBack(String callBack, Class<?> reciever, Object recieverInstance)
	{
		try
		{
			CallbackReciever rec = new CallbackReciever();
			rec.Reciever = reciever;
			if (recieverInstance != null)
			{
				rec.IsStatic = false;
				rec.RecieverInstance = recieverInstance;
			}
			else
			{
				rec.IsStatic = true;
				rec.RecieverInstance = null;
			}
			rec.CallbackMethod = reciever.getMethod(callBack, WebClientResponse.class, Object.class);
			
			this.reqCompleteCallbacks.add(rec);
		}
		catch (NoSuchMethodException exp)
		{
			this.isValidRequest = false;
			this.webClientReponse.occuredException = exp;
		}
		catch(Exception e)
		{
			this.isValidRequest = false;
			this.webClientReponse.occuredException = e;
		}
	}

	public void AttachCallBackObject(Object userState)
	{
		this.userState = userState;
	}
	
	public void AttachCookies(List<Cookie> cookies)
	{
		this.cookies = new BasicCookieStore();
		
		for (Cookie cookie : cookies)
		{
			this.cookies.addCookie(cookie);
		}
	}
	
	public void AttachHeaders(List<NameValuePair> headers)
	{
		this.headers = new ArrayList<NameValuePair>();
		
		for (NameValuePair nvp : headers)
		{
			this.headers.add(nvp);
		}
	}
	
	@Override
	protected Void doInBackground(Void... params)
	{
		if (!this.isValidRequest)
			return null;
		
		this.webClientReponse.callStatus = WebClientCallStatus.InProgress;
		
		this.InitializeHttpClient();
		
		if (!(isValidRequest && this.isHttpClientInitialized))
		{
			this.isValidRequest = false;
			return null;
		}
		
		if (this.headers != null)
		{
			this.AttachHeaders();
			
			if (!(isValidRequest && this.isHeadersAttached))
			{
				this.isValidRequest = false;
				return null;
			}
		}
		
		if (this.cookies != null)
		{
			this.AttachCookies();
			
			if (!(isValidRequest && this.isCookiesAttached))
			{
				this.isValidRequest = false;
				return null;
			}
		}
		
		HttpResponse httpReponse = null;
		HttpPost httpPost = null;
		HttpGet httpGet = null;
		
		try
		{
			if (this.reqType == RequestType.GET)
			{
		        httpGet = new HttpGet(this.url);
	        	httpReponse = this.httpClient.execute(httpGet, this.httpCallContext);
			}
			else
			{
				httpPost = new HttpPost(this.url);
				HttpEntity entity = this.GetRequestEntity();
				if (entity != null)
					httpPost.setEntity(entity);
				else
				{
					if (!this.isValidRequest)
					{
						throw new UnsupportedEncodingException();
					}
				}
				httpReponse = this.httpClient.execute(httpPost, this.httpCallContext);
			}
		}
		catch (Exception e)
		{			
			this.webClientReponse.callStatus = WebClientCallStatus.Failed;
			
			if (this.reqType == RequestType.GET)
			{
				httpGet.abort();
			}
			else
			{
				httpPost.abort();
			}
			
			if((this.httpClient instanceof DefaultHttpClient)) 
            { 
				this.httpClient.getConnectionManager().shutdown();
            }
			
			this.isValidRequest = false;
			this.webClientReponse.occuredException = e;
		}
	    
	    if (!(this.webClientReponse.callStatus == WebClientCallStatus.Failed))
	    {	
		    try
		    {
		    	this.webClientReponse.status = httpReponse.getStatusLine().getStatusCode();
		    	this.webClientReponse.headers = httpReponse.getAllHeaders();
		    	
		    	WebClientCookieExtractor extractor = new WebClientCookieExtractor();
		    	this.webClientReponse.responseCookies = extractor.GetCookies(this.webClientReponse.headers);
		    	
		    	this.webClientReponse.requestUri = (HttpUriRequest) this.httpCallContext.getAttribute(ExecutionContext.HTTP_REQUEST); 
		    	this.webClientReponse.requestHost = (HttpHost) this.httpCallContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		    	
		    	HttpEntity getResEntity = httpReponse.getEntity();
		    	
		        if(getResEntity != null) 
		        {
		        	InputStream input = getResEntity.getContent();
		        	this.webClientReponse.SetResponseStream(input);
		        	
		        	input.close();
	            }
		        
		        this.webClientReponse.callStatus = WebClientCallStatus.Complete;
		    }
		    catch (Exception e)
		    {
		    	this.webClientReponse.occuredException = e;
		    	this.isValidRequest = false;
		    	this.webClientReponse.callStatus = WebClientCallStatus.Failed;
		    }	        
			finally 
			{
				if((this.httpClient instanceof DefaultHttpClient)) 
	            { 
					this.httpClient.getConnectionManager().shutdown();
	            }
	        }
	    }
	    return null;
	}
	
	@Override
	protected void onPostExecute(Void result)
	{
		// TODO Auto-generated method stub
		super.onPostExecute(result); 
		
		if (this.reqCompleteCallbacks.size() > 0)
		{
			for (int i = 0; i < this.reqCompleteCallbacks.size(); i++)
			{
				try
				{
					if (this.reqCompleteCallbacks.get(i).IsStatic)
					{
						this.reqCompleteCallbacks.get(i).CallbackMethod.invoke(null, this.webClientReponse, this.userState);
					}
					else
					{
						this.reqCompleteCallbacks.get(i).CallbackMethod.invoke(this.reqCompleteCallbacks.get(i).RecieverInstance, this.webClientReponse, this.userState);
					}
				}
				catch (Exception e)
				{
					
				}
			}
		}
		
		this.RemoveTemperaryFiles();
	}
}
