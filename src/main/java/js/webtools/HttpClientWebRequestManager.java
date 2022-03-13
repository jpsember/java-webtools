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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import js.json.JSMap;
import js.webtools.WebRequest.Verb;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpClientWebRequestManager extends WebRequestManager {

  public HttpClientWebRequestManager() {
  }

  @Override
  public String makeRequest(WebRequest request) {
    CloseableHttpClient httpClient;
    httpClient = HttpClients.createDefault();
    URI uri = request.uri();
    HttpRequestBase httpRequest;

    switch (request.getVerb()) {
    case GET:
      httpRequest = new HttpGet(uri);
      if (request.isJsonRequestType()) {
        httpRequest.setHeader("Content-Type", "application/json");
      }
      break;
    case POST:
    case PUT: {
      HttpEntityEnclosingRequestBase httpEntityRequest;
      if (request.getVerb() == Verb.POST)
        httpEntityRequest = new HttpPost(uri);
      else
        httpEntityRequest = new HttpPut(uri);
      HttpEntity entity;
      // Issue #1159: don't use multipart entity if we're not uploading files;
      // refreshing GMail tokens doesn't accept such requests.
      // This also seems to have been the problem of issue #1151.
      File uploadFile = request.getUploadFile();
      if (uploadFile != null) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(uploadFile.getName(), uploadFile);
        JSMap params = request.readArgs();
        for (String key : params.keySet())
          builder.addTextBody(key, params.getUnsafe(key).toString(),
              ContentType.create("text/plain", Charset.forName("UTF-8")));
        entity = builder.build();
      } else if (request.readArgs().containsKey(WebRequest.KEY_CONTENT_TYPE_JSON)) {
        JSMap json = request.readArgs().getMap(WebRequest.KEY_CONTENT_TYPE_JSON);
        entity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
      } else {
        try {
          entity = new UrlEncodedFormEntity(request.readParameters());
        } catch (UnsupportedEncodingException e) {
          throw asIllegalArgumentException(e);
        }
      }
      httpEntityRequest.setEntity(entity);
      httpRequest = httpEntityRequest;
    }
      break;
    case DELETE:
      httpRequest = new HttpDelete(uri);
      break;
    default:
      throw notSupported("verb not supported:", request.getVerb());
    }

    int timeout = request.getTimeout();
    if (mRequestConfig == null || timeout != mPreviousTimeout) {
      mPreviousTimeout = timeout;
      int ms = (int) timeout;
      mRequestConfig = RequestConfig.custom().setSocketTimeout(ms).setConnectTimeout(ms)
          .setConnectionRequestTimeout(ms).build();
    }
    httpRequest.setConfig(mRequestConfig);

    if (request.getUserName() != null) {
      String userInfoString = request.getUserName() + ":" + request.getPassword();
      byte[] sBytes = userInfoString.getBytes(StandardCharsets.UTF_8);
      String encoding = Base64.encodeBase64String(sBytes);
      String authorizationString = "Basic " + encoding;
      request.addHeader("Authorization", authorizationString);
    }

    List<String> headers = request.getHeaders();
    for (int i = 0; i < headers.size(); i += 2) {
      httpRequest.addHeader(headers.get(i), headers.get(i + 1));
    }

    CloseableHttpResponse httpResponse = null;
    try {
      httpResponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpResponse.getEntity();
      return EntityUtils.toString(entity);
    } catch (IOException e) {
      throw asRuntimeException(e);
    } finally {
      if (httpResponse != null) {
        try {
          httpResponse.close();
        } catch (IOException e) {
          pr(e);
        }
      }
    }
  }

  @Override
  public boolean canRetryAfter(Throwable throwable) {
    while (throwable != null) {
      if (throwable instanceof java.net.SocketException)
        return true;
      throwable = throwable.getCause();
    }
    return false;
  }

  private int mPreviousTimeout;
  private RequestConfig mRequestConfig;
}
