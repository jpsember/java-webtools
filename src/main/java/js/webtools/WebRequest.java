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

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import js.base.BaseObject;
import js.base.DateTimeTools;
import js.json.JSList;
import js.json.JSMap;

/**
 * Abstraction for MAKING http requests
 * 
 * A WebRequest is in one of two fundamental states:
 * 
 * 1) unsent. Parameters can be modified, as can its host, port, path, verb...
 * 
 * 2) sent. Parameters are frozen, results are available.
 * 
 * Even if a WebRequest is in the 'sent' state, it may not have actually been
 * sent yet; but client code can probably assume that it has.
 */
public final class WebRequest extends BaseObject {

  public enum Verb {
    GET, POST, DELETE, PUT;

    public static Verb withName(String name, boolean mustExist) {
      if (mustExist)
        return Verb.valueOf(name);
      for (Verb verb : Verb.values()) {
        if (name.equals(verb.name()))
          return verb;
      }
      return null;
    }
  }

  public enum Scheme {
    HTTP, HTTPS;

    @Override
    public String toString() {
      return mString;
    }

    private Scheme() {
      mString = this.name().toLowerCase();
    }

    private final String mString;
  }

  /**
   * If POST, and payload is json data, store the json map under this key. This
   * is kind of clunky, and could use some refactoring... of course, the whole
   * HTTP GET/POST arguments vs payload stuff is overly confusing, and we didn't
   * design that... ...blame Al Gore
   */
  public static final String KEY_CONTENT_TYPE_JSON = "__json__";

  /**
   * Get the json map that contains the request parameters, to add (or edit)
   * them
   */
  public JSMap editArgs() {
    assertUnsent();
    return readArgs();
  }

  public WebRequest setJsonRequest() {
    // TODO: refactor this; determine if it's already done elsewhere
    mJsonRequestType = true;
    return this;
  }

  public boolean isJsonRequestType() {
    return mJsonRequestType;
  }

  private boolean mJsonRequestType;

  public WebRequest setVerb(Verb verb) {
    assertUnsent();
    mVerb = verb;
    return this;
  }

  public WebRequest setPost() {
    setVerb(Verb.POST);
    return this;
  }

  public WebRequest setPostFile(File file) {
    //testOnlyAssert();
    mPostFile = file;
    return setPost();
  }

  public WebRequest setPut() {
    setVerb(Verb.PUT);
    return this;
  }

  public WebRequest setDelete() {
    setVerb(Verb.DELETE);
    return this;
  }

  public WebRequest setTimeout(int timeoutMs) {
    assertUnsent();
    checkArgument(timeoutMs > 0);
    mTimeout = timeoutMs;
    return this;
  }

  public WebRequest addHeader(String name, String value) {
    assertUnsent();
    mHeaders.add(name);
    mHeaders.add(value);
    return this;
  }

  public List<String> getHeaders() {
    return mHeaders;
  }

  public URI uri() {
    assertSent();
    return mURI;
  }

  /**
   * Get the json map that contains the request parameters, for read-only
   * purposes
   */
  public JSMap readArgs() {
    return mParameterMap;
  }

  public WebRequest setUploadFile(File file) {
    setPost();
    mUploadFile = file;
    return this;
  }

  public File getUploadFile() {
    return mUploadFile;
  }

  public List<NameValuePair> readParameters() {
    if (mParameterList == null) {
      assertSent();
      List<org.apache.http.NameValuePair> p = arrayList();
      mParameterList = p;
      for (String key : mParameterMap.keySet())
        p.add(new BasicNameValuePair(key, mParameterMap.getUnsafe(key).toString()));
    }
    return mParameterList;
  }

  public Verb getVerb() {
    return mVerb;
  }

  public int getTimeout() {
    return mTimeout;
  }

  public File getPostFile() {
    return mPostFile;
  }

  /**
   * Perform request (if not already), return as json map.
   * 
   * The method can handle other request output ('out') types as follows.
   * 
   * If out is successfully parsed as a json map, it returns that map.
   * 
   * If out is successfully parsed as a json list, the list is accessible by
   * calling resultList().
   * 
   * Otherwise, the out string is accessible by calling resultText().
   */
  public JSMap resultMap() {
    makeRequest();
    return mResultMap;
  }

  /**
   * Perform request (if not already), return as json list
   */
  public JSList resultList() {
    return resultMap().getList(WebTools.LIST);
  }

  /**
   * Perform request (if not already), return as string
   */
  public String resultString() {
    makeRequest();
    return mFetchedString;
  }

