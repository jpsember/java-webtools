package js.webtools.gen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import js.data.AbstractData;
import js.data.DataUtil;
import js.json.JSMap;

public class RemoteInfo implements AbstractData {

  public String activeHandlerName() {
    return mActiveHandlerName;
  }

  public RemoteEntityInfo activeEntity() {
    return mActiveEntity;
  }

  public Map<String, RemoteEntityInfo> userEntities() {
    return mUserEntities;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "active_handler_name";
  protected static final String _1 = "active_entity";
  protected static final String _2 = "user_entities";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mActiveHandlerName);
    m.putUnsafe(_1, mActiveEntity.toJson());
    {
      JSMap j = new JSMap();
      for (Map.Entry<String, RemoteEntityInfo> e : mUserEntities.entrySet())
        j.put(e.getKey(), e.getValue().toJson());
      m.put(_2, j);
    }
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
    mActiveHandlerName = m.opt(_0, "user");
    {
      mActiveEntity = RemoteEntityInfo.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_1);
      if (x != null) {
        mActiveEntity = RemoteEntityInfo.DEFAULT_INSTANCE.parse(x);
      }
    }
    {
      mUserEntities = DataUtil.emptyMap();
      {
        JSMap m2 = m.optJSMap("user_entities");
        if (m2 != null && !m2.isEmpty()) {
          Map<String, RemoteEntityInfo> mp = new ConcurrentHashMap<>();
          for (Map.Entry<String, Object> e : m2.wrappedMap().entrySet())
            mp.put(e.getKey(), RemoteEntityInfo.DEFAULT_INSTANCE.parse((JSMap) e.getValue()));
          mUserEntities = mp;
        }
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
    if (!(mUserEntities.equals(other.mUserEntities)))
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
      r = r * 37 + mUserEntities.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mActiveHandlerName;
  protected RemoteEntityInfo mActiveEntity;
  protected Map<String, RemoteEntityInfo> mUserEntities;
  protected int m__hashcode;

  public static final class Builder extends RemoteInfo {

    private Builder(RemoteInfo m) {
      mActiveHandlerName = m.mActiveHandlerName;
      mActiveEntity = m.mActiveEntity;
      mUserEntities = DataUtil.mutableCopyOf(m.mUserEntities);
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
      r.mUserEntities = DataUtil.immutableCopyOf(mUserEntities);
      return r;
    }

    public Builder activeHandlerName(String x) {
      mActiveHandlerName = (x == null) ? "user" : x;
      return this;
    }

    public Builder activeEntity(RemoteEntityInfo x) {
      mActiveEntity = (x == null) ? RemoteEntityInfo.DEFAULT_INSTANCE : x.build();
      return this;
    }

    public Builder userEntities(Map<String, RemoteEntityInfo> x) {
      mUserEntities = DataUtil.mutableCopyOf((x == null) ? DataUtil.emptyMap() : x);
      return this;
    }

  }

  public static final RemoteInfo DEFAULT_INSTANCE = new RemoteInfo();

  private RemoteInfo() {
    mActiveHandlerName = "user";
    mActiveEntity = RemoteEntityInfo.DEFAULT_INSTANCE;
    mUserEntities = DataUtil.emptyMap();
  }

}
