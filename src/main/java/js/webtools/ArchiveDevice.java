package js.webtools;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import js.base.BaseObject;
import js.webtools.gen.CloudFileEntry;

/**
 * Interface to a filesystem that acts as an external storage device, e.g. AWS
 * S3
 */
public abstract class ArchiveDevice extends BaseObject {

  public abstract void setDryRun(boolean dryRun);

  /**
   * Determine if an object exists in the archive
   */
  public abstract boolean fileExists(String name);

  /**
   * Push a local object to the archive
   * 
   * @param name
   *          if null or empty, uses source.getName()
   */
  public abstract void push(File source, String name);

  /**
   * Open a stream for writing a new object to the archive
   */
  public abstract OutputStream openForPush(String name);

  /**
   * Pull an object from the archive to the local machine
   * 
   * @param destination
   *          if this is a directory, pulls to a file in this directory with
   *          name 'name'
   */
  public abstract void pull(String name, File destination);

  /**
   * Get a list of items within the archive
   */
  public abstract List<CloudFileEntry> listFiles(String prefixOrNull);

  /**
   * For test purposes: simulate a network outage for any subsequent calls
   */
  public final void setSimulatedNetworkProblem(boolean flag) {
    if (mSimulatedNetworkProblem == flag)
      return;
    log("=== ArchiveDevice simulated network problem state changing to:", flag);
    mSimulatedNetworkProblem = flag;
  }

  public final void setWritesDisabled(boolean flag) {
    if (mWritesDisabled == flag)
      return;
    log("=== ArchiveDevice writes disabled changing to:", flag);
    mWritesDisabled = flag;
  }

  public final boolean writesDisabled() {
    return mWritesDisabled;
  }

  private boolean mSimulatedNetworkProblem;
  private boolean mWritesDisabled;

}
