/*
  FlickrDownload - Copyright(C) 2011 Brian Masney <masneyb@onstation.org>.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
*/

package org.gftp.FlickrDownload;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.FlickrException;

public class Stats {
	private Sets sets;

	public Stats(Sets sets) {
		this.sets = sets;
	}

	public Element createStatsXml(MediaIndexer mediaIndexer) throws FlickrException, IOException, SAXException, JDOMException {
		Map<String, MediaStats> allStats = new HashMap<String, MediaStats>();

		for (AbstractSet set : this.sets.getSets()) {
			File setXmlFilename = set.getSetXmlFilename();
			if (!setXmlFilename.exists())
				continue;

			processXmlFile(setXmlFilename, allStats, set.getSetId(), mediaIndexer);
		}

		return generateStatsXml(allStats);
	}

	public static String getPhotosUsername(File xmlFilename) throws IOException, JDOMException {
		if (!xmlFilename.exists())
			return null;

		SAXBuilder builder = new SAXBuilder();
		Reader in = null;
		try {
			in = new FileReader(xmlFilename);
			Document doc = builder.build(in);
			Element root = doc.getRootElement();

			Element userEle = root.getChild("user");
			if (userEle == null)
				return null;

			Element usernameEle = userEle.getChild("username");
			if (usernameEle == null)
				return null;

			return usernameEle.getText();
		}
		finally {
			in.close();
		}
	}

	public static String getAuthUsername(File xmlFilename) throws IOException, JDOMException {
		if (!xmlFilename.exists())
			return null;

		SAXBuilder builder = new SAXBuilder();
		Reader in = null;
		try {
			in = new FileReader(xmlFilename);
			Document doc = builder.build(in);
			Element root = doc.getRootElement();

			Element userEle = root.getChild("user");
			if (userEle == null)
				return null;

			Element usernameEle = userEle.getChild("authUser");
			if (usernameEle == null)
				return null;

			return usernameEle.getAttributeValue("username");
		}
		finally {
			in.close();
		}
	}

	public static void processXmlFile(File setXmlFilename, Map<String, MediaStats> allStats, String setId, MediaIndexer mediaIndexer) throws JDOMException, IOException {
		if (!setXmlFilename.exists())
			return;

		SAXBuilder builder = new SAXBuilder();
		Reader in = null;
		try {
			in = new FileReader(setXmlFilename);
			Document doc = builder.build(in);
			Element root = doc.getRootElement();
			if (!root.getName().equals("set")) {
				Logger.getLogger(Stats.class).warn(String.format("Skipping file %s since it does not have the expected root element of 'set'", setXmlFilename.getAbsolutePath()));
				return;
			}

			Logger.getLogger(Stats.class).info(String.format("Gathering statistics from file %s", setXmlFilename.getAbsolutePath()));

			for (Object mediaTemp : (List<?>) root.getChildren("media")) {
				Element mediaElement = (Element) mediaTemp; // because getChildren returns List, not List<Element>
				mediaIndexer.addToIndex(setId, mediaElement);

				String type = mediaElement.getAttributeValue("type");
				if (!allStats.containsKey(type))
					allStats.put(type, new MediaStats());

				MediaStats stats = allStats.get(type);

				String mediaId = mediaElement.getChildText("id");
				String mediumMd5sum = null;

				for (Object imageTemp : (List<?>) mediaElement.getChildren("image")) {
					Element imageElement = (Element) imageTemp;
					String sizeStr = imageElement.getAttributeValue("size");
					if (sizeStr == null)
						continue;

					String imageType = imageElement.getAttributeValue("type");
					Long size = Long.valueOf(sizeStr);
					if (stats.diskSpaceByFileType.containsKey(imageType))
						size += stats.diskSpaceByFileType.get(imageType);
					stats.diskSpaceByFileType.put(imageType, size);

					if (imageType.equals(AbstractSet.MEDIUM_PHOTO_DESCRIPTION))
						mediumMd5sum = StringUtils.trimToNull(imageElement.getAttributeValue("md5sum"));
				}

				// Be sure to calculate the disk space before this check...
				if (stats.mediaIdsSeen.contains(mediaId)) {
					stats.duplicatePhotos++;
					continue;
				}
				stats.mediaIdsSeen.add(mediaId);
				stats.totalPhotos++;

				if (mediumMd5sum != null) {
					if (!stats.md5sums.containsKey(mediumMd5sum))
						stats.md5sums.put(mediumMd5sum, new ArrayList<String>());
					stats.md5sums.get(mediumMd5sum).add(mediaId);
				}

				Element privacy = mediaElement.getChild("privacy");
				if (privacy.getAttributeValue("public").equals("1"))
					stats.publicPhotos++;
				else if (privacy.getAttributeValue("friends").equals("1") && privacy.getAttributeValue("family").equals("1"))
					stats.friendsAndFamilyPhotos++;
				else if (privacy.getAttributeValue("family").equals("1"))
					stats.familyOnlyPhotos++;
				else if (privacy.getAttributeValue("friends").equals("1"))
					stats.friendsOnlyPhotos++;
				else
					stats.privatePhotos++;

				if (mediaElement.getChild("tags").getChildren("tag").size() > 0)
					stats.tagged++;
				else
					stats.notTagged++;

				Element geodata = mediaElement.getChild("geodata");
				if (geodata.getAttributeValue("latitude").equals(""))
					stats.notGeotagged++;
				else
					stats.geotagged++;

				String license = mediaElement.getChildText("license");
				Integer num = 1;
				if (stats.licenses.containsKey(license))
					num += stats.licenses.get(license);
				stats.licenses.put(license, num);
			}
		}
		finally {
			if (in != null)
				in.close();
		}
	}

