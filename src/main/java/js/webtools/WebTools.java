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

import java.util.*;

import js.file.Files;
import js.json.*;

import org.apache.http.NameValuePair;

public final class WebTools {

  public static final String ERR = "err";
  public static final String HTTP_RESPONSE_STATUS_CODE = "http_code";

  public static final String MSG = "msg";
  public static final String MSG_JSON = "msg_json";

  public static final String STACK_TRACE = "stack trace";

  // Requests parsed as something other than json maps are stored as values within a map
  public static final String LIST = "_list_";
  public static final String TEXT = "_text_";
  // In case client extracts extra path elements, ".../a/b/c", and stores
  // them as parameters with this key
  public static final String PATH_SUFFIX_KEY = "_path_suffix";

  @Deprecated
  public static final String RESOURCE_PATH = "r";

  // Constants originally supplied by javax.servlet.http.HttpServletResponse
  public static final int SC_OK = 200;
  public static final int SC_BAD_REQUEST = 400;
  public static final int SC_INTERNAL_SERVER_ERROR = 500;

  /**
   * Try to infer an appropriate http response status code for an exception
   */
  public static int inferHttpStatusResponseCode(Throwable throwableOrNull) {
    int httpStatusCode = SC_OK;
    if (throwableOrNull != null) {
      Throwable t = throwableOrNull;
      httpStatusCode = SC_INTERNAL_SERVER_ERROR;
      if (t instanceof IllegalArgumentException)
        httpStatusCode = SC_BAD_REQUEST;
    }
    return httpStatusCode;
  }

  /**
   * Convert NameValuePair parameters to JSON map
   */
  public static JSMap getParametersAsJson(List<NameValuePair> parameters) {
    Map<String, List<String>> map = hashMap();
    for (NameValuePair pair : parameters) {
      String key = pair.getName();
      String value = pair.getValue();
      List<String> list = null;
      if (!map.containsKey(key)) {
        list = arrayList();
        map.put(key, list);
      }
      list.add(value);
    }

    JSMap jsonMap = new JSMap();
    for (String key : map.keySet()) {
      List<String> values = map.get(key);
      if (values.size() > 1) {
        JSList list = new JSList();
        for (String v : values)
          list.add(v);
        jsonMap.put(key, list);
      } else {
        jsonMap.put(key, values.get(0));
      }
    }

    return jsonMap;
  }

  /**
   * Determine if request results contain error
   * 
   * @param map
   *          json map containing request results
   * @return true if map has "err" key with value true
   */
  public static boolean hasError(JSMap map) {
    return map.opt(ERR);
  }

  /**
   * Get "msg" value from request results, or empty string if no such value
   */
  public static String getMessage(JSMap map) {
    return map.opt(MSG, "");
  }

  /**
   * If error occurred with request, get "msg"; else empty string
   */
  public static String getErrorMessage(JSMap map) {
    if (hasError(map))
      return getMessage(map);
    return "";
  }

  public static String verifyPathPrefix(String pathPrefix) {
    if (!pathPrefix.startsWith("/"))
      throw new IllegalArgumentException("path prefix doesn't start with slash: '" + pathPrefix + "'");
    return pathPrefix;
  }

  private static final Map<String, String> sContentTypesMap;
  static {
    sContentTypesMap = concurrentHashMap();
    Map<String, String> m = sContentTypesMap;
    m.put("png", "image/png");
    m.put("jpg", "image/jpeg");
    m.put("jpeg", "image/jpeg");
    m.put("html", "text/html");
    m.put("css", "text/css");
    m.put("js", "text/javascript");
    m.put("json", "application/json");
  }

  public static String determineContentType(String filename) {
    String ext = Files.getExtension(filename);
    return nullToEmpty(sContentTypesMap.get(ext));
  }

}
