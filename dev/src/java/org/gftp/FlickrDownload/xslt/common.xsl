<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<!--
  FlickrDownload - Copyright(C) 2010 Brian Masney <masneyb@onstation.org>.
  If you have any questions, comments, or suggestions about this program, please
  feel free to email them to me. You can always find out the latest news about
  FlickrDownload from my website at http://www.onstation.org/flickrdownload/

  FlickrDownload comes with ABSOLUTELY NO WARRANTY; for details, see the COPYING
  file. This is free software, and you are welcome to redistribute it under
  certain conditions; for details, see the COPYING file.
-->

<xsl:template match="user" mode="header">
  <xsl:param name="title"/>
  <xsl:param name="baseDir"/>

  <div class="header">
    <img class="buddy_icon" width="{buddyIcon/@width}" height="{buddyIcon/@height}" src="{$baseDir}/{buddyIcon/@localFilename}"/>
    <span class="header_links">
      <span class="header_title"><xsl:value-of select="$title"/></span>
      <span class="header_breadcrumbs">
        <span class="header_breadcrumb"><a href="{$baseDir}/collections.html">Collections</a></span>
        <span class="header_breadcrumb"><a href="{$baseDir}/sets.html">Sets</a></span>
        <xsl:call-template name="show_extra_breadcrumbs"/>
        <span class="header_breadcrumb"><a href="{$baseDir}/stats.html">Stats</a></span>
        <span class="header_breadcrumb"><a href="{$baseDir}/archive.html">Archive</a></span>
      </span>
    </span>
  </div>
</xsl:template>

<xsl:template match="user" mode="footer">
</xsl:template>

<xsl:template name="pretty_file_size">
  <xsl:param name="size"/>

  <xsl:choose>
    <xsl:when test="$size > 1024*1024*1024">
      <xsl:value-of select="format-number($size div (1024 * 1024 * 1024), '###,###.#')"/>
      <xsl:text> GB</xsl:text>
    </xsl:when>
    <xsl:when test="$size > 1024*1024">
      <xsl:value-of select="format-number($size div (1024 * 1024), '###,###.#')"/>
      <xsl:text> MB</xsl:text>
    </xsl:when>
    <xsl:when test="$size > 1024">
      <xsl:value-of select="format-number($size div 1024, '###,###.#')"/>
      <xsl:text> KB</xsl:text>
    </xsl:when>
    <xsl:when test="$size > 0">
      <xsl:value-of select="format-number($size, '###,###.#')"/>
      <xsl:text> bytes</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <!-- NOOP -->
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>

