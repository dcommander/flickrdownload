/*
  FlickrDownload - Copyright(C) 2011 Brian Masney <masneyb@onstation.org>.
                 - Copyright(C) 2015 D. R. Commander.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
*/

package org.gftp.FlickrDownload;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;

public class PhotosNotInASet extends AbstractSet {
	private Set<Photo> photoSet = new LinkedHashSet<Photo>();
	private String primaryPhotoId;
	private String primaryPhotoFilename;
	private String primaryPhotoSmallSquareUrl;

	public PhotosNotInASet(Configuration configuration, Flickr flickr) throws IOException, SAXException, FlickrException {
		super(configuration);

        if(configuration.limitDownloadsToSets.size() > 0) {
            return;
        }

		Logger.getLogger(getClass()).info("Downloading list of photos that are not in a set");

		int pageNum = 1;
		while (true) {
			PhotoList photos = flickr.getPhotosInterface().getNotInSet(500, pageNum);
			if (photos.size() == 0)
				break;

			for (int i = 0; i < photos.size(); i++) {
				Photo photo = (Photo) photos.get(i);
				if (this.primaryPhotoId == null) {
					this.primaryPhotoId = photo.getId();
					if (configuration.useTitles)
						this.primaryPhotoFilename =
							FlickrDownload.sanitizeTitle(photo.getTitle());
					else
						this.primaryPhotoFilename = photo.getId();
					this.primaryPhotoSmallSquareUrl = photo.getSmallSquareUrl();
				}

				this.photoSet.add(photo);
			}
			pageNum++;
		}

		Logger.getLogger(getClass()).info(String.format("There are a total of %s photos that are not in a set", this.photoSet.size()));
	}

	@Override
	protected void download(Flickr flickr, Element setXml) throws IOException, SAXException, FlickrException {
        int retrievedPhotos = 0;
		for (Photo photo : this.photoSet) {
            retrievedPhotos++;
            Logger.getLogger(PhotosNotInASet.class).info("Processing photo " + retrievedPhotos + " of " + this.photoSet.size() + ": " + photo.getUrl());
			processPhoto(photo, flickr, setXml);
		}
	}

	@Override
	protected int getMediaCount() {
		return this.photoSet.size();
	}

	@Override
	protected String getPrimaryPhotoId() {
		return this.primaryPhotoId;
	}

	@Override
	protected String getPrimaryPhotoFilename() {
		return this.primaryPhotoFilename;
	}

	@Override
	protected String getPrimaryPhotoSmallSquareUrl() {
		return this.primaryPhotoSmallSquareUrl;
	}

	@Override
	protected String getSetDescription() {
		return "";
	}

	@Override
	protected String getRealSetId() {
		return "photos-not-in-a-set";
	}

	@Override
	protected String getSetId() {
		return "photos-not-in-a-set";
	}

	@Override
	protected String getSetTitle() {
		return "Photos Not In A Set";
	}

}