	public static Element generateStatsXml(Map<String, MediaStats> allStats) {
		Element topLevelStats = new Element("media_stats");

		for (String mediaType : allStats.keySet()) {
			MediaStats stats = allStats.get(mediaType);

			Element diskSpaceElement = new Element("disk_space");
			for (String type : stats.diskSpaceByFileType.keySet()) {
				diskSpaceElement.addContent(new Element("image")
					.setAttribute("type", type)
					.setText(String.valueOf(stats.diskSpaceByFileType.get(type))));
			}

			Element licensesElement = new Element("licenses");
			for (String type : stats.licenses.keySet()) {
				licensesElement.addContent(new Element("license")
					.setAttribute("type", type)
					.setText(String.valueOf(stats.licenses.get(type))));
			}

			Element duplicatesElement = new Element("duplicates");
			for (String md5sum : stats.md5sums.keySet()) {
				List<String> photoIds = stats.md5sums.get(md5sum);
				if (photoIds.size() <= 1)
					continue;

				Element dupEle = new Element("duplicate");
				for (String photoId : photoIds) {
					dupEle.addContent(new Element("photo_id").setText(photoId));
				}
				duplicatesElement.addContent(dupEle);
			}

			topLevelStats.addContent(new Element("media")
				.setAttribute("type", WordUtils.capitalize(mediaType))
				.addContent(new Element("photo_counts")
					.setAttribute("total", String.valueOf(stats.totalPhotos))
					.setAttribute("additional_duplicate", String.valueOf(stats.duplicatePhotos))
					.addContent(new Element("public").setText(String.valueOf(stats.publicPhotos)))
					.addContent(new Element("private").setText(String.valueOf(stats.privatePhotos)))
					.addContent(new Element("friendsOnly").setText(String.valueOf(stats.friendsOnlyPhotos)))
					.addContent(new Element("familyOnly").setText(String.valueOf(stats.familyOnlyPhotos)))
					.addContent(new Element("friendsAndFamily").setText(String.valueOf(stats.friendsAndFamilyPhotos)))
				)
				.addContent(new Element("tagged")
					.addContent(new Element("yes").setText(String.valueOf(stats.tagged)))
					.addContent(new Element("no").setText(String.valueOf(stats.notTagged)))
				)
				.addContent(new Element("geoTagged")
					.addContent(new Element("yes").setText(String.valueOf(stats.geotagged)))
					.addContent(new Element("no").setText(String.valueOf(stats.notGeotagged)))
				)
				.addContent(diskSpaceElement)
				.addContent(licensesElement)
				.addContent(duplicatesElement)
			);
		}
		return topLevelStats;
	}

	public static int getMediaCount(File setXmlFilename, String setId) throws IOException, JDOMException {
		int total = 0;
		Map<String, MediaStats> allStats = new HashMap<String, MediaStats>();
		processXmlFile(setXmlFilename, allStats, setId, NoopMediaIndexer.INSTANCE);
		for (String key : allStats.keySet()) {
			total += allStats.get(key).totalPhotos;
		}
		return total;
	}

	public static class MediaStats {
		Collection<String> mediaIdsSeen = new HashSet<String>();
		public Map<String, List<String>> md5sums = new HashMap<String, List<String>>();

		public int totalPhotos = 0;

		public int duplicatePhotos = 0;

		public int publicPhotos = 0;
		public int privatePhotos = 0;
		public int friendsOnlyPhotos = 0;
		public int familyOnlyPhotos = 0;
		public int friendsAndFamilyPhotos = 0;

		public int tagged = 0;
		public int notTagged = 0;

		public int geotagged = 0;
		public int notGeotagged = 0;

		public Map<String, Long> diskSpaceByFileType = new LinkedHashMap<String, Long>();

		public Map<String, Integer> licenses = new HashMap<String, Integer>();
	}
}
