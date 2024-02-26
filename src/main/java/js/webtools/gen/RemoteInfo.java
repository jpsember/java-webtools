package js.webtools.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class RemoteInfo implements AbstractData {

  public String activeHandlerName() {
    return mActiveHandlerName;
  }

  public RemoteEntityInfo activeEntity() {
    return mActiveEntity;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "active_handler_name";
  protected static final String _1 = "active_entity";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mActiveHandlerName);
    m.putUnsafe(_1, mActiveEntity.toJson());
    return m;
  }

  @Override
  public RemoteInfo build() {
    return this;
  }

  @Override
  public RemoteInfo parse(Object obj) {
    return new RemoteInfo((JSMap) obj);
  }

  private RemoteInfo(JSMap m) {
    mActiveHandlerName = m.opt(_0, "");
    {
      mActiveEntity = RemoteEntityInfo.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_1);
      if (x != null) {
        mActiveEntity = RemoteEntityInfo.DEFAULT_INSTANCE.parse(x);
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
    if (object == null || !(object instanceof RemoteInfo))
      return false;
    RemoteInfo other = (RemoteInfo) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mActiveHandlerName.equals(other.mActiveHandlerName)))
      return false;
    if (!(mActiveEntity.equals(other.mActiveEntity)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mActiveHandlerName.hashCode();
      r = r * 37 + mActiveEntity.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mActiveHandlerName;
  protected RemoteEntityInfo mActiveEntity;
  protected int m__hashcode;

  public static final class Builder extends RemoteInfo {

    private Builder(RemoteInfo m) {
      mActiveHandlerName = m.mActiveHandlerName;
      mActiveEntity = m.mActiveEntity;
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
    public RemoteInfo build() {
      RemoteInfo r = new RemoteInfo();
      r.mActiveHandlerName = mActiveHandlerName;
      r.mActiveEntity = mActiveEntity;
      return r;
    }

    public Builder activeHandlerName(String x) {
      mActiveHandlerName = (x == null) ? "" : x;
      return this;
    }

    public Builder activeEntity(RemoteEntityInfo x) {
      mActiveEntity = (x == null) ? RemoteEntityInfo.DEFAULT_INSTANCE : x.build();
      return this;
    }

  }

  public static final RemoteInfo DEFAULT_INSTANCE = new RemoteInfo();

  private RemoteInfo() {
    mActiveHandlerName = "";
    mActiveEntity = RemoteEntityInfo.DEFAULT_INSTANCE;
  }

}
