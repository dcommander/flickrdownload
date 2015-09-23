/*
  FlickrDownload - Copyright(C) 2010-2011 Brian Masney <masneyb@onstation.org>.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.PeopleInterface;
import com.flickr4java.flickr.people.User;

public class FlickrDownload {
	public static String ALL_COLLECTIONS_HTML_FILENAME = "collections.html";
	public static String ALL_SETS_HTML_FILENAME = "sets.html";
	public static String STATS_HTML_FILENAME = "stats.html";
	public static String TOPLEVEL_XML_FILENAME = "flickr.xml";
	public static String PHOTOS_CSS_FILENAME = "photos.css";
	public static String PLAY_ICON_FILENAME = "play_icon.png";

	protected static class Arguments {
		@Option(name="--authDir", required=false)
		public String authDirectory;

		@Option(name="--authUsername", required=false)
		public String authUsername;

		@Option(name="--photosUsername", required=false)
		public String photosUsername; // FIXME - don't make this mandatory

		@Option(name="--photosDir", required=true)
		public String photosDirectory;

		@Option(name="--partial", required=false)
		public boolean partial = false;

		@Option(name="--addExtensionToUnknownFiles", required=false)
		public String addExtensionToUnknownFiles;

		@Option(name="--limitToSet", required=false, multiValued=true)
		public List<String> limitDownloadsToSets = new ArrayList<String>();		

		@Option(name="--debug", required=false)
		public boolean debug = false;
		
		@Option(name="--downloadExifData", required=false)
		public boolean downloadExifData = false;

		@Option(name="--onlyData", required=false)
		public boolean onlyData = false;

		@Option(name="--onlyOriginals", required=false)
		public boolean onlyOriginals = false;

		@Option(name="--useTitles", required=false)
		public boolean useTitles = false;

		@Option(name="--checkSize", required=false)
		public boolean checkSize = false;
	}

	private static File getToplevelXmlFilename(File photosBaseDirectory) {
		return new File(photosBaseDirectory, TOPLEVEL_XML_FILENAME);
	}

	private static Collection<String> createTopLevelFiles(Configuration configuration, Collections collections, Sets sets) throws FlickrException, SAXException, IOException, JDOMException, TransformerException {
		Collection<String> createdFiles = new HashSet<String>();

		File toplevelXmlFilename = getToplevelXmlFilename(configuration.photosBaseDirectory);
		Logger.getLogger(FlickrDownload.class).info("Creating XML file " + toplevelXmlFilename.getAbsolutePath());

		MediaIndexer indexer = new XmlMediaIndexer(configuration);
		Element toplevel = new Element("flickr")
			.addContent(XmlUtils.createApplicationXml())
			.addContent(XmlUtils.createUserXml(configuration))
			.addContent(collections.createTopLevelXml())
			.addContent(sets.createTopLevelXml())
			.addContent(new Stats(sets).createStatsXml(indexer));

		createdFiles.addAll(indexer.writeIndex());

		XmlUtils.outputXmlFile(toplevelXmlFilename, toplevel);
		createdFiles.add(toplevelXmlFilename.getName());

		Logger.getLogger(FlickrDownload.class).info("Copying support files and performing XSLT transformations");

		IOUtils.copyToFileAndCloseStreams(XmlUtils.class.getResourceAsStream("xslt/" + PHOTOS_CSS_FILENAME),
				new File(configuration.photosBaseDirectory, PHOTOS_CSS_FILENAME));
		createdFiles.add(PHOTOS_CSS_FILENAME);

		IOUtils.copyToFileAndCloseStreams(XmlUtils.class.getResourceAsStream("xslt/" + PLAY_ICON_FILENAME),
				new File(configuration.photosBaseDirectory, PLAY_ICON_FILENAME));
		createdFiles.add(PLAY_ICON_FILENAME);

		XmlUtils.performXsltTransformation(configuration, "all_sets.xsl", toplevelXmlFilename,
				new File(configuration.photosBaseDirectory, ALL_SETS_HTML_FILENAME));
		createdFiles.add(ALL_SETS_HTML_FILENAME);

		XmlUtils.performXsltTransformation(configuration, "all_collections.xsl", toplevelXmlFilename,
				new File(configuration.photosBaseDirectory, ALL_COLLECTIONS_HTML_FILENAME));
		createdFiles.add(ALL_COLLECTIONS_HTML_FILENAME);
		createdFiles.add(Collections.COLLECTIONS_ICON_DIRECTORY);

		XmlUtils.performXsltTransformation(configuration, "stats.xsl", toplevelXmlFilename,
				new File(configuration.photosBaseDirectory, STATS_HTML_FILENAME));
		createdFiles.add(STATS_HTML_FILENAME);

		sets.performXsltTransformation();
		for (AbstractSet set : sets.getSets()) {
			createdFiles.add(set.getSetId());
		}
		return createdFiles;
	}

	public static String getApplicationName() {
		return StringUtils.defaultString(FlickrDownload.class.getPackage().getImplementationTitle(), "FlickrDownload");
	}

	public static String getApplicationVersion() {
		return StringUtils.defaultString(FlickrDownload.class.getPackage().getImplementationVersion(), "?");
	}

	public static String getApplicationWebsite() {
		return "http://www.onstation.org/flickrdownload/";
	}

	public static String sanitizeTitle(String title) {
		String sanitized = title.replaceAll("[^a-zA-Z0-9_\\-\\.\\ \\+]", "_").trim();
		return sanitized;
	}

	private static void usage(CmdLineParser parser, String error) {
		System.err.println(error);
		System.err.print("usage: FlickrDownload ");
		parser.printSingleLineUsage(System.err);
		System.err.println();
		System.exit(1);
	}

	public static void main(String[] args) throws Exception {
		System.out.println(String.format("%s %s - Copyright(C) 2007,2010-2011 Brian Masney <masneyb@onstation.org>.", 
				getApplicationName(), getApplicationVersion()));
		System.out.println("If you have any questions, comments, or suggestions about this program, please");
		System.out.println("feel free to email them to me. You can always find out the latest news about");
		System.out.println(String.format("%s from my website at %s.", getApplicationName(), getApplicationWebsite()));
		System.out.println();
		System.out.println(String.format("%s is distributed under the terms of the GPLv3 and comes with", getApplicationName()));
		System.out.println("ABSOLUTELY NO WARRANTY; for details, see the COPYING file. This is free");
		System.out.println("software, and you are welcome to redistribute it under certain conditions;");
		System.out.println("for details, see COPYING file.");
		System.out.println();

		Arguments values = new Arguments();
		CmdLineParser parser = new CmdLineParser(values);

		try {
			parser.parseArgument(args);
		}
		catch (CmdLineException e) {
			usage(parser, e.getMessage());
		}

		File toplevelXmlFilename = getToplevelXmlFilename(new File(values.photosDirectory));
		if (StringUtils.isBlank(values.authUsername))
			values.authUsername = Stats.getAuthUsername(toplevelXmlFilename);
		if (StringUtils.isBlank(values.photosUsername))
			values.photosUsername = Stats.getPhotosUsername(toplevelXmlFilename);

		if (values.authUsername == null)
			usage(parser, "--authUsername must be specified");

		Collection<String> createdToplevelFiles = new HashSet<String>();

		Flickr flickr = Authentication.getFlickr();
		Configuration configuration = new Configuration(
				flickr,
				new File(values.photosDirectory), 
					StringUtils.isBlank(values.authDirectory) ? null : new File(values.authDirectory), 
				values.authUsername);

		createdToplevelFiles.add(configuration.authUser.getId() + ".auth");

		if (StringUtils.isBlank(values.photosUsername))
			configuration.photosUser = configuration.authUser;
		else {
			PeopleInterface pi = flickr.getPeopleInterface();
			User pu = pi.findByUsername(values.photosUsername);
			if (pu == null)
				throw new IllegalArgumentException("Cannot find user with ID " + values.photosUsername);
			configuration.photosUser = pi.getInfo(pu.getId());
		} 

        configuration.onlyData = values.onlyData;
        configuration.onlyOriginals = values.onlyOriginals;
		configuration.useTitles = values.useTitles;
		configuration.checkSize = values.checkSize;

		configuration.buddyIconFilename = new File(configuration.photosBaseDirectory, configuration.photosUser.getRealName() + ".jpg");
		if (!configuration.onlyData && (configuration.alwaysDownloadBuddyIcon || !configuration.buddyIconFilename.exists()))
			IOUtils.downloadUrl(configuration.photosUser.getSecureBuddyIconUrl(), 
					configuration.buddyIconFilename);
		createdToplevelFiles.add(configuration.buddyIconFilename.getName());

		configuration.downloadExifData = values.downloadExifData;
		configuration.partialDownloads = values.partial;
		configuration.addExtensionToUnknownFiles = values.addExtensionToUnknownFiles;
        configuration.limitDownloadsToSets = values.limitDownloadsToSets;

		if (values.debug) {
			Flickr.debugRequest = true;
			Flickr.debugStream = true;
		}

		Collections collections = new Collections(configuration, flickr);
		Sets sets = new Sets(configuration, flickr);

		// The photos must be downloaded before the toplevel XML files are created
		sets.downloadAllPhotos();

		createdToplevelFiles.addAll(createTopLevelFiles(configuration, collections, sets));

		IOUtils.findFilesThatDoNotBelong(configuration.photosBaseDirectory, createdToplevelFiles, configuration.addExtensionToUnknownFiles);
	}
}
