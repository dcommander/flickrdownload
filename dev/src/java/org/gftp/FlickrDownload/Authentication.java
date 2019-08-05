/*
  FlickrDownload - Copyright(C) 2010 Brian Masney <masneyb@onstation.org>.
                 - Copyright(C) 2019 D. R. Commander.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
*/

package org.gftp.FlickrDownload;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.Response;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;

import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth1Token;

public class Authentication {
	public static Flickr getFlickr() throws ParserConfigurationException {
		return new Flickr("16c1a6a31f28e670500d02f6b13935b1", "0fa4d39da5eab415", new RESTRetryTransport());
	}

	public static Auth authorize(Flickr flickr, File authDirectory, String username) throws IOException, SAXException, FlickrException {
		AuthStore authStore = new FileAuthStore(authDirectory);
		Auth auth = authStore.retrieve(flickr.getPeopleInterface().findByUsername(username).getId());
		if (auth != null) {
			RequestContext.getRequestContext().setAuth(auth);
			return auth;
		}

		AuthInterface authInterface = flickr.getAuthInterface();
		OAuth1RequestToken requestToken = authInterface.getRequestToken();

		String url =
			authInterface.getAuthorizationUrl(requestToken, Permission.READ);
		System.out.println("Please visit the following URL to get your authorization token:");
		System.out.println();
		System.out.println(url);
		System.out.println();
		System.out.print("Enter your token: ");
		String tokenKey = new Scanner(System.in).nextLine();

		OAuth1Token accessToken =
			authInterface.getAccessToken(requestToken, tokenKey);

		auth = authInterface.checkToken(accessToken);
		RequestContext.getRequestContext().setAuth(auth);
		authStore.store(auth);
		return auth;
	}

	protected static class RESTRetryTransport extends REST {
		public RESTRetryTransport() throws ParserConfigurationException {
			super();
		}

		@Override
		public Response get(String path, Map<String, Object> parameters, String apiKey, String sharedSecret) {
			while (true) {
				try {
					return super.get(path, parameters, apiKey, sharedSecret);
				}
				catch (Exception e) {
					Logger.getLogger(getClass()).warn(String.format("Get operation failed, retrying: %s", e.getMessage()), e);
					try {
						Thread.sleep(4000);
					}
					catch (InterruptedException ie) {
						// NOOP
					}
				}
			}
		}

		@Override
		public Response post(String path, Map<String, Object> parameters, String apiKey, String sharedSecret) {
			while (true) {
				try {
					return super.post(path, parameters, apiKey, sharedSecret);
				}
				catch (Exception e) {
					Logger.getLogger(getClass()).warn(String.format("Post operation failed, retrying: %s", e.getMessage()), e);
					try {
						Thread.sleep(4000);
					}
					catch (InterruptedException ie) {
						// NOOP
					}
				}
			}
		}
	}
}
