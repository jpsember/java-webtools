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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import js.base.BaseObject;
import js.base.BasePrinter;
import js.data.DataUtil;
import js.file.Files;
import js.json.*;
import js.webtools.WebRequest.Verb;

/**
 * Parameters for SERVICING http requests. The request's input parameters can be
 * read from it, and the outputs can be written to it
 */
//This suppresses the 'access restriction' warning on the com.sun.net webserver classes
@SuppressWarnings("restriction")
public final class WebResponse extends BaseObject {

  // ------------------------------------------------------------------
  // Constructors
  // ------------------------------------------------------------------

  /**
   * Build WebResponse from a JSMap defining its arguments
   */
  public static WebResponse build(Verb verb, JSMap webRequestArgs) {
    return new WebResponse(verb, webRequestArgs);
  }

  /**
   * Build a WebResponse from an HttpExchange
   */
  public static WebResponse build(HttpExchange httpExchange) {
    JSMap jsonMap = map();
    String query = httpExchange.getRequestURI().getQuery();
    if (query != null) {
      for (String param : query.split("&")) {
        String[] entry = param.split("=");
        String key = entry[0];
        String value = "";
        if (entry.length > 1)
          value = entry[1];

        // Parse key/value pairs, and parse the values as json entities if possible.
        // If there are multiple values for a key, store them as a json list
        //
        Object parsedValue = parseValueAsJson(value);
        if (jsonMap.containsKey(key)) {
          Object currentValue = jsonMap.optUnsafe(key);
          JSList valueList = null;
          if (currentValue instanceof JSList)
            valueList = (JSList) currentValue;
          else {
            // Promote the existing scalar value to a JSList
            valueList = list();
            valueList.addUnsafe(currentValue);
            jsonMap.put(key, valueList);
          }
          valueList.addUnsafe(parsedValue);
        } else
          jsonMap.putUnsafe(key, parsedValue);
      }
    }
    WebResponse response = WebResponse.build(Verb.valueOf(httpExchange.getRequestMethod()), jsonMap);
    response.mExchange = httpExchange;
    return response;
  }

  private WebResponse(Verb verb, JSMap webRequestArgs) {
    mVerb = verb;
    mInputArgs = webRequestArgs;
  }

  // ------------------------------------------------------------------
  // Attributes
  // ------------------------------------------------------------------

  public Verb getVerb() {
    return mVerb;
  }
  // ------------------------------------------------------------------
  // Logging
  // ------------------------------------------------------------------

  @Override
  public JSMap toJson() {
    JSMap m = super.toJson();
    m.put("verb", getVerb().toString());
    if (!pathElements().isEmpty()) {
      m.put("path_elements", JSList.withStringRepresentationsOf(pathElements()));
    }
    m.put("input_args", mInputArgs);
    m.put("output_args", mOutputArgs);
    if (mOutputMarkup != null)
      m.put("output markup", mOutputMarkup);
    m.lock();
    return m;
  }

  // ------------------------------------------------------------------
  // URI Path elements
  // ------------------------------------------------------------------

  public boolean hasNextElement() {
    return mPathElementCursor < pathElements().size();
  }

  public String peekNextElement() {
    return pathElements().get(mPathElementCursor);
  }

  public boolean readElementIf(String string) {
    if (hasNextElement() && peekNextElement().equals(string)) {
      nextElement();
      return true;
    }
    return false;
  }

  /**
   * Read remaining elements, return them concatenated with '/' delimiters
   */
  public String remainingElements() {
    int end = pathElements().size();
    String result = String.join("/", pathElements().subList(mPathElementCursor, end));
    mPathElementCursor = end;
    return result;
  }

  public String nextElement() {
    String element = peekNextElement();
    mPathElementCursor++;
    return element;
  }

  /**
   * Determine which path elements, if any, were included within the url (relies
   * on nginx rewriting)
   */
  private List<String> pathElements() {
    if (mPathElements == null) {
      String path = mExchange.getRequestURI().getPath();
      mPathElements = split(chompPrefix(path, "/"), '/');
    }
    return mPathElements;
  }

  // ------------------------------------------------------------------
  // Input and output arguments
  // ------------------------------------------------------------------

  public JSMap inputArgs() {
    return mInputArgs;
  }

  public JSMap outputArgs() {
    return mOutputArgs;
  }

  /**
   * Replace the existing output arguments with a new json map
   */
  public void setOutputArgs(JSMap map) {
    mOutputArgs = map;
  }

  /**
   * Set key MSG to point to a message
   */
  public void setOutputMessage(String message) {
    outputArgs().put(WebTools.MSG, message);
  }

  // ------------------------------------------------------------------
  // Error handling
  // ------------------------------------------------------------------

