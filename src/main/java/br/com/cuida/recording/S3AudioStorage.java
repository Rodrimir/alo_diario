package br.com.cuida.recording;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Armazenamento em S3 (AWS) ou compatível (MinIO, com path-style). */
class S3AudioStorage implements AudioStorage, AutoCloseable {

  private final S3Client client;
  private final String bucket;

  S3AudioStorage(StorageProperties.S3 properties) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(properties.region()))
            .forcePathStyle(properties.pathStyle());
    if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
      builder.endpointOverride(URI.create(properties.endpoint()));
    }
    if (properties.accessKey() != null && !properties.accessKey().isBlank()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
    }
    this.client = builder.build();
    this.bucket = properties.bucket();
  }

  @Override
  public void put(String key, byte[] content, String contentType) {
    client.putObject(
        request -> request.bucket(bucket).key(key).contentType(contentType),
        RequestBody.fromBytes(content));
  }

  @Override
  public Optional<AudioContent> get(String key) {
    try {
      ResponseInputStream<GetObjectResponse> stream =
          client.getObject(request -> request.bucket(bucket).key(key));
      GetObjectResponse response = stream.response();
      return Optional.of(
          new AudioContent(stream, response.contentLength(), response.contentType()));
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(String key) {
    client.deleteObject(request -> request.bucket(bucket).key(key));
  }

  @Override
  public int deletePrefix(String prefix) {
    int deleted = 0;
    String token = null;
    do {
      String continuation = token;
      ListObjectsV2Response page =
          client.listObjectsV2(
              request -> request.bucket(bucket).prefix(prefix).continuationToken(continuation));
      List<ObjectIdentifier> keys =
          page.contents().stream()
              .map(S3Object::key)
              .map(key -> ObjectIdentifier.builder().key(key).build())
              .toList();
      if (!keys.isEmpty()) {
        client.deleteObjects(
            request -> request.bucket(bucket).delete(Delete.builder().objects(keys).build()));
        deleted += keys.size();
      }
      token = page.isTruncated() ? page.nextContinuationToken() : null;
    } while (token != null);
    return deleted;
  }

  @Override
  public void close() {
    client.close();
  }
}
