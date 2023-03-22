/**
 * MIT License
 * 
 * Copyright (c) 2022 Jeff Sember
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
import java.util.Map;
import java.util.regex.Matcher;

import js.base.BaseObject;
import js.base.SystemCall;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;
import js.webtools.gen.RemoteEntityInfo;

/**
 * This class is threadsafe
 */
public class Ngrok extends BaseObject {

  // ------------------------------------------------------------------
  // Singleton implementation
  // ------------------------------------------------------------------

  public static Ngrok sharedInstance() {
    if (sSharedInstance == null) {
      sSharedInstance = new Ngrok();
    }
    return sSharedInstance;
  }

  private static Ngrok sSharedInstance;

  private Ngrok() {
  }
  
  // ------------------------------------------------------------------

  /**
   * Request a refresh of the entity information read from ngrok
   */
  public Ngrok refresh() {
    mRequestRefresh = true;
    return this;
  }

  /**
   * Given a RemoteEntityInfo, return a copy that has the ngrok's tunnel and
   * port fields filled in; or null if no ngrok information exists
   */
  public RemoteEntityInfo addNgrokInfo(RemoteEntityInfo entity, boolean mustExist) {
    RemoteEntityInfo tunnel = remoteEntityInfoMap().get(entity.id());
    if (tunnel == null) {
      if (mustExist)
        throw badArg("no ngrok tunnel found for entity:", entity.id());
      log("*** no ngrok tunnel found for entity:", entity.id());
      return null;
    }
    return entity.build().toBuilder().url(tunnel.url()).port(tunnel.port()).build();
  }

  /**
   * Get map of entity tags => RemoteEntityInfo
   */
  private Map<String, RemoteEntityInfo> remoteEntityInfoMap() {
    updateVerbose();
    if (mRequestRefresh || mTunnelMap == null) {
      synchronized (this) {
        Map<String, RemoteEntityInfo> newMap = treeMap();
        JSList tunnelList = ngrokTunnelMap();
        for (JSMap tunMap : tunnelList.asMaps()) {
          String metadata = tunMap.opt("metadata", "");
          if (metadata.isEmpty()) {
            // If this is not a tcp tunnel, ignore
            if (!tunMap.get("proto").equals("tcp"))
              continue;
          }
          if (metadata.isEmpty()) {
            pr("*** ngrok tunnel has no metadata, public_url:", tunMap.get("public_url"), "tunnel map:",
                INDENT, tunMap);
            continue;
          }

          if (newMap.containsKey(metadata)) {
            pr("*** multiple tunnels sharing same metadata:", metadata);
            continue;
          }

          String publicUrl = tunMap.get("public_url");
          chompPrefix(publicUrl, "tcp://");
          Matcher matcher = RegExp.matcher("tcp:\\/\\/(.+):(\\d+)", publicUrl);
          if (!matcher.matches()) {
            pr("*** failed to parse public_url:", publicUrl);
            continue;
          }
          RemoteEntityInfo result = RemoteEntityInfo.newBuilder()//
              .url(matcher.group(1)) //
              .port(Integer.parseInt(matcher.group(2)))//
              .build();
          log("parsed public url:", publicUrl, CR, "to entity info:", INDENT, result);
          newMap.put(metadata, result);
        }
        mTunnelMap = newMap;
      }
    }
    return mTunnelMap;
  }

  /**
   * Call the ngrok API, with a particular endpoint appended to their url.
   */
  private JSMap callAPI(String endpoint) {
    SystemCall sc = new SystemCall();
    sc.setVerbose(verbose());
    sc.arg("curl", "-sS");
    sc.arg("-H", "Accept: application/json");
    sc.arg("-H", "Authorization: Bearer " + getNgrokToken());
    sc.arg("-H", "ngrok-version: 2");
    sc.arg("https://api.ngrok.com/" + endpoint);
    sc.assertSuccess();
    JSMap result = new JSMap(sc.systemOut());
    return result;
  }

  private String getNgrokToken() {
    if (mToken == null) {
      File tokenFile = new File(Files.S.projectSecretsDirectory(), "ngrok_token.txt");
      checkState(tokenFile.exists(), "no such file:", tokenFile);
      mToken = Files.readString(tokenFile).trim();
    }
    return mToken;
  }

  private JSList ngrokTunnelMap() {
    JSMap apiResult = callAPI("tunnels");
    log("Called api:", INDENT, apiResult);
    JSList result = apiResult.getList("tunnels");
    log("tunnels:", INDENT, result);
    return result;
  }

  private boolean mRequestRefresh;
  private Map<String, RemoteEntityInfo> mTunnelMap;
  private String mToken;

}
