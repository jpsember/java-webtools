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

public class Ngrok extends BaseObject {

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

  /**
   * Discard any cached tunnels that may have previously been read
   */
  public Ngrok discardTunnels() {
    mCachedTunnels = null;
    mTunnelMap = null;
    return this;
  }

  /**
   * Get map of entity tags => RemoteEntityInfo
   */
  public Map<String, RemoteEntityInfo> remoteEntityInfoMap() {
    todo(
        "refactor so this is thread safe, perhaps updated periodically from network thread? Or, only when problem occurs?");
    if (mTunnelMap == null) {
      updateVerbose();
      Map<String, RemoteEntityInfo> newMap = treeMap();
      for (JSMap tunMap : ngrokTunnelMap().asMaps()) {
        String metadata = tunMap.opt("metadata", "");
        if (metadata.isEmpty()) {
          // If this is not a tcp tunnel, ignore
          if (!tunMap.get("proto").equals("tcp"))
            continue;
        }
        if (metadata.isEmpty()) {
          pr("*** ngrok tunnel has no metadata, public_url:", tunMap.get("public_url"), "tunnel map:", INDENT,
              tunMap);
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
    return mTunnelMap;
  }

  /**
   * Given a 'static' RemoteEntityInfo, return a copy that has the ngrok's
   * tunnel and port fields filled in; or null if no ngrok information exists
   */
  public RemoteEntityInfo addNgrokInfo(RemoteEntityInfo entity) {
    todo(
        "refactor so this is thread safe, perhaps updated periodically from network thread? Or, only when problem occurs?");
    RemoteEntityInfo tunnel = remoteEntityInfoMap().get(entity.id());
    if (tunnel == null) {
      log("*** no ngrok tunnel found for entity:", entity.id());
      return null;
    }
    return entity.build().toBuilder().url(tunnel.url()).port(tunnel.port()).build();
  }

  private Map<String, RemoteEntityInfo> mTunnelMap;

  private String getNgrokToken() {
    if (mToken == null) {
      File tokenFile = new File(Files.S.projectSecretsDirectory(), "ngrok_token.txt");
      checkState(tokenFile.exists(), "no such file:", tokenFile);
      mToken = Files.readString(tokenFile).trim();
    }
    return mToken;
  }

  private JSList ngrokTunnelMap() {
    if (mCachedTunnels == null) {
      updateVerbose();
      JSMap apiResult = callAPI("tunnels");
      log("Called api:", INDENT, apiResult);
      mCachedTunnels = apiResult.getList("tunnels");
      log("tunnels:", INDENT, mCachedTunnels);
    }
    return mCachedTunnels;
  }

  private JSList mCachedTunnels;

  private String mToken;

}
