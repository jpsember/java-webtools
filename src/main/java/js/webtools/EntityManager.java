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

import js.base.BaseObject;
import js.file.Files;
import js.webtools.gen.DynamicEntityInfo;
import js.webtools.gen.OsType;
import js.webtools.gen.RemoteEntityCollection;
import js.webtools.gen.RemoteEntityInfo;

/**
 * <pre>
 * 
 * Files of note
 * 
 * Static registry
 * =================
 * This is a RemoteEntityCollection that represents the known remote entities,
 * but without the fields that are likely to change frequently, specifically the
 * ngrok port and urls, and the 'active' entity id. It is to be tracked by git.
 * 
 * Dynamic registry
 * =================
 * This contains the 'active' entity id. It is not to be tracked by git.
 * 
 * </pre>
 */
public final class EntityManager extends BaseObject {

  public static EntityManager sharedInstance() {
    if (sSharedInstance == null)
      sSharedInstance = new EntityManager();
    return sSharedInstance;
  }

  private static EntityManager sSharedInstance;

  public EntityManager() {
    updateVerbose();
  }

  //------------------------------------------------------------------
  // Supplying Files object, to support dryrun operation
  // ------------------------------------------------------------------

  /**
   * To support dryrun operation, injectable Files object (initially Files.S)
   */
  public EntityManager withFiles(Files files) {
    mFiles = files;
    return this;
  }

  public RemoteEntityInfo entity(String id) {
    checkArgument(nonEmpty(id));
    RemoteEntityInfo ent = registry().entityMap().get(id);
    return ent;
  }

  public RemoteEntityCollection registry() {
    if (mRegistry == null) {
      log("reading registry");
      RemoteEntityCollection s = Files.parseAbstractData(RemoteEntityCollection.DEFAULT_INSTANCE,
          staticEntityFile());

      // Process the entries, applying any fixes; e.g. mismatched id, missing
      // default values
      Map<String, RemoteEntityInfo> updatedMap = hashMap();
      for (Map.Entry<String, RemoteEntityInfo> entry : s.entityMap().entrySet()) {
        String id = entry.getKey();
        RemoteEntityInfo original = entry.getValue();
        RemoteEntityInfo fixed = applyDefaults(id, original, s.entityTemplate()).build();
        if (!original.equals(fixed)) {
          log("Fixed entity:", id, INDENT, fixed, CR, "was:", CR, original);
          mRegistryModified = true;
        }
        updatedMap.put(id, fixed);
      }
      mRegistry = s.toBuilder().entityMap(updatedMap).build();
    }
    return mRegistry;
  }

  public String activeEntityId() {
    return dynamicRegistry().activeEntity();
  }

  public RemoteEntityInfo activeEntity() {
    String activeId = dynamicRegistry().activeEntity();
    RemoteEntityInfo info = entity(activeId);
    if (info == null)
      throw badState("Empty or missing active remote entity:", activeId);
    return info;
  }

  public void setActive(String key) {
    if (entity(key) == null)
      throw badArg("entity not found:", key);
    dynamicRegistry().activeEntity(key);
    flushChanges();
  }

  public void create(RemoteEntityInfo info) {
    log("create", INDENT, info);
    checkArgument(!info.id().isEmpty(), "invalid id:", INDENT, info);
    if (entity(info.id()) != null)
      throw badArg("entity already exists:", info.id());
    RemoteEntityInfo modified = applyDefaults(info.id(), info, registry().entityTemplate());

    Map<String, RemoteEntityInfo> newMap = hashMap();
    newMap.putAll(registry().entityMap());
    newMap.put(info.id(), modified);
    mRegistry = mRegistry.toBuilder().entityMap(newMap).build();
    mRegistryModified = true;
    flushChanges();
  }

  private RemoteEntityInfo.Builder applyDefaults(String id, RemoteEntityInfo entity,
      RemoteEntityInfo template) {
    RemoteEntityInfo.Builder builder = entity.toBuilder();
    builder.id(id);
    if (builder.osType() == OsType.UNKNOWN)
      builder.osType(template.osType());
    if (builder.user().isEmpty())
      builder.user(template.user());
    if (Files.empty(builder.projectDir()))
      builder.projectDir(template.projectDir());
    // Clear out the dynamic elements, which shouldn't exist (except in an older version), 
    // as they are never written back to the registry
    builder.port(null).url(null);
    return builder;
  }

  private static final String DYNAMIC_REGISTRY_NAME = ".entity_map.json";
  private static final String STATIC_REGISTRY_NAME = "entity_map.json";

  private File dynamicEntityFile() {
    return files().fileWithinProjectConfigDirectory(DYNAMIC_REGISTRY_NAME);
  }

  private File staticEntityFile() {
    return files().fileWithinProjectConfigDirectory(STATIC_REGISTRY_NAME);
  }

  private DynamicEntityInfo.Builder dynamicRegistry() {
    if (mDynamicRegistry == null) {
      mOriginalDynamicRegistry = Files.parseAbstractDataOpt(DynamicEntityInfo.DEFAULT_INSTANCE,
          dynamicEntityFile());
      mDynamicRegistry = mOriginalDynamicRegistry.toBuilder();
    }
    return mDynamicRegistry;
  }

  private void flushChanges() {
    if (mRegistryModified) {
      File file = staticEntityFile();
      log("flushing changes to static registry:", file);
      files().writePretty(file, registry());
      mRegistryModified = false;
    }

    // Don't attempt to flush dynamic registry if it hasn't yet been loaded
    if (mDynamicRegistry != null) {
      DynamicEntityInfo dynamicCurrent = dynamicRegistry().build();
      if (!dynamicCurrent.equals(mOriginalDynamicRegistry)) {
        File file = dynamicEntityFile();
        log("flushing changes to dynamic registry:", file);
        mOriginalDynamicRegistry = dynamicCurrent;
        files().writePretty(file, dynamicCurrent);
      }
    }
  }

  private Files files() {
    return mFiles;
  }

  private RemoteEntityCollection mRegistry;
  private boolean mRegistryModified;
  private DynamicEntityInfo mOriginalDynamicRegistry;
  private DynamicEntityInfo.Builder mDynamicRegistry;
  private Files mFiles = Files.S;

}
