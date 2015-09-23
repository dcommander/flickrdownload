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
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.people.User;

public class Configuration {
	public User authUser;
	public User photosUser;
	public File photosBaseDirectory;
	public File authDirectory;
	public File buddyIconFilename;
	public boolean downloadExifData = false;
	public boolean downloadCollectionIcons = false;
	public boolean alwaysDownloadBuddyIcon = false;
	public boolean partialDownloads = false;
	public boolean onlyData = false;
	public boolean onlyOriginals = false;
	public boolean useTitles = false;
	public String addExtensionToUnknownFiles;
	public List<String> limitDownloadsToSets;
	public Auth auth;
	
	public Configuration(Flickr flickr, File photosBaseDirectory, File authDirectory, String userName) throws IOException, FlickrException, SAXException, ParserConfigurationException {
		this.photosBaseDirectory = photosBaseDirectory;
		this.photosBaseDirectory.mkdirs();

		this.authDirectory = authDirectory != null ? authDirectory : photosBaseDirectory;
		this.authDirectory.mkdirs();

		this.auth = Authentication.authorize(flickr, this.authDirectory, userName);
		this.authUser = flickr.getPeopleInterface().getInfo(this.auth.getUser().getId());
	}
}
