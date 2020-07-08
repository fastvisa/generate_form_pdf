package com.fastvisa.services;

import java.io.File;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AwsS3Service {
  private final AmazonS3 s3Client;

  public AwsS3Service(String accessKey, String secretKey) {
    Regions clientRegion = Regions.AP_SOUTHEAST_1;
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    
    AmazonS3 client = AmazonS3ClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withRegion(clientRegion)
      .build();

    this.s3Client = client;
  }

  public void postObject(String bucketName, String key, File file) {
    PutObjectRequest request = new PutObjectRequest(bucketName, key, file)
      .withCannedAcl(CannedAccessControlList.PublicRead);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType("application/pdf");
    request.setMetadata(metadata);
    s3Client.putObject(request);
  }
  
  public String getUrl(String bucketName, String key) {
    return s3Client.getUrl(bucketName, key).toExternalForm();
  }
}