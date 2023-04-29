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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import js.webtools.gen.CloudFileEntry;
import js.webtools.gen.S3Params;
import js.base.DateTimeTools;
import js.file.Files;
import js.parsing.RegExp;

public class S3Archive extends ArchiveDevice {

  public S3Archive(S3Params params) {
    mParams = params.build();
    todo("investigate the best practices guide:"
        + " https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/best-practices.html");

    String a = params.bucketName();
    // There are more constraints than this regex captures, but this eliminates a lot of bad cases
    checkArgument(RegExp.patternMatchesString("[a-zA-Z0-9.-]+", //
        a), "illegal bucket name", quote(a));

    a = params.folderPath();
    if (nonEmpty(a))
      checkArgument(RegExp.patternMatchesString("(\\w|-)+(:?\\/(\\w|-)+)*", //
          a), "illegal folder path", quote(a));

    if (nonEmpty(mParams.folderPath()))
      absFolderPathPrefix = mParams.folderPath() + "/";
    else
      absFolderPathPrefix = "";

    updateVerbose();

    if (alert("constructing connection immediately")) {
      s3();
    }
  }

  @Override
  public void setDryRun(boolean dryRun) {
    checkState(mDryRun == null || mDryRun == dryRun, "dry run already initialized");
    mDryRun = dryRun;
  }

  @Override
  public boolean fileExists(String path) {
    String absPath = absPath(path);
    boolean result = s3().doesObjectExist(mParams.bucketName(), absPath);
    log("fileExists; bucket:", mParams.bucketName(), "path:", absPath, "result:", result);
    return result;
  }

  @Override
  public void push(File source, String path) {
    if (isDryRun())
      return;

    if (nullOrEmpty(path))
      path = source.getName();

    String absPath = absPath(path);
    log("push File:", source, "path:", absPath, "bucket:", mParams.bucketName());
    if (writesDisabled()) {
      alert("Not writing any files to S3 (for dev purposes); just delaying a bit");
      DateTimeTools.sleepForRealMs(15000);
      return;
    }
    s3().putObject(mParams.bucketName(), absPath, source);
  }

  @Override
  public void pull(String path, File destination) {
    if (isDryRun())
      return;
    String absPath = absPath(path);
    log("pull File:", destination, "path:", absPath, "bucket:", mParams.bucketName());
    if (destination.isDirectory())
      destination = new File(destination, path);
    log("pulling to directory; destination now:", destination);
    s3().getObject(new GetObjectRequest(mParams.bucketName(), absPath), destination);
  }

  @Override
  public S3Archive withMaxItems(int maxItems) {
    mMaxItems = maxItems;
    return this;
  }

  private Integer mMaxItems;

  @Override
  public List<CloudFileEntry> listFiles(String path) {
    if (isDryRun())
      throw notSupported("not supported in dryrun");
    path = absPath(nullToEmpty(path));
    log("listFiles, path:", path, "params:", INDENT, mParams);
    ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(mParams.bucketName())
        .withPrefix(path);
    if (mMaxItems != null) {
      request.withMaxKeys(mMaxItems);
      mMaxItems = null;
    }

    ListObjectsV2Result result2 = s3().listObjectsV2(request);
    List<S3ObjectSummary> objects = result2.getObjectSummaries();
    List<CloudFileEntry> fileEntryList = arrayList();
    for (S3ObjectSummary os : objects) {
      String key = os.getKey();
      key = chompPrefix(key, mParams.bucketName());
      if (key.isEmpty() || key.endsWith("/"))
        continue;
      fileEntryList.add(CloudFileEntry.newBuilder() //
          .name(key) //
          .size(os.getSize())//
          .build());
    }
    log("number of files:", fileEntryList.size());
    return fileEntryList;
  }

  private boolean isDryRun() {
    if (mDryRun == null)
      setDryRun(false);
    return mDryRun;
  }

  // ---------------------------------

  public void push(byte[] input, String path) {
    if (isDryRun())
      return;
    path = absPath(path);
    log("push", input.length, "bytes, path:", path);
    InputStream stream = new ByteArrayInputStream(input);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(input.length);
    // See https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/
    s3().putObject(mParams.bucketName(), path, stream, metadata);
  }

  private AmazonS3 s3() {
    if (mAws == null) {
      log("attempting to construct AmazonS3 client");
      try {

        String regionName = "us-west-2";
        pr("Calling getRegionMetadata");
        RegionUtils.getRegionMetadata();
        pr("Calling getRegion with", regionName);
        Region region = RegionUtils.getRegion(regionName);
        pr("got region:", region);

        ClientConfiguration cc = new ClientConfiguration();

        if (mParams.connectionTimeoutMs() != 0)
          cc.withConnectionTimeout(mParams.connectionTimeoutMs());
        if (mParams.socketTimeoutMs() != 0)
          cc.withSocketTimeout(mParams.socketTimeoutMs());
        log("parameters:", mParams);

        mAws = AmazonS3ClientBuilder.standard() //
            .withCredentials(new AWSStaticCredentialsProvider(credentials())) //
            // Do we need to explicitly set the same region that the bucket was created with?
            //   http://opensourceforgeeks.blogspot.com/2018/07/how-to-fix-unable-to-find-region-via.html

            .withRegion(regionName) // 
            .withClientConfiguration(cc) //
            .build();
        log("success");
      } catch (Throwable t) {
        alert("Failed to construct AmazonS3Client!");
        throw t;
      }
    }
    updateVerbose();
    return mAws;
  }

  /**
   * Construct an AWSSessionCredentials by parsing the aws_credentials.txt file
   * in the project's secrets directory
   */
  private AWSSessionCredentials credentials() {
    String s = Files.readString(Files.S.fileWithinSecrets("aws_credentials.txt"));
    List<String> rows = split(s, '\n');
    int i = rows.indexOf("[" + mParams.profile() + "]");
    checkArgument(i >= 0, "can't find profile", mParams.profile(), "in aws_credentials.txt");
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

  private String absPath(String relativePath) {
    checkArgument(!relativePath.startsWith("/"));
    return absFolderPathPrefix + relativePath;
  }

  private final S3Params mParams;
  private final String absFolderPathPrefix;
  private Boolean mDryRun;
  private AmazonS3 mAws;
}
