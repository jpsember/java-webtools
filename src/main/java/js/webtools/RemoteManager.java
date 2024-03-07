package js.webtools;

import java.io.File;

import js.base.BaseObject;
import js.file.Files;
import js.webtools.gen.RemoteEntityInfo;
import js.webtools.gen.RemoteInfo;

import static js.base.Tools.*;

public class RemoteManager extends BaseObject {

  public static final RemoteManager SHARED_INSTANCE = new RemoteManager();

  private RemoteManager() {
    //alertVerbose();
  }

  public RemoteInfo.Builder info() {
    if (mRemoteInfo == null) {
      var f = persistFile();
      mRemoteInfo = Files.parseAbstractDataOpt(RemoteInfo.DEFAULT_INSTANCE, persistFile()).toBuilder();
      log("parsed RemoteInfo from:", INDENT, Files.infoMap(f), CR, mRemoteInfo);
      sRemoteInfoModified = false;
    }
    return mRemoteInfo;
  }

  public RemoteInfo.Builder infoEdit() {
    var b = info();
    sRemoteInfoModified = true;
    return b;
  }

  private File persistFile() {
    return new File(Files.homeDirectory(), ".remote_info");
  }

  public void flush() {
    if (!sRemoteInfoModified)
      return;
    Files.S.write(persistFile(), info());
    sRemoteInfoModified = false;
  }

  public RemoteEntityInfo activeEntityOpt() {
    return info().activeEntity();
  }

  public RemoteEntityInfo activeEntity() {
    var r = activeEntityOpt();
    if (r == null)
      throw badState("no active entity");
    return r;
  }

  public void createSSHScript() {
    var ent = activeEntity();
    checkArgument(nonEmpty(ent.user()), "no user:", INDENT, ent);
    StringBuilder sb = new StringBuilder();
    sb.append("#!/usr/bin/env bash\n");
    sb.append("echo \"Connecting to: ");
    sb.append(ent.label());
    sb.append("\"\n");
    sb.append("ssh ");
    sb.append(ent.user());
    sb.append("@");
    sb.append(ent.url());
    sb.append(" -oStrictHostKeyChecking=no");
    sb.append(" $@");
    sb.append('\n');
    File f = new File(Files.binDirectory(), "sshe");
    var fl = Files.S;
    fl.writeString(f, sb.toString());
    fl.chmod(f, 755);
  }

  private RemoteInfo.Builder mRemoteInfo;
  private boolean sRemoteInfoModified;

}
