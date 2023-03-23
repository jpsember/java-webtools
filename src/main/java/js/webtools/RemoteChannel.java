package js.webtools;

import static js.base.Tools.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.ChannelSftp.LsEntrySelector;

import js.base.BaseObject;
import js.base.DateTimeTools;
import js.data.AbstractData;
import js.data.DataUtil;
import js.file.Files;
import js.webtools.gen.RemoteEntityInfo;

public class RemoteChannel extends BaseObject implements AutoCloseable {

  public RemoteChannel withPrivateKey(String privateKeyFilename) {
    mPrivateKeyFilename = checkNonEmpty(privateKeyFilename);
    return this;
  }

  public RemoteChannel withEntity(String entityId) {
    mEntityId = entityId;
    return this;
  }

  public RemoteChannel withHomeDirectory(File homeDir) {
    mHomeDirectory = Files.assertExists(Files.assertAbsolute(homeDir));
    return this;
  }

  @Override
  public void close() {
    if (mChannelSftp != null) {
      try {
        mChannelSftp.disconnect();
      } catch (Throwable ignored) {
      }
      mChannelSftp = null;
    }

    if (mSession != null) {
      try {
        mSession.disconnect();
      } catch (Throwable ignored) {
      }
      mSession = null;
    }
  }

  public List<String> getDirectoryContents(File remoteDirectory, String extensionOrNull) {
    String suffix = null;
    if (nonEmpty(extensionOrNull))
      suffix = "." + extensionOrNull;

    mEntryList.clear();
    String path = remoteDirectory.toString();

    try {
      channelSftp().ls(path, mEntrySelector);
    } catch (SftpException e) {
      throw Files.asFileException(e);
    }
    List<String> files = arrayList();
    if (suffix == null)
      files.addAll(mEntryList);
    else
      for (String f : mEntryList)
        if (f.endsWith(suffix))
          files.add(f);
    return files;
  }

  private List<String> mEntryList = arrayList();
  private LsEntrySelector mEntrySelector = new LsEntrySelector() {
    @Override
    public int select(LsEntry entry) {
      String name = entry.getFilename();
      if (!(name.equals(".") || name.equals(".."))) {
        mEntryList.add(name);
      }
      return LsEntrySelector.CONTINUE;
    }
  };

  /**
   * Write byte array to file, with atomic renaming for thread safety
   * 
   * If targetFile = "xxx.yyy", assumes no file "xxx.yyy.tmp" exists
   */
  public void writeFileAtomically(byte[] data, File remoteTargetFile) {

    // See https://epaul.github.io/jsch-documentation/javadoc/

    String finalTargetStr = remoteTargetFile.toString();
    String tempTargetStr = finalTargetStr + ".tmp";

    ByteArrayInputStream strm = new ByteArrayInputStream(data);

    try {

      channelSftp().put(strm, tempTargetStr);
      channelSftp().rename(tempTargetStr, finalTargetStr);

    } catch (SftpException t) {
      throw Files.asFileException(t);
    }
  }

  public void writeFileAtomically(AbstractData data, File remoteTargetFile) {
    writeFileAtomically(DataUtil.toByteArray(data), remoteTargetFile);
  }

  /**
   * Read file from remote machine
   */
  public byte[] readFile(File remoteFile) {
    ByteArrayOutputStream os = new ByteArrayOutputStream((int) DataUtil.ONE_KB);
    try {
      if (verbose())
        checkpoint("readFile");
      channelSftp().get(remoteFile.toString(), os);
      if (verbose())
        checkpoint("readFile done");
    } catch (SftpException e) {
      throw Files.asFileException(e);
    }
    return os.toByteArray();
  }

  /**
   * Delete file from remote machine. Throws an exception if there is no such
   * file
   */
  public void deleteFile(File remoteFile) {
    try {
      channelSftp().rm(remoteFile.toString());
    } catch (SftpException t) {
      throw Files.asFileException(t);
    }
  }

  private Session session() {
    if (mSession == null) {
      log("constructing secure channel");

      checkNonEmpty(mEntityId, "no entity id");

      mChannelSftp = null;

      try {
        JSch jsch = new JSch();

        File sshDir = new File(homeDirectory(), ".ssh");
        File privateKey = Files.assertExists(new File(sshDir, mPrivateKeyFilename), "private_key_filename");

        mEntityManager = EntityManager.sharedInstance();

        RemoteEntityInfo markerEntity = mEntityManager.entity(mEntityId);
        if (markerEntity == null)
          badState("no such cow_marker_entity_id found:", mEntityId);

        markerEntity = Ngrok.sharedInstance().addNgrokInfo(markerEntity, true);

        String username = markerEntity.user();
        String host = markerEntity.url();
        int port = markerEntity.port();

        // The key pair must be generated with this command:
        //
        //   ssh-keygen -m PEM
        //
        // For example, this is how I generated a key on my Macbook:
        //
        /**
         * <pre>
         * 
           .ssh] ssh-keygen -m PEM
            Generating public/private rsa key pair.
            Enter file in which to save the key (/Users/home/.ssh/id_rsa): eiodashboard
            Enter passphrase (empty for no passphrase): 
            Enter same passphrase again: 
            Your identification has been saved in eiodashboard.
            Your public key has been saved in eiodashboard.pub.
         * 
         * </pre>
         */

        jsch.addIdentity(privateKey.toString());
        Session session = jsch.getSession(username, host, port);

        session.setConfig("StrictHostKeyChecking", "no");

        todo("add some optional timing info");
        log("connecting");
        if (verbose())
          checkpoint("connecting");
        session.connect();
        if (verbose())
          checkpoint("connected");
        log("connected");
        mSession = session;
      } catch (Throwable e) {
        String msg = e.getMessage();
        if (msg.contains("invalid privatekey")) {
          pr("*** Failed to add identity to JSch; was the key created via 'ssh-keygen -m PEM' ?");
        }
        throw asRuntimeException(e);
      }
    }
    return mSession;
  }

  private ChannelSftp channelSftp() {
    if (mChannelSftp == null) {
      try {
        mChannelSftp = (ChannelSftp) session().openChannel("sftp");
        // Set a 5 second timeout
        mChannelSftp.connect((int) DateTimeTools.SECONDS(5));
      } catch (JSchException e) {
        throw Files.asFileException(e);
      }
    }
    return mChannelSftp;
  }

  private File homeDirectory() {
    if (mHomeDirectory == null)
      mHomeDirectory = Files.homeDirectory();
    return mHomeDirectory;
  }

  private File mHomeDirectory;
  private EntityManager mEntityManager;
  private Session mSession;
  private ChannelSftp mChannelSftp;
  private String mPrivateKeyFilename = "id_rsa";
  private String mEntityId;

}
