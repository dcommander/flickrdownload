/*
  FlickrDownload - Copyright(C) 2010 Brian Masney <masneyb@onstation.org>.
                 - Copyright(C) 2015 D. R. Commander.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
*/

package org.gftp.FlickrDownload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;

import javax.xml.ws.http.HTTPException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class IOUtils {
	public static void copy(InputStream istr, OutputStream ostr) throws IOException {
		byte[] buffer = new byte[4096];
		int n;
		while ((n = istr.read(buffer)) != -1) {
			ostr.write(buffer, 0, n);
		}
	}

	public static String md5Sum(File file) {
		InputStream istr = null;
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			istr = new FileInputStream(file);
			byte[] buffer = new byte[4096];
			int n;
			while ((n = istr.read(buffer)) != -1) {
				digest.update(buffer, 0, n);
			}
			istr.close();
			return new BigInteger(1, digest.digest()).toString(16);
		}
		catch (Exception e) {
			Logger.getLogger(IOUtils.class).error(String.format("Could not get md5sum of %s: %s", file, e.getMessage()), e);
			return "";
		}
	}
	

	public static void copyToFileAndCloseStreams(InputStream istr, File destFile) throws IOException {
		OutputStream ostr = null;
		try {
 			ostr = new FileOutputStream(destFile);
 			IOUtils.copy(istr, ostr);
		}
		finally {
			if (ostr != null)
				ostr.close();
			if (istr != null)
				istr.close();
		}
	}

	public static boolean sameSize(String url, File destFile) throws IOException, HTTPException {
		long localFileSize = destFile.length(), remoteFileSize = -1;

		HttpClient client = new HttpClient();
		HeadMethod head = new HeadMethod(url);
		head.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		int code = client.executeMethod(head);
		if (code >= 200 && code < 300) {
			remoteFileSize = head.getResponseContentLength();
			if (localFileSize == remoteFileSize) {
				Logger.getLogger(IOUtils.class).debug(String.format("%s and %s are the same size", destFile.getName(), url));
				return true;
      }
		}
		else {
			Logger.getLogger(IOUtils.class).warn("Got HTTP response code " + code + " when trying to check size of " + url);
			return false;
		}

		Logger.getLogger(IOUtils.class).debug(String.format("Local file size %d != remote file size %d", localFileSize, remoteFileSize));
		return false;
	}

	public static void downloadUrl(String url, File destFile) throws IOException, HTTPException {
		File tmpFile = new File(destFile.getAbsoluteFile() + ".tmp");
		Logger.getLogger(IOUtils.class).debug(String.format("Downloading URL %s to %s", url, tmpFile));

		tmpFile.getParentFile().mkdirs();

        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        get.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        int code = client.executeMethod(get);
        if (code >= 200 && code < 300) {
        	copyToFileAndCloseStreams(get.getResponseBodyAsStream(), tmpFile);
            tmpFile.renameTo(destFile);
        }
        else
        	Logger.getLogger(IOUtils.class).fatal("Got HTTP response code " + code + " when trying to download " + url);
	}

	private static String getRemoteFilename(String url) throws IOException, HTTPException {
        HttpClient client = new HttpClient();
        HeadMethod get = new HeadMethod(url);
        get.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        int code = client.executeMethod(get);
        
        if (code >= 200 && code < 400) {
            Header disposition = get.getResponseHeader("Content-Disposition");
            if (disposition != null)
            	return disposition.getValue().replace("attachment; filename=", "");
        }
       	Logger.getLogger(IOUtils.class).fatal("Got HTTP response code " + code + " when trying to download " + url + ". Returning null.");
        return null;
	}

	public static String getVideoExtension(String url) throws IOException, HTTPException {
		String filename = getRemoteFilename(url);
		if (filename == null || filename.endsWith("."))
			return "mp4"; // FIXME
		return filename.substring(filename.lastIndexOf(".") + 1);
	}
	
	public static void findFilesThatDoNotBelong(File setDir, Collection<String> expectedFiles, String addExtensionToUnknownFiles) {
		for (String file : setDir.list()) {
			if (expectedFiles.contains(file))
				continue;

			if (StringUtils.isNotBlank(addExtensionToUnknownFiles) && !file.endsWith(addExtensionToUnknownFiles)) {
				File oldFile = new File(setDir, file);
				File newFile = new File(setDir, file + "." + addExtensionToUnknownFiles);
				Logger.getLogger(IOUtils.class).warn(String.format("Unexpected file %s, adding %s extension.", new File(setDir, file).getAbsoluteFile(), addExtensionToUnknownFiles));
				try {
					oldFile.renameTo(newFile);
				}
				catch (Exception e) {
					Logger.getLogger(IOUtils.class).warn(String.format("Error renaming %s to %s: %s", oldFile.getAbsolutePath(), newFile.getAbsolutePath(), e.getMessage()));
				}
			}
			else {
				Logger.getLogger(IOUtils.class).warn(String.format("Unexpected file %s.", new File(setDir, file).getAbsoluteFile()));
			}
		}
	}
}
