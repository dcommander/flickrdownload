/*
  FlickrDownload - Copyright(C) 2010 Brian Masney <masneyb@onstation.org>.
                 - Copyright(C) 2015-2017 D. R. Commander.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
*/

package org.gftp.FlickrDownload;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.http.HTTPException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XmlUtils {
	public static String RAW_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static DateFormat rawDateFormatter = new SimpleDateFormat(RAW_DATE_FORMAT);
	private static DateFormat prettyDateFormatter = new SimpleDateFormat("MMMM d, yyyy");

	public static Element createUserXml(Configuration configuration) {
		return new Element("user")
			.addContent(new Element("id").setText(configuration.photosUser.getId()))
			.addContent(new Element("username").setText(configuration.photosUser.getUsername()))
			.addContent(new Element("realname").setText(configuration.photosUser.getRealName()))
			.addContent(new Element("location").setText(configuration.photosUser.getLocation()))
			.addContent(new Element("isProUser").setText(configuration.photosUser.isPro() ? "1" : "0"))
			.addContent(new Element("mediaCount").setText(String.valueOf(configuration.photosUser.getPhotosCount())))
			.addContent(createDateElement("flickrMemberSince", configuration.photosUser.getPhotosFirstDate()))
			.addContent(createDateElement("mediaFirstTakenOn", configuration.photosUser.getPhotosFirstDateTaken()))
			.addContent(createDateElement("photosSyncedOn", new Date()))
			.addContent(createMediaElement("buddyIcon", configuration.buddyIconFilename, configuration.buddyIconFilename.getName(), configuration.photosUser.getSecureBuddyIconUrl()))
			.addContent(new Element("authUser")
				.setAttribute("id", configuration.auth.getUser().getId())
				.setAttribute("username", configuration.auth.getUser().getUsername())
				.setAttribute("permission", configuration.auth.getPermission().toString()))
			.addContent(new Element("flickrUrls")
				.setAttribute("mobile", StringUtils.defaultString(configuration.photosUser.getMobileurl()))
				.setAttribute("photos", StringUtils.defaultString(configuration.photosUser.getPhotosurl()))
				.setAttribute("profile", StringUtils.defaultString(configuration.photosUser.getProfileurl())));
	}

	public static Element createApplicationXml() {
		return new Element("application")
			.addContent(new Element("name").setText(FlickrDownload.getApplicationName()))
			.addContent(new Element("version").setText(FlickrDownload.getApplicationVersion()))
			.addContent(new Element("website").setText(FlickrDownload.getApplicationWebsite()));
	}

	public static void outputXmlFile(File dest, Element root) throws IOException {
		Logger.getLogger(XmlUtils.class).info(String.format("Writing file %s", dest));
		PrintWriter out = new PrintWriter(dest);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(new Document(root), out);
		out.flush();
	}

	public static Element createDateElement(String elementName, Date date) {
		return new Element(elementName)
			.setAttribute("raw", date == null ? "" : rawDateFormatter.format(date))
			.setAttribute("pretty", date == null ? "" : prettyDateFormatter.format(date));
	}

	public static Element createMediaElement(String elementName, File localFilename, String displayLocalFilename, String remoteUrl) {
		Element element = new Element(elementName)
			.setAttribute("publicUrl", StringUtils.defaultString(remoteUrl));

		if (localFilename != null) {
			element.setAttribute("localFilename", displayLocalFilename)
				.setAttribute("size", String.valueOf(localFilename.length()))
				.setAttribute("md5sum", localFilename.exists() ? IOUtils.md5Sum(localFilename) : "");

			if (localFilename.getName().endsWith(".jpg") || localFilename.getName().endsWith(".png")) {
				Image image = new ImageIcon(localFilename.getAbsolutePath()).getImage();
				element.setAttribute("width", String.valueOf(image.getWidth(null)))
					.setAttribute("height", String.valueOf(image.getHeight(null)));
			}
		}

		return element;
	}

	public static Element downloadMediaAndCreateElement(String elementName, File localFilename, String displayLocalFilename, String remoteUrl, boolean forceDownload, boolean checkSize, Configuration configuration) throws IOException {
		if (!configuration.onlyData && remoteUrl != null &&
				(!localFilename.exists() ||
					(checkSize && !IOUtils.sameSize(remoteUrl, localFilename)) ||
						forceDownload))
			IOUtils.downloadUrl(remoteUrl, localFilename);

		return createMediaElement(elementName, localFilename, displayLocalFilename, remoteUrl);
	}

	public static Element downloadVideoAndCreateElement(String elementName, File localFilename, String displayLocalFilename, String urls[], boolean forceDownload, boolean checkSize, Configuration configuration) throws IOException {
		int i;

		for (i = 0; i < urls.length; i++) {
			try {
				if (!configuration.onlyData && urls[i] != null &&
					 (!localFilename.exists() ||
					   (checkSize && !IOUtils.sameSize(urls[i], localFilename)) ||
						   forceDownload))
					IOUtils.downloadUrl(urls[i], localFilename);
			} catch (HTTPException e) {
				if (i == urls.length - 1)
					throw e;
				continue;
			}
			break;
		}

		return createMediaElement(elementName, localFilename, displayLocalFilename, urls[i]);
	}

	public static void performXsltTransformation(final Configuration configuration, String xsltStylesheet, File xmlFile, File outputFile, XsltParameter... parameters) throws IOException, TransformerException {
		Logger.getLogger(XmlUtils.class).info(String.format("Writing file %s", outputFile));

		InputStream xslt = null;
		InputStream xmlInput = null;
		PrintWriter output = null;
		try {
			xslt = XmlUtils.class.getResourceAsStream("xslt/" + xsltStylesheet);
			xmlInput = new FileInputStream(xmlFile);
			output = new PrintWriter(outputFile);
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setURIResolver(new URIResolver() {
				public Source resolve(String href, String base) throws TransformerException {
					return new StreamSource(XmlUtils.class.getResourceAsStream("xslt/" + href));
				}
			});
			Transformer transformer = factory.newTransformer(new StreamSource(xslt));
			transformer.setParameter("PHOTOS_BASE_DIR", configuration.photosBaseDirectory.getAbsolutePath());
			for (XsltParameter param : parameters) {
				transformer.setParameter(param.key, param.value);
			}
			transformer.transform(new StreamSource(xmlFile), new StreamResult(output));
			output.flush();
		}
		catch (TransformerException e) {
			throw new TransformerException(String.format("Error transforming %s to %s using stylesheet %s", xmlFile, outputFile, xsltStylesheet), e);
		}
		finally {
			if (xslt != null)
				xslt.close();
			if (xmlInput != null)
				xmlInput.close();
			if (output != null)
				output.close();
		}
	}

	public static class XsltParameter {
		protected String key;
		protected String value;

		public XsltParameter(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}
