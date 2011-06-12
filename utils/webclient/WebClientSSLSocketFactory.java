/*
	Author : Amir Ali Jiwani
	Email : amir.ali@pi-labs.net
*/

package utils.webclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class WebClientSSLSocketFactory implements SocketFactory, LayeredSocketFactory
{
	private static SSLContext sc = null;

	// always verify the host - dont check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier()
	{
	        public boolean verify(String hostname, SSLSession session)
	        {
	                return true;
	        }
	};

	// Create a trust manager that does not validate certificate chains
    static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
    {
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {
            return new java.security.cert.X509Certificate[] {};
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }
    } };

	private static SSLContext createEasySSLContext() throws IOException
	{
		try
		{
	        sc = SSLContext.getInstance("TLS");
	        sc.init(null, trustAllCerts, null);
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	        return sc;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private SSLContext getSSLContext() throws IOException
	{
        if (sc == null)
        {
    		createEasySSLContext();
        }
        return sc;
	}

	/**
     * @see org.apache.http.conn.scheme.SocketFactory#connectSocket(java.net.Socket,
     *      java.lang.String, int, java.net.InetAddress, int,
     *      org.apache.http.params.HttpParams)
     */
    public Socket connectSocket(Socket sock, String host, int port,
                    InetAddress localAddress, int localPort, HttpParams params)
                    throws IOException, UnknownHostException, ConnectTimeoutException {
            int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
            int soTimeout = HttpConnectionParams.getSoTimeout(params);

            InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
            SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

            if ((localAddress != null) || (localPort > 0)) {
                    // we need to bind explicitly
                    if (localPort < 0) {
                            localPort = 0; // indicates "any"
                    }
                    InetSocketAddress isa = new InetSocketAddress(localAddress,
                                    localPort);
                    sslsock.bind(isa);
            }

            sslsock.connect(remoteAddress, connTimeout);
            sslsock.setSoTimeout(soTimeout);
            return sslsock;
    }

	public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3)
			throws IOException, UnknownHostException {
		// TODO Auto-generated method stub
		return getSSLContext().getSocketFactory().createSocket(arg0, arg1, arg2, arg3);
	}

	public Socket createSocket() throws IOException {
		// TODO Auto-generated method stub
		return getSSLContext().getSocketFactory().createSocket();
	}

	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(
        		WebClientSSLSocketFactory.class));
	}

	public int hashCode() {
	        return WebClientSSLSocketFactory.class.hashCode();
	}
}
