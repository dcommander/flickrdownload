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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;

import org.jdom.Element;

public class NoopMediaIndexer implements MediaIndexer {
	public static NoopMediaIndexer INSTANCE = new NoopMediaIndexer();

	public void addToIndex(String setId, Element mediaElement) {
		// NOOP
	}

	public Collection<String> writeIndex() throws IOException {
		Collection<String> empty = new ArrayList<String>();
		return empty;
	}

}
