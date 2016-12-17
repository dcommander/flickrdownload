<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml">
<xsl:include href="settings.xsl"/>
<xsl:include href="common.xsl"/>
<xsl:include href="set_common.xsl"/>
<xsl:output name="photo-format" method="xml" omit-xml-declaration="yes" encoding="UTF-8" indent="no"/>

<xsl:param name="PHOTOS_BASE_DIR"/>

<!--
  FlickrDownload - Copyright(C) 2007,2010 Brian Masney <masneyb@onstation.org>.
                 - Copyright(C) 2016 D. R. Commander.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
-->

<xsl:template match="/set">
  <html>
    <head>
      <link href="../photos.css" rel="stylesheet" type="text/css"/>
      <title><xsl:value-of select="title"/></title>
    </head>
    <body>
      <xsl:apply-templates select="user" mode="header">
        <xsl:with-param name="title"><xsl:value-of select="title"/></xsl:with-param>
        <xsl:with-param name="baseDir">..</xsl:with-param>
      </xsl:apply-templates>

      <center>
        <table>
          <tr>
            <td class="set_thumbnails_primary" width="45%" valign="top">
              <xsl:variable name="filename" select="concat(media[primary='1']/id,'.html')"/>
              <a title="{media[primary='1']/title}" href="{$filename}">
                <img class="thumbnail_photo" src="{media[primary='1']/image[@type='Medium']/@localFilename}" width="100%"/>
              </a>
    
              <xsl:if test="description != ''">
                <div class="set_description"><xsl:value-of select="description" disable-output-escaping="yes"/></div>
              </xsl:if>
            </td>
            <td class="set_thumbnails_other" valign="top">
              <xsl:call-template name="generate_all_rows_images">
                <xsl:with-param name="num_items"><xsl:value-of select="count(media)"/></xsl:with-param>
                <xsl:with-param name="items_per_row">6</xsl:with-param>
                <xsl:with-param name="item_width">75</xsl:with-param>
              </xsl:call-template>
            </td>
          </tr>
        </table>
      </center>

      <xsl:apply-templates select="user" mode="footer"/>
    </body>
  </html>
</xsl:template>

<!-- ********************************************************************** -->