  /**
   * Store error (if one has not already been stored)
   */
  public void storeError(Integer optionalHttpResponseStatusCode, Throwable t) {
    if (hasError())
      return;

    if (optionalHttpResponseStatusCode == null)
      optionalHttpResponseStatusCode = WebTools.inferHttpStatusResponseCode(t);

    mOutputArgs.put(WebTools.HTTP_RESPONSE_STATUS_CODE, optionalHttpResponseStatusCode);
    mOutputArgs.put(WebTools.ERR, true);

    String message = t.getMessage();
    if (!nullOrEmpty(message)) {
      setOutputMessage(message);

      // If the message looks like a json map, parse it and include it
      if (message.startsWith("{") && message.length() < 5000) {
        try {
          JSMap jsonMap = new JSMap(message);
          outputArgs().put(WebTools.MSG_JSON, jsonMap);
        } catch (Throwable tIgnored) {
        }
      }
    }

    mOutputArgs.put(WebTools.STACK_TRACE, stackTraceToList(t));

    if (!testMode()) {
      pr("setOutputError", INDENT, t, CR, mOutputArgs);
    }
  }

  /**
   * Store an error (if one hasn't already been stored), and throw an
   * IllegalArgumentException
   */
  public IllegalArgumentException fail(Object... messageObjects) {
    String messageText = BasePrinter.toString(messageObjects);
    if (mExchange != null)
      messageText = messageText + "\n (url: " + mExchange.getRequestURI().getPath() + ")";
    IllegalArgumentException t = new IllegalArgumentException(messageText);
    storeError(WebTools.SC_BAD_REQUEST, t);
    throw t;
  }

  private boolean hasError() {
    return WebTools.hasError(mOutputArgs);
  }

  // ------------------------------------------------------------------
  // Request body
  // ------------------------------------------------------------------

  /**
   * Get body of request as an array of bytes
   */
  public byte[] readBytesFromRequestBody() {
    if (mRequestBody == null) {
      setRequestBody(Files.toByteArray(openRequestBodyInputStream(), "readBytesFromRequestBody"));
    }
    return mRequestBody;
  }

  /**
   * For test purposes only: set the request body to a particular byte array
   * (normally the bytes would be read from the request body via
   * readBytesFromRequestBody())
   */
  public void setRequestBody(byte[] byteArray) {
    mRequestBody = byteArray;
  }

  /**
   * Subclasses should override this to read bytes from the http request body
   */
  protected InputStream openRequestBodyInputStream() {
    throw new UnsupportedOperationException();
  }

  // ------------------------------------------------------------------
  // Generating response
  // ------------------------------------------------------------------

  /**
   * Set response to HTML content
   */
  public WebResponse setOutputHTML(String markup) {
    setOutput("text/html", DataUtil.toByteArray(markup));
    mOutputMarkup = markup;
    return this;
  }

  /**
   * Set response to a particular content type and byte array
   */
  public WebResponse setOutput(String contentType, byte[] bytes) {
    checkState(mResponseBytes == null, "response already set");
    checkNotNull(contentType, "missing content type");
    mContentType = contentType;
    mResponseBytes = bytes;
    return this;
  }

  /**
   * Public for test purposes
   * 
   * If HTML content has been stored as the response, return it; else, null
   */
  public String outputMarkup() {
    return mOutputMarkup;
  }

  /**
   * Write the response to an appropriate OutputStream for the HttpExchange
   */
  public WebResponse writeResponse() {

    if (hasError()) {
      String errorMessage = outputArgs().prettyPrint();
      setOutputHTML("<pre>\n" + errorMessage + "</pre>");
    }

    // If we don't yet have mResponseBytes, try to construct it
    if (mResponseBytes == null) {
      if (mResponseMap != null)
        mResponseBytes = DataUtil.toByteArray(mResponseMap.prettyPrint());
    }
    checkState(mResponseBytes != null, "no responseBytes defined");

    try {
      mExchange.getResponseHeaders().set("Content-Type", mContentType);
      int responseCode = mOutputArgs.opt(WebTools.HTTP_RESPONSE_STATUS_CODE, WebTools.SC_OK);
      mExchange.sendResponseHeaders(responseCode, mResponseBytes.length);
      OutputStream os = mExchange.getResponseBody();
      os.write(mResponseBytes);
      os.close();
    } catch (IOException e) {
      throw asRuntimeException(e);
    }
    return this;
  }

  /**
   * Parse a string, if possible, as a json value. If that fails, return the
   * original string.
   * 
   * Special case: if the string is equal to 'null', return the empty string.
   */
  private static Object parseValueAsJson(String value) {
    if (value.isEmpty() || value.equals("null"))
      return "";
    try {
      return JSUtils.parse(value);
    } catch (IllegalArgumentException e) {
      return value;
    }
  }

  private String mContentType;
  private String mOutputMarkup;
  private JSMap mInputArgs;
  private JSMap mOutputArgs = new JSMap();
  private Verb mVerb;
  private List<String> mPathElements;
  private int mPathElementCursor;
  private byte[] mRequestBody;
  private HttpExchange mExchange;
  private byte[] mResponseBytes;
  private JSMap mResponseMap;

}
