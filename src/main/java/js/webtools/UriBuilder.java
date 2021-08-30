/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package js.webtools;

import static js.base.Tools.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;

import org.apache.http.client.utils.URIBuilder;

import js.parsing.RegExp;

/**
 * A subclass of Apache's URIBuilder that makes exceptions unchecked
 */
public final class UriBuilder extends URIBuilder {

  public UriBuilder() {
  }

  public UriBuilder(String uri) throws URISyntaxException {
    super(uri);
  }

  /**
   * Factory constructor; provided since there seems to be no way to have a
   * 'normal' constructor that doesn't throw a checked exception
   */
  public static UriBuilder build(String uri) {
    try {
      return new UriBuilder(uri);
    } catch (URISyntaxException e) {
      throw asIllegalArgumentException(e);
    }
  }

  @Override
  public URI build() {
    try {
      return super.build();
    } catch (URISyntaxException e) {
      throw asIllegalArgumentException(e);
    }
  }

  @Override
  public UriBuilder setPort(int port) {
    super.setPort(port);
    return this;
  }

  @Override
  public UriBuilder setHost(String host) {
    super.setHost(host);
    return this;
  }

  @Override
  public UriBuilder setPath(String path) {
    super.setPath(WebTools.verifyPathPrefix(path));
    return this;
  }

  @Override
  public UriBuilder setScheme(String scheme) {
    super.setScheme(scheme);
    return this;
  }

  public UriBuilder setHTTP() {
    return setScheme("http");
  }

  public UriBuilder setHTTPS() {
    return setScheme("https");
  }

  /**
   * Given a string, attempts to clean it up so it's a valid URL.
   * 
   * If the string is empty, it returns an empty string.
   * 
   * At present, it can return false negatives: urls that are not in fact legal;
   * see the unit tests.
   * 
   * @throws IllegalArgumentException
   *           if it was not a valid URL
   * @return the possibly 'cleaned up' valid url
   */
  public static String cleanUpURL(String originalUrl) {
    String url = originalUrl.trim();
    if (url.isEmpty())
      return url;
    Matcher matcher = RegExp.pattern("^([hH][tT][tT][pP][sS]?:\\/?\\/?)").matcher(url);
    if (matcher.find()) {
      String prefix = matcher.group(1).toLowerCase();
      String modifiedPrefix = "http://";
      if (prefix.startsWith("https"))
        modifiedPrefix = "https://";
      url = modifiedPrefix + url.substring(prefix.length());
    } else {
      url = "http://" + url;
    }

    // Try building a URI from this to throw an IllegalArgumentException if it's invalid
    URI.create(url);
    return url;
  }

}
