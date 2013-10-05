package com.outr.citonet.http

import com.outr.citonet.http.filter._
import com.outr.citonet.http.request.HttpRequest
import com.outr.citonet.http.content.HttpContent
import com.outr.citonet.http.response.HttpResponse
import com.outr.citonet.http.filter.ClassLoadingLookupFilter
import com.outr.citonet.http.content.StringContent

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class WebApplication extends HttpApplication with FilteredApplication with NotFoundApplication {
  def addContent(uri: String, creator: => HttpContent): HttpHandler = {
    addContent(uri, request => creator)
  }

  def addContent(uri: String, creator: HttpRequest => HttpContent): HttpHandler = {
    addHandler(uri, HttpHandler(request => HttpResponse(creator(request))))
  }

  def addHandler(uri: String, handler: HttpHandler): HttpHandler = {
    PathMappingFilter.add(this, uri, handler)
    handler
  }

  def removeContent(uri: String) = {
    PathMappingFilter.remove(this, uri)
  }

  def addClassPath(urlBasePath: String,
                   lookupPath: String,
                   allowCaching: Boolean = true,
                   priority: Double = HttpFilter.Low) = {
    addFilter(ClassLoadingLookupFilter(urlBasePath, lookupPath, allowCaching = allowCaching, priority = priority))
  }

  def addFilePath(urlBasePath: String,
                   lookupPath: String,
                   allowCaching: Boolean = true,
                   priority: Double = HttpFilter.Low) = {
    addFilter(FileLoadingLookupFilter(urlBasePath, lookupPath, allowCaching = allowCaching, priority = priority))
  }

  protected def notFoundContent(request: HttpRequest) = {
    StringContent(s"404 Page not found: ${request.url}")
  }
}
