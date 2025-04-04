package com.phegondev.PhegonHotel.service;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.phegondev.PhegonHotel.exception.OurException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class AwsS3Service {

  @Value("${aws.s3.bucket.name}")
  private String bucketName;

  @Value("${aws.s3.access.key}")
  private String awsS3AccessKey;

  @Value("${aws.s3.secret.key}")
  private String awsS3SecretKey;

  @Value("${aws.region.static}")
  private String region;

  public String saveImageToS3(MultipartFile photo) {
    try {
      String s3Filename = photo.getOriginalFilename();

      BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsS3AccessKey, awsS3SecretKey);
      AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
              .withRegion(region)
              .build();

      InputStream inputStream = photo.getInputStream();

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("image/jpeg");
      metadata.setContentLength(photo.getSize()); // Add content length

      PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Filename, inputStream, metadata);
      s3Client.putObject(putObjectRequest);

      // Correct URL format with region
      return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + s3Filename;
    } catch (Exception e) {
      throw new OurException("Unable to upload image to s3 bucket: " + e.getMessage());
    }
  }
}

















