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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photosets.Photoset;

public class Sets {
	public static String SET_THUMBNAIL_FILENAME = "index.html";
	public static String SET_DETAIL_FILENAME = "detail.html";

	private List<AbstractSet> sets = null;
	private Configuration configuration;
	private Flickr flickr;

	public Sets(Configuration configuration, Flickr flickr) throws Exception {
		this.configuration = configuration;
		this.flickr = flickr;
		this.sets = getSets();
	}

	public List<AbstractSet> getSets() throws FlickrException, IOException, SAXException {
		if (this.sets != null)
			return this.sets;

		Logger.getLogger(Sets.class).info("Downloading photo set information");

		List<AbstractSet> setMap = new LinkedList<AbstractSet>();
        Iterator<Photoset> fsets = this.flickr.getPhotosetsInterface().getList(this.configuration.photosUser.getId()).getPhotosets().iterator();
        while (fsets.hasNext()) {
            Photoset fset = fsets.next();
        	AbstractSet s = new Set(this.configuration, fset);
        	setMap.add(s);
        }
        
        PhotosNotInASet photosNotInSet = new PhotosNotInASet(this.configuration, this.flickr);
        if (photosNotInSet.getMediaCount() > 0)
        	setMap.add(photosNotInSet);

        return setMap;
	}
	
	public Element createTopLevelXml() throws JDOMException, IOException {
		Element allSets = new Element("sets");
    	for (AbstractSet set : this.sets) {
			if (configuration.limitDownloadsToSets.size() > 0 && !configuration.limitDownloadsToSets.contains(set.getSetId()))
				continue;

    		allSets.addContent(set.createToplevelXml());
    	}
    	return allSets;
	}
	
	public void performXsltTransformation() throws IOException, TransformerException {	
    	for (AbstractSet set : this.sets) {
    		File setXmlFile = set.getSetXmlFilename();
    		if (!setXmlFile.exists())
    			continue;

    		File setDir = set.getSetDirectory();
    		XmlUtils.performXsltTransformation(this.configuration, "set.xsl",
    				setXmlFile,
    				new File(setDir, SET_THUMBNAIL_FILENAME));

    		XmlUtils.performXsltTransformation(this.configuration, "set_detail.xsl",
    				setXmlFile,
    				new File(setDir, SET_DETAIL_FILENAME));
    	}		
	}

	public void downloadAllPhotos() throws Exception {
		for (AbstractSet set : this.sets) {
			if (configuration.limitDownloadsToSets.size() > 0 && !configuration.limitDownloadsToSets.contains(set.getSetId()) &&
			    !configuration.limitDownloadsToSets.contains(set.getSetTitle()))
				continue;

			File setDir = set.getSetDirectory();
			setDir.mkdir();

			File setXmlFilename = set.getSetXmlFilename();
			if (Sets.this.configuration.partialDownloads && setXmlFilename.exists() && Stats.getMediaCount(setXmlFilename, set.getSetId()) == set.getMediaCount()) {
				Logger.getLogger(getClass()).info(String.format("Skipping the download of set %s - %s", set.getRealSetId(), set.getSetTitle()));
				continue;
			}

			XmlUtils.outputXmlFile(setXmlFilename, set.createSetlevelXml(this.flickr));
		}
	}
}
