package js.webtools.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class CloudFileEntry implements AbstractData {

  public String name() {
    return mName;
  }

  public long size() {
    return mSize;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "name";
  protected static final String _1 = "size";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mName);
    m.putUnsafe(_1, mSize);
    return m;
  }

  @Override
  public CloudFileEntry build() {
    return this;
  }

  @Override
  public CloudFileEntry parse(Object obj) {
    return new CloudFileEntry((JSMap) obj);
  }

  private CloudFileEntry(JSMap m) {
    mName = m.opt(_0, "");
    mSize = m.opt(_1, 0L);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CloudFileEntry))
      return false;
    CloudFileEntry other = (CloudFileEntry) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mName.equals(other.mName)))
      return false;
    if (!(mSize == other.mSize))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mName.hashCode();
      r = r * 37 + (int)mSize;
      m__hashcode = r;
    }
    return r;
  }

  protected String mName;
  protected long mSize;
  protected int m__hashcode;

  public static final class Builder extends CloudFileEntry {

    private Builder(CloudFileEntry m) {
      mName = m.mName;
      mSize = m.mSize;
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
    public CloudFileEntry build() {
      CloudFileEntry r = new CloudFileEntry();
      r.mName = mName;
      r.mSize = mSize;
      return r;
    }

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder size(long x) {
      mSize = x;
      return this;
    }

  }

  public static final CloudFileEntry DEFAULT_INSTANCE = new CloudFileEntry();

  private CloudFileEntry() {
    mName = "";
  }

}
