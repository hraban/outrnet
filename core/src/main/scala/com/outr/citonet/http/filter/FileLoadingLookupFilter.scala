package com.outr.citonet.http.filter

import com.outr.citonet.URL
import com.outr.citonet.http.content.{FileContent, HttpContent}
import java.io.File

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class FileLoadingLookupFilter(urlBasePath: String,
                                   lookupPath: String,
                                   allowCaching: Boolean,
                                   priority: Double = HttpFilter.Low) extends LookupFilter {
  def lookup(url: URL): Option[HttpContent] = if (url.path.startsWith(urlBasePath)) {
    val path = url.path.substring(urlBasePath.length)
    val file = new File(s"$lookupPath$path")
    if (file.isFile) {
      Some(FileContent(file, allowCaching = allowCaching))
    } else {
      None
    }
  } else {
    None
  }
}