<xsl:template name="display_item">
  <xsl:param name="pos"/>

  <xsl:variable name="baseFilename" select="concat(media[$pos + 0]/id,'.html')" />
  <xsl:variable name="filename" select="concat($PHOTOS_BASE_DIR,'/',id,'/',$baseFilename)" />

  <xsl:result-document href="{$filename}" format="photo-format">
    <html>
      <head>
        <link href="../photos.css" rel="stylesheet" type="text/css"/>
        <title><xsl:value-of select="media[$pos + 0]/title"/></title>
      </head>
      <body>
        <xsl:apply-templates select="/set/user" mode="header">
          <xsl:with-param name="title">
            <xsl:text>Photos from </xsl:text>
            <xsl:value-of select="/set/user/realname"/>
          </xsl:with-param>
          <xsl:with-param name="baseDir">..</xsl:with-param>
        </xsl:apply-templates>

        <xsl:if test="media[$pos + 0]/notes and count(media[$pos + 0]/notes/*) &gt; 0">
          <map id="notes" name="notes">
            <xsl:for-each select="media[$pos + 0]/notes/note">
              <area coords="{@x},{@y},{@x+@width},{@y+@height}" href="{image[@type='Original']/@localFilename}" title="[{@author}] {@text}" alt="[{@author}] {@text}"/>
            </xsl:for-each>
            <area coords="0,0,1024,768" href="{image[@type='Original']/@localFilename}"/>
          </map>
        </xsl:if>

        <table class="photo_table">
          <td class="photo_side" valign="top">
            <a href="{image[@type='Original']/@localFilename}">
              <xsl:choose>
                <xsl:when test="media[$pos + 0]/notes and count(media[$pos + 0]/notes/*) &gt; 0">
                  <img class="medium_photo" usemap="#notes" src="{media[$pos + 0]/image[@type='Medium']/@localFilename}"/>
                </xsl:when>
                <xsl:otherwise>
                  <img class="medium_photo" src="{media[$pos + 0]/image[@type='Medium']/@localFilename}"/>
                </xsl:otherwise>
              </xsl:choose>
            </a>
            <div class="photo_title"><xsl:value-of select="media[$pos + 0]/title"/></div>
            <div class="photo_description"><xsl:value-of select="media[$pos + 0]/description"/></div>
          </td>
          <td class="info_side" valign="top">
            <table class="photo_nav">
              <tr>
                <td colspan="2" class="photo_nav_header">Photo Navigation</td>
              </tr>
              <tr>
              <td align="center" class="photo">
                <xsl:if test="$pos > 1">
                  <a title="{media[$pos - 1]/title}">
                    <xsl:attribute name="href"><xsl:value-of select="concat(media[$pos - 1]/id,'.html')"/></xsl:attribute>
                    <img class="thumb_photo" src="{media[$pos - 1]/image[@type='Small Square']/@localFilename}"/>
                  </a>
                </xsl:if>
              </td>
              <td align="center" class="photo">
                <xsl:if test="media[$pos + 1]">
                  <a title="{media[$pos + 1]/title}">
                    <xsl:attribute name="href"><xsl:value-of select="concat(media[$pos + 1]/id,'.html')"/></xsl:attribute>
                    <img class="thumb_photo" src="{media[$pos + 1]/image[@type='Small Square']/@localFilename}"/>
                  </a>
                </xsl:if>
              </td>
              </tr>
              <tr>
                <td colspan="2" class="view_on_flickr"><a href="index.html">Back to Index</a></td>
              </tr>
            </table>

            <div class="privacy">
              <xsl:text>Privacy: </xsl:text>
              <xsl:choose>
                <xsl:when test="media[$pos + 1]/privacy/@public='1'">
                  <xsl:text>Public</xsl:text>
                </xsl:when>
                <xsl:when test="media[$pos + 1]/privacy/@friends='1' and media[$pos + 1]/privacy/@family='1'">
                  <xsl:text>Friends/Family</xsl:text>
                </xsl:when>
                <xsl:when test="media[$pos + 1]/privacy/@friends='1'">
                  <xsl:text>Friends</xsl:text>
                </xsl:when>
                <xsl:when test="media[$pos + 1]/privacy/@family='1'">
                  <xsl:text>Family</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>Private</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </div>

            <div>License: <xsl:value-of select="media[$pos + 0]/license"/></div>

            <div class="date_taken">Date Taken: <xsl:value-of select="media[$pos + 0]/dates/taken/@raw"/></div>

            <xsl:if test="media[$pos + 0]/geodata and media[$pos + 0]/geodata/@latitude != '' and media[$pos + 0]/geodata/@longitude != ''">
              <div class="location">
                Coordinates:
                <xsl:value-of select="media[$pos + 0]/geodata/@latitude"/>, <xsl:value-of select="media[$pos + 0]/geodata/@longitude"/>
                <xsl:if test="media[$pos + 0]/geodata/@accuracy">
                  (Accuracy = <xsl:value-of select="media[$pos + 0]/geodata/@accuracy"/>)
                </xsl:if>
              </div>
              <div class="location">
                Map links:
                [<a href="http://maps.google.com/maps?ll={media[$pos + 0]/geodata/@latitude},{media[$pos + 0]/geodata/@longitude}&amp;t=h&amp;z={media[$pos + 0]/geodata/@accuracy}" target="_blank">Google</a>]
              </div>
            </xsl:if>

            <xsl:if test="media[$pos + 0]/notes and count(media[$pos + 0]/notes/*) &gt; 0">
              <div class="notes">This photo has notes. Move your mouse over the photo to see them.</div>
            </xsl:if>

            <div class="download_links">
              <div class="download_links_header">Download</div>
              <xsl:for-each select="media[$pos + 0]/image[@localFilename != '']">
                <div class="download_link">
                  <a href="{@localFilename}" target="_blank"><xsl:value-of select="@type"/></a>
                  <xsl:text> - </xsl:text>
                  <xsl:call-template name="pretty_file_size">
                    <xsl:with-param name="size" select="@size"/>
                  </xsl:call-template>

                  <xsl:if test="@width != '' and @height != ''">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="@width"/>
                    <xsl:text>x</xsl:text>
                    <xsl:value-of select="@height"/>
                  </xsl:if>
                </div>
              </xsl:for-each>
              <div class="download_link">
                <a href="{media[$pos + 0]/publicUrl}" target="_blank">View on Flickr</a>
              </div>
            </div>

            <xsl:if test="media[$pos + 0]/tags/tag">
              <div class="tags">
                <div class="tags_header">Tags</div>
  
                <xsl:for-each select="media[$pos + 0]/tags/tag">
                  <xsl:sort select="@value"/>

                  <div class="tag">
                    <xsl:value-of select="@raw"/>
                  </div>
                </xsl:for-each>
              </div>
            </xsl:if>

            <xsl:if test="media[$pos + 0]/exif/exif">
              <div class="exif_tags">
                <div class="exif_tags_header">EXIF Tags</div>
  
                <xsl:for-each select="media[$pos + 0]/exif/exif">
                  <xsl:sort select="@label"/>
  
                  <div class="exif_tag">
                    <xsl:value-of select="@label"/>
                    <xsl:text>: </xsl:text>
  
                    <xsl:choose>
                      <xsl:when test="@clean != ''">
                        <xsl:value-of select="@clean"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="@raw"/>
                      </xsl:otherwise>
                    </xsl:choose>
                  </div>
                </xsl:for-each>
              </div>
            </xsl:if>
          </td>
        </table>

        <xsl:apply-templates select="/set/user" mode="footer"/>
      </body>
    </html>
  </xsl:result-document>

  <td align="center" class="photo">
    <a title="{media[$pos + 0]/title}" href="{$baseFilename}">
      <xsl:if test="media[$pos + 0]/@type='video'">
        <xsl:attribute name="class">video_thumbnail</xsl:attribute>
      </xsl:if>

      <img class="thumbnail_photo" width="{$thumbnail_width}" height="{$thumbnail_height}" src="{media[$pos + 0]/image[@type='Small Square']/@localFilename}"/>

      <xsl:if test="media[$pos + 0]/@type='video'">
        <span class="play_icon"><img src="../play_icon.png" width="{$thumbnail_width}" height="{$thumbnail_height}" alt="Play"/></span>
      </xsl:if>
    </a>
  </td>
</xsl:template>

<!-- ********************************************************************** -->

<xsl:template name="show_extra_breadcrumbs">
  <span class="header_breadcrumb"><a href="index.html">Set Thumbnails</a></span>
  <span class="header_breadcrumb"><a href="detail.html">Set Details</a></span>
</xsl:template>

</xsl:stylesheet>
