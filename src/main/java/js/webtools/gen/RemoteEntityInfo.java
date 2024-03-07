package js.webtools.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class RemoteEntityInfo implements AbstractData {

  public String label() {
    return mLabel;
  }

  public String user() {
    return mUser;
  }

  public String url() {
    return mUrl;
  }

  public int port() {
    return mPort;
  }

  public String host() {
    return mHost;
  }

  public JSMap hostInfo() {
    return mHostInfo;
  }

  public File projectDir() {
    return mProjectDir;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "label";
  protected static final String _1 = "user";
  protected static final String _2 = "url";
  protected static final String _3 = "port";
  protected static final String _4 = "host";
  protected static final String _5 = "host_info";
  protected static final String _6 = "project_dir";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mLabel);
    m.putUnsafe(_1, mUser);
    m.putUnsafe(_2, mUrl);
    m.putUnsafe(_3, mPort);
    m.putUnsafe(_4, mHost);
    m.putUnsafe(_5, mHostInfo);
    m.putUnsafe(_6, mProjectDir.toString());
    return m;
  }

  @Override
  public RemoteEntityInfo build() {
    return this;
  }

  @Override
  public RemoteEntityInfo parse(Object obj) {
    return new RemoteEntityInfo((JSMap) obj);
  }

  private RemoteEntityInfo(JSMap m) {
    mLabel = m.opt(_0, "");
    mUser = m.opt(_1, "root");
    mUrl = m.opt(_2, "");
    mPort = m.opt(_3, 0);
    mHost = m.opt(_4, "");
    {
      mHostInfo = JSMap.DEFAULT_INSTANCE;
      JSMap x = m.optJSMap(_5);
      if (x != null) {
        mHostInfo = x.lock();
      }
    }
    {
      mProjectDir = Files.DEFAULT;
      String x = m.opt(_6, (String) null);
      if (x != null) {
        mProjectDir = new File(x);
      }
    }
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof RemoteEntityInfo))
      return false;
    RemoteEntityInfo other = (RemoteEntityInfo) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mLabel.equals(other.mLabel)))
      return false;
    if (!(mUser.equals(other.mUser)))
      return false;
    if (!(mUrl.equals(other.mUrl)))
      return false;
    if (!(mPort == other.mPort))
      return false;
    if (!(mHost.equals(other.mHost)))
      return false;
    if (!(mHostInfo.equals(other.mHostInfo)))
      return false;
    if (!(mProjectDir.equals(other.mProjectDir)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mLabel.hashCode();
      r = r * 37 + mUser.hashCode();
      r = r * 37 + mUrl.hashCode();
      r = r * 37 + mPort;
      r = r * 37 + mHost.hashCode();
      r = r * 37 + mHostInfo.hashCode();
      r = r * 37 + mProjectDir.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mLabel;
  protected String mUser;
  protected String mUrl;
  protected int mPort;
  protected String mHost;
  protected JSMap mHostInfo;
  protected File mProjectDir;
  protected int m__hashcode;

  public static final class Builder extends RemoteEntityInfo {

    private Builder(RemoteEntityInfo m) {
      mLabel = m.mLabel;
      mUser = m.mUser;
      mUrl = m.mUrl;
      mPort = m.mPort;
      mHost = m.mHost;
      mHostInfo = m.mHostInfo;
      mProjectDir = m.mProjectDir;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    @Override
    public int hashCode() {
      m__hashcode = 0;
      return super.hashCode();
    }

    @Override
    public RemoteEntityInfo build() {
      RemoteEntityInfo r = new RemoteEntityInfo();
      r.mLabel = mLabel;
      r.mUser = mUser;
      r.mUrl = mUrl;
      r.mPort = mPort;
      r.mHost = mHost;
      r.mHostInfo = mHostInfo;
      r.mProjectDir = mProjectDir;
      return r;
    }

    public Builder label(String x) {
      mLabel = (x == null) ? "" : x;
      return this;
    }

    public Builder user(String x) {
      mUser = (x == null) ? "root" : x;
      return this;
    }

    public Builder url(String x) {
      mUrl = (x == null) ? "" : x;
      return this;
    }

    public Builder port(int x) {
      mPort = x;
      return this;
    }

    public Builder host(String x) {
      mHost = (x == null) ? "" : x;
      return this;
    }

    public Builder hostInfo(JSMap x) {
      mHostInfo = (x == null) ? JSMap.DEFAULT_INSTANCE : x;
      return this;
    }

    public Builder projectDir(File x) {
      mProjectDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

  }

  public static final RemoteEntityInfo DEFAULT_INSTANCE = new RemoteEntityInfo();

  private RemoteEntityInfo() {
    mLabel = "";
    mUser = "root";
    mUrl = "";
    mHost = "";
    mHostInfo = JSMap.DEFAULT_INSTANCE;
    mProjectDir = Files.DEFAULT;
  }

}
