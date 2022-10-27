package com.aws.dispatcher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.time.Instant;
import java.util.Date;
import com.amazonaws.xray.handlers.TracingHandler;
import com.amazonaws.xray.AWSXRay;

public class AwsCw1XMLDispatcher
    implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {

  final private AmazonS3 s3 = AmazonS3ClientBuilder.standard()
      .withRequestHandlers(new TracingHandler(AWSXRay.getGlobalRecorder())).build();

  @Override
  public ApplicationLoadBalancerResponseEvent handleRequest(final ApplicationLoadBalancerRequestEvent requestEvent,
      final Context context) {
    final var traceId = context.getAwsRequestId();
    final var config = Config._INSTANCE;
    final var fileName = "cw1-request/" + traceId + ".xml";
    final var response = new ApplicationLoadBalancerResponseEvent();
    final var expiration = new Date();
    expiration.setTime(Instant.now().toEpochMilli() + (4320 * 60 * 1000));
    final var preSignedUrl = s3.generatePresignedUrl(config.getBucket(), fileName, expiration);
    ObjectMetadata meta = s3.getObjectMetadata(config.getBucket(), "artifacts/lambda-native.zip");
    response.setStatusCode(200);
    response.setBody(preSignedUrl.toString());
    return response;
  }

  private static class Config {
    private static Config _INSTANCE = new Config();

    private String bucket;

    public String getBucket() {
      return bucket;
    }

    private Config() {
      this.bucket = System.getenv("AWS_S3_BUCKET");
    }

  }

}
