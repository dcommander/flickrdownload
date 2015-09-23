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

import org.jdom.Element;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.collections.Collection;
import com.flickr4java.flickr.collections.CollectionsInterface;
import com.flickr4java.flickr.photosets.Photoset;

public class Collections {
	public static String COLLECTIONS_ICON_DIRECTORY = "collections";
	private Configuration configuration;
	private Flickr flickr;

	public Collections(Configuration configuration, Flickr flickr) {
		this.configuration = configuration;
		this.flickr = flickr;
	}
	
	public Element createTopLevelXml() throws FlickrException, SAXException, IOException {
		CollectionsInterface collectionsInterface = this.flickr.getCollectionsInterface();

		File iconsDir = new File(this.configuration.photosBaseDirectory, COLLECTIONS_ICON_DIRECTORY);
		iconsDir.mkdir();

		Element allCollections = new Element("collections");
    	for (Collection collection : collectionsInterface.getTree(null, null)) {
    		Element setsEle = new Element("sets");
    		for (Photoset set : collection.getPhotosets()) {
    			setsEle.addContent(new Element("set")
    				.setAttribute("id", set.getId())
    				.setAttribute("title", set.getTitle()));
    		}

            String iconLarge = collection.getIconLarge();
            String iconSmall = collection.getIconSmall();

            if(!iconLarge.matches("^https://.*")) iconLarge = "https://www.flickr.com" + iconLarge;
            if(!iconSmall.matches("^https://.*")) iconSmall = "https://www.flickr.com" + iconSmall;

    		File largeFile = new File(iconsDir, collection.getId() + "-large." + (iconLarge.matches(".*jpg$") ? "jpg" : "gif"));
    		File smallFile = new File(iconsDir, collection.getId() + "-small." + (iconSmall.matches(".*jpg$") ? "jpg" : "gif"));

    		allCollections.addContent(new Element("collection")
    			.addContent(new Element("id").setText(collection.getId()))
    			.addContent(new Element("title").setText(collection.getTitle()))
    			.addContent(new Element("description").setText(collection.getDescription()))
    			.addContent(XmlUtils.downloadMediaAndCreateElement("iconLarge", largeFile, iconsDir.getName() + File.separator + largeFile.getName(), iconLarge, this.configuration.downloadCollectionIcons, false, configuration))
    			.addContent(XmlUtils.downloadMediaAndCreateElement("iconSmall", smallFile, iconsDir.getName() + File.separator + smallFile.getName(), iconSmall, this.configuration.downloadCollectionIcons, false, configuration))
    			.addContent(setsEle));
    	}

    	return allCollections;
	}
}
