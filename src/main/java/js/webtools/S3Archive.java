/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import js.webtools.gen.CloudFileEntry;
import js.base.DateTimeTools;
import js.file.Files;
import js.parsing.RegExp;

public class S3Archive extends ArchiveDevice {

  public S3Archive(String profileName, String bucketName, String subfolderPath, File projectDirectoryOrNull) {
    todo(
        "investigate the best practices guide: https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/best-practices.html");
    checkArgument(RegExp.patternMatchesString("^\\w+(?:\\.\\w+)*(?:\\/\\w+(?:\\.\\w+)*)*$", bucketName),
        "bucket name should be of form xxx.yyy/aaa/bbb.ccc");
    mProfileName = profileName;
    mBareBucket = bucketName;

    if (!nullOrEmpty(subfolderPath)) {
      checkArgument(!subfolderPath.endsWith("/"), "unexpected trailing / in subfolderPath");
      mSubfolderPrefix = subfolderPath + "/";
    } else
      mSubfolderPrefix = "";
  }

  @Override
  public void setDryRun(boolean dryRun) {
    checkState(mDryRun == null || mDryRun == dryRun, "dry run already initialized");
    mDryRun = dryRun;
  }

  @Override
  public boolean fileExists(String name) {
    return s3().doesObjectExist(mBareBucket, mSubfolderPrefix + name);
  }

  @Override
  public void push(File source, String name) {
    if (isDryRun())
      return;

    if (nullOrEmpty(name))
      name = source.getName();

    String key = mSubfolderPrefix + name;
    log("push File:", source, "name:", name, "key:", key);
    if (writesDisabled()) {
      alert("Not writing any files to S3 (for dev purposes); just delaying a bit");
      DateTimeTools.sleepForRealMs(15000);
      return;
    }
    s3().putObject(mBareBucket, key, source);
  }

  @Override
  public void pull(String name, File destination) {
    if (isDryRun())
      return;
    if (destination.isDirectory())
      destination = new File(destination, name);

    s3().getObject(new GetObjectRequest(mBareBucket, mSubfolderPrefix + name), destination);
  }

  @Override
  public S3Archive withMaxItems(int maxItems) {
    mMaxItems = maxItems;
    return this;
  }

  private Integer mMaxItems;

  @Override
  public List<CloudFileEntry> listFiles(String prefix) {
    if (isDryRun())
      throw notSupported("not supported in dryrun");

    prefix = mSubfolderPrefix + nullToEmpty(prefix);

    ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(mBareBucket).withPrefix(prefix);
    if (mMaxItems != null) {
      request.withMaxKeys(mMaxItems);
      mMaxItems = null;
    }

    ListObjectsV2Result result2 = s3().listObjectsV2(request);
    List<S3ObjectSummary> objects = result2.getObjectSummaries();
    List<CloudFileEntry> fileEntryList = arrayList();
    for (S3ObjectSummary os : objects) {
      String key = os.getKey();
      key = chompPrefix(key, mSubfolderPrefix);
      if (key.isEmpty() || key.endsWith("/"))
        continue;
      fileEntryList.add(CloudFileEntry.newBuilder() //
          .name(key) //
          .size(os.getSize())//
          .build());
    }
    return fileEntryList;
  }

  private boolean isDryRun() {
    if (mDryRun == null)
      setDryRun(false);
    return mDryRun;
  }

  // ---------------------------------

  public void push(byte[] input, String name) {
    if (isDryRun())
      return;
    String key = mSubfolderPrefix + name;
    InputStream stream = new ByteArrayInputStream(input);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(input.length);
    s3().putObject(mBareBucket, key, stream, metadata);
  }

  private AmazonS3 s3() {
    if (mAws == null) {
      log("attempting to construct AmazonS3 client");
      mAws = AmazonS3ClientBuilder.standard() //
          .withCredentials(new AWSStaticCredentialsProvider(credentials())) //
          .build();
      log("success");
    }
    return mAws;
  }

  /**
   * Construct an AWSSessionCredentials by parsing the aws_credentials.txt file
   * in the project's secrets directory
   */
  private AWSSessionCredentials credentials() {
    String s = Files.readString(Files.S.fileWithinSecrets("aws_credentials.txt"));
    List<String> rows = split(s, '\n');
    int i = rows.indexOf("[" + mProfileName + "]");
    checkArgument(i >= 0, "can't find profile", mProfileName, "in aws_credentials.txt");
    String key = parseArg(rows.get(i + 1));
    String secretKey = parseArg(rows.get(i + 2));
    return new BasicSessionCredentials(key, secretKey, null);
  }

  /**
   * Parse the string following the '=' in e.g.:
   * 
   * aws_access_key_id = AKIA455B5SQPQW66LPSL
   */
  private String parseArg(String arg) {
    int i = arg.indexOf('=');
    checkArgument(i >= 0, "can't parse:", arg);
    return arg.substring(i + 1).trim();
  }

  private final String mProfileName;
  private final String mSubfolderPrefix;
  private final String mBareBucket;
  private Boolean mDryRun;
  private AmazonS3 mAws;
}
