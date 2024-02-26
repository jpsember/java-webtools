package js.webtools.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class S3Params implements AbstractData {

  public String profile() {
    return mProfile;
  }

  public String bucketName() {
    return mBucketName;
  }

  public String folderPath() {
    return mFolderPath;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "profile";
  protected static final String _1 = "bucket_name";
  protected static final String _2 = "folder_path";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mProfile);
    m.putUnsafe(_1, mBucketName);
    m.putUnsafe(_2, mFolderPath);
    return m;
  }

  @Override
  public S3Params build() {
    return this;
  }

  @Override
  public S3Params parse(Object obj) {
    return new S3Params((JSMap) obj);
  }

  private S3Params(JSMap m) {
    mProfile = m.opt(_0, "");
    mBucketName = m.opt(_1, "");
    mFolderPath = m.opt(_2, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof S3Params))
      return false;
    S3Params other = (S3Params) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mProfile.equals(other.mProfile)))
      return false;
    if (!(mBucketName.equals(other.mBucketName)))
      return false;
    if (!(mFolderPath.equals(other.mFolderPath)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mProfile.hashCode();
      r = r * 37 + mBucketName.hashCode();
      r = r * 37 + mFolderPath.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mProfile;
  protected String mBucketName;
  protected String mFolderPath;
  protected int m__hashcode;

  public static final class Builder extends S3Params {

    private Builder(S3Params m) {
      mProfile = m.mProfile;
      mBucketName = m.mBucketName;
      mFolderPath = m.mFolderPath;
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
    public S3Params build() {
      S3Params r = new S3Params();
      r.mProfile = mProfile;
      r.mBucketName = mBucketName;
      r.mFolderPath = mFolderPath;
      return r;
    }

    public Builder profile(String x) {
      mProfile = (x == null) ? "" : x;
      return this;
    }

    public Builder bucketName(String x) {
      mBucketName = (x == null) ? "" : x;
      return this;
    }

    public Builder folderPath(String x) {
      mFolderPath = (x == null) ? "" : x;
      return this;
    }

  }

  public static final S3Params DEFAULT_INSTANCE = new S3Params();

  private S3Params() {
    mProfile = "";
    mBucketName = "";
    mFolderPath = "";
  }

}