  /**
   * Perform request (if not already done), and store result in mFetchedString
   */
  private void makeRequest() {
    if (mFetchedString != null)
      return;

    setSent();

    WebRequestExecutor ex = getEffectiveExecutor();
    try {
      mFetchedString = ex.makeRequest(this);

      // Attempt to parse the result string as a json map directly.
      // If that fails, then construct a json map with an entry
      // for either a json list (if result string successfully parsed as one),
      // or a string (the result string).

      try {
        mResultMap = new JSMap(mFetchedString);
      } catch (Throwable t) {
        try {
          JSList list = new JSList(mFetchedString);
          getOrBuildResultMap().put(WebTools.LIST, list);
        } catch (Throwable t2) {
          getOrBuildResultMap().put(WebTools.TEXT, mFetchedString);
        }
      }
    } catch (Throwable t) {
      mFetchedString = t.getMessage();
      setThrowable(t);
    }
  }

  private WebRequestExecutor getEffectiveExecutor() {
    // Attempt filtered request first
    WebRequestExecutor executor = getManager().getPossiblyFilteredRequestExecutor(this);
    if (executor == null) {
      // If a specific executor has been provided (e.g. for OAuth), use that;
      // otherwise, use manager
      executor = mExecutor;
      if (executor == null)
        executor = getManager();
    }
    return executor;
  }

  /**
   * If no throwable already set for the request, store one; update result map
   * accordingly
   */
  private void setThrowable(Throwable throwable) {
    if (mThrowable != null)
      return;
    mThrowable = throwable;
    String message = throwable.getMessage();
    if (message == null)
      message = "(unknown)";
    getOrBuildResultMap().put(WebTools.ERR, true).put(WebTools.MSG, message).put(WebTools.STACK_TRACE,
        stackTraceToList(throwable));
    pr(throwable);
    pr(getOrBuildResultMap());
  }

  private JSMap getOrBuildResultMap() {
    if (mResultMap == null)
      mResultMap = new JSMap();
    return mResultMap;
  }

  /**
   * Perform request (if not already), and determine if an error occurred
   */
  public boolean hasError() {
    return WebTools.hasError(resultMap());
  }

  public void setExecutor(WebRequestExecutor executor) {
    assertUnsent();
    mExecutor = executor;
  }

  private void setManager(WebRequestManager manager) {
    assertUnsent();
    mManager = manager;
  }

  private void setSent() {
    assertUnsent();
    mSentFlag = true;
    // If it's a POST, we will send the parameters as an entity (handled by subclass);
    // otherwise, add parameters to uri
    if (getVerb() != Verb.POST && getVerb() != Verb.PUT) {
      // Don't add an empty parameter list; else url with '?'
      if (!readArgs().isEmpty())
        mURIBuilder.addParameters(readParameters());
    }
    mURI = mURIBuilder.build();
  }

  public WebRequest setCredentials(String userName, String password) {
    assertUnsent();
    checkArgument(userName != null && password != null);
    mUserName = userName;
    mPassword = password;
    return this;
  }

  public String getUserName() {
    return mUserName;
  }

  public String getPassword() {
    return mPassword;
  }

  private static WebRequest build(WebRequestManager manager) {
    WebRequest request = new WebRequest();
    request.setManager(manager);
    return request;
  }

  /**
   * Build a WebRequest, given uri
   */
  public static WebRequest build(WebRequestManager manager, UriBuilder uri) {
    WebRequest request = build(manager);
    request.mURIBuilder = uri;
    return request;
  }

  /**
   * Build a WebRequest, given uri as string
   */
  public static WebRequest build(WebRequestManager manager, String url) {
    return build(manager, UriBuilder.build(url));
  }

  @Override
  public JSMap toJson() {
    JSMap map = super.toJson();
    map.put("uri", sent() ? mURI.toString() : mURIBuilder.toString());
    if (mUploadFile != null)
      map.put("uploadFile", mUploadFile.toString());
    map.put("parameters", readArgs());
    if (mResultMap != null)
      map.put("result", mResultMap);
    map.put("verb", mVerb.name());
    if (mPostFile != null)
      map.put("post_file", mPostFile.toString());
    return map;
  }

  /**
   * Determine if the request has already been sent
   */
  private boolean sent() {
    return mSentFlag;
  }

  private void assertSent() {
    if (!sent())
      throw new IllegalStateException("WebRequest not sent");
  }

  private void assertUnsent() {
    if (sent())
      throw new IllegalStateException("WebRequest already sent");
  }

  private WebRequestManager getManager() {
    return mManager;
  }

  private static final int DEFAULT_TIMEOUT = (int) DateTimeTools.SECONDS(15);

  // These fields are considered immutable once request is sent
  private UriBuilder mURIBuilder = new UriBuilder();
  private int mTimeout = DEFAULT_TIMEOUT;
  private Verb mVerb = Verb.GET;
  private JSMap mParameterMap = map();
  private WebRequestManager mManager;
  private WebRequestExecutor mExecutor;
  private String mUserName;
  private String mPassword;
  private File mUploadFile;
  private File mPostFile;

  private boolean mSentFlag;

  // These fields are only valid once request is sent

  private URI mURI;
  // Issue #680: inserting full package name makes problem go away
  private List<org.apache.http.NameValuePair> mParameterList;
  private Throwable mThrowable;
  private String mFetchedString;
  private JSMap mResultMap;
  private List<String> mHeaders = arrayList();
}
