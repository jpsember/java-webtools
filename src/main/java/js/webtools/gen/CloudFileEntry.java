package js.webtools.gen;

import js.data.AbstractData;
import js.json.JSMap;

/**
 * Generated Java data class (do not edit!)
 *
 * Instances of this class should be considered immutable.  A mutable copy of an instance
 * can be constructed by calling the toBuilder() method.  When clients pass instances to other
 * code, if mutation of those instances is not desired, then the client should ensure that the
 * instance is not actually a Builder (e.g. by calling build() if necessary).
 */
public class CloudFileEntry implements AbstractData {

  // Field 'getters'

  public String name() {
    return mName;
  }

  public long size() {
    return mSize;
  }

  /**
   * Construct a builder from this data class object.
   * Where appropriate, this object's values are defensively copied to mutable versions
   */
  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  // Constants used to refer to the fields, e.g., as they appear in json maps

  public static final String NAME = "name";
  public static final String SIZE = "size";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  /**
   * Serialize this object to a json map
   */
  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.put(NAME, mName);
    m.put(SIZE, mSize);
    return m;
  }

  /**
   * The instance is already immutable, so return unchanged
   */
  @Override
  public CloudFileEntry build() {
    return this;
  }

  @Override
  public CloudFileEntry parse(Object obj) {
    return new CloudFileEntry((JSMap) obj);
  }

  private CloudFileEntry(JSMap m) {
    mName = m.opt(NAME, "");
    mSize = m.opt(SIZE, 0L);
  }

  /**
   * Construct a new builder for objects of this data class
   */
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

    /**
     * Create an immutable version of this builder.  Where appropriate, defensive, immutable copies
     * are made of the builder fields.
     */
    @Override
    public CloudFileEntry build() {
      CloudFileEntry r = new CloudFileEntry();
      r.mName = mName;
      r.mSize = mSize;
      return r;
    }

    // Field 'setters'.  Where appropriate, if an argument is immutable, a mutable copy is stored instead

    public Builder name(String x) {
      mName = (x == null) ? "" : x;
      return this;
    }

    public Builder size(long x) {
      mSize = x;
      return this;
    }

  }

  /**
   * The default (immutable) instance of this data object
   */
  public static final CloudFileEntry DEFAULT_INSTANCE = new CloudFileEntry();

  /**
   * The private constructor.  To create new instances, use newBuilder()
   */
  private CloudFileEntry() {
    mName = "";
  }

}
