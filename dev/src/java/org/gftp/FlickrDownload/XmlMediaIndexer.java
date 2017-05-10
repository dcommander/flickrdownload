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
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.gftp.FlickrDownload.XmlUtils.XsltParameter;
import org.jdom.Attribute;
import org.jdom.Element;

public class XmlMediaIndexer implements MediaIndexer {
	private static SimpleDateFormat rawDateInput = new SimpleDateFormat(XmlUtils.RAW_DATE_FORMAT);
	private static final String INDEX_XML_FILENAME = "index.xml";
	private static final String INDEX_HTML_FILENAME = "archive.html";

	private Map<String, MainPhotoIndexEntry> mainEntries = new HashMap<String, MainPhotoIndexEntry>();
	private SortedMap<DateGroup, SortedSet<MediaEntryByDate>> takenByDate = new TreeMap<DateGroup, SortedSet<MediaEntryByDate>>();

	private Configuration configuration;

	public XmlMediaIndexer(Configuration configuration) {
		this.configuration = configuration;
	}

	public void addToIndex(String setId, Element mediaElement) {
		String mediaId = mediaElement.getChildText("id");
		if (!this.mainEntries.containsKey(mediaId)) {
			MainPhotoIndexEntry entry = new MainPhotoIndexEntry();
			entry.sets.add(setId);
			entry.xmlSnippet = cloneXml(mediaElement);
			this.mainEntries.put(mediaId, entry);
		}
		else
			this.mainEntries.get(mediaId).sets.add(setId);

		String dateTimeStr = mediaElement.getChild("dates").getChild("taken").getAttributeValue("raw");
		Date date;
		try {
			date = rawDateInput.parse(dateTimeStr);
		}
		catch (ParseException e) {
			Logger.getLogger(getClass()).warn(String.format("Cannot parse date/time %s: %s", dateTimeStr, e.getMessage()));
			return;
		}

		DateGroup dateGroup = new DateGroup(date);
		if (!this.takenByDate.containsKey(dateGroup))
			this.takenByDate.put(dateGroup, new TreeSet<MediaEntryByDate>());
		this.takenByDate.get(dateGroup).add(new MediaEntryByDate(mediaId, date));
	}

	private Element cloneXml(Element origEle) {
		Element newEle = new Element(origEle.getName());
		for (Object attrTemp : (List<?>) origEle.getAttributes()) {
			Attribute attr = (Attribute) attrTemp; // because jdom doesn't use generics
			newEle.setAttribute(attr.getName(), attr.getValue());
		}
		if (origEle.getText() != null)
			newEle.setText(origEle.getText());
		for (Object childTemp : (List<?>) origEle.getChildren()) {
			Element child = (Element) childTemp;
			newEle.addContent(cloneXml(child));
		}
		return newEle;
	}

	private Element generateStatsXml() {
		Element parent = new Element("index")
			.addContent(XmlUtils.createApplicationXml())
			.addContent(XmlUtils.createUserXml(this.configuration));

		Element allMedia = new Element("all_media");
		for (MainPhotoIndexEntry entry : this.mainEntries.values()) {
			Element setsXml = new Element("sets");
			for (String id : entry.sets) {
				setsXml.addContent(new Element("set").setAttribute("id", id));
			}
			entry.xmlSnippet.addContent(setsXml);
			allMedia.addContent(entry.xmlSnippet);
		}
		parent.addContent(allMedia);

		Element byDateTaken = new Element("by_date_taken");
		SortedMap<String, SortedSet<DateGroup>> yearMonths = new TreeMap<String, SortedSet<DateGroup>>();
		for (DateGroup date : this.takenByDate.keySet()) {
			if (!yearMonths.containsKey(date.year))
				yearMonths.put(date.year, new TreeSet<DateGroup>());
			yearMonths.get(date.year).add(date);

			Element dateEle = new Element("date")
								.setAttribute("raw", date.rawDate)
								.setAttribute("year", date.year)
								.setAttribute("month", date.month);
			for (MediaEntryByDate m : this.takenByDate.get(date)) {
				dateEle.addContent(new Element("media")
								.setAttribute("id", m.mediaId)
								.setAttribute("date_taken_raw", XmlUtils.rawDateFormatter.format(m.date)));
			}
			byDateTaken.addContent(dateEle);
		}

		Element yearsEle = new Element("years");
		for (String year : yearMonths.keySet()) {
			Element yearEle = new Element("year").setAttribute("value", year);
			for (DateGroup date : yearMonths.get(year)) {
				yearEle.addContent(new Element("month")
					.setAttribute("raw", date.rawDate)
					.setAttribute("year", date.year)
					.setAttribute("month", date.month));
			}
			yearsEle.addContent(yearEle);
		}
		byDateTaken.addContent(yearsEle);
		parent.addContent(byDateTaken);

		return parent;
	}

	public Collection<String> writeIndex() throws IOException, TransformerException {
		Collection<String> outputFiles = new ArrayList<String>();

		File xmlFile = new File(this.configuration.photosBaseDirectory, INDEX_XML_FILENAME);
		XmlUtils.outputXmlFile(xmlFile, generateStatsXml());
		outputFiles.add(INDEX_XML_FILENAME);

		XmlUtils.performXsltTransformation(this.configuration, "date_taken_index.xsl", xmlFile,
				new File(this.configuration.photosBaseDirectory, INDEX_HTML_FILENAME));
		outputFiles.add(INDEX_HTML_FILENAME);

		for (DateGroup date : this.takenByDate.keySet()) {
			String baseName = String.format("photos-taken-on-%s.html", date.rawDate);
			XmlUtils.performXsltTransformation(this.configuration, "date_taken_detail.xsl", xmlFile,
					new File(this.configuration.photosBaseDirectory, baseName),
					new XsltParameter("date", date.rawDate));
			outputFiles.add(baseName);
		}

		return outputFiles;
	}

	protected class MainPhotoIndexEntry {
		protected Element xmlSnippet;
		protected Set<String> sets = new HashSet<String>();
	}

	protected static class MediaEntryByDate implements Comparable<MediaEntryByDate> {
		String mediaId;
		Date date;

		public MediaEntryByDate(String mediaId, Date date) {
			this.mediaId = mediaId;
			this.date = date;
		}

		public int compareTo(MediaEntryByDate m) {
			return this.date.compareTo(m.date);
		}

		@Override
		public int hashCode() {
			return this.mediaId.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this.mediaId.equals(obj);
		}
	}

	protected static class DateGroup implements Comparable<DateGroup> {
		private static DateFormat yearFormatter = new SimpleDateFormat("yyyy");
		private static DateFormat monthFormatter = new SimpleDateFormat("MMMM");
		private static DateFormat rawDateFormatter = new SimpleDateFormat("yyyy-MM");

		String year;
		String month;
		String rawDate;

		public DateGroup(Date input) {
			this.year = yearFormatter.format(input);
			this.month = monthFormatter.format(input);
			this.rawDate = rawDateFormatter.format(input);
		}

		public int compareTo(DateGroup o) {
			return this.rawDate.compareTo(o.rawDate);
		}

		@Override
		public boolean equals(Object obj) {
			return this.rawDate.equals(obj);
		}

		@Override
		public int hashCode() {
			return this.rawDate.hashCode();
		}
	}
}
