package com.aws.dispatcher;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;
import com.amazonaws.xray.handlers.TracingHandler;
import com.amazonaws.xray.AWSXRay;

public class AwsCw1XMLDispatcher
    implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {

  final private AmazonS3 s3 = AmazonS3ClientBuilder.standard()
      .withRequestHandlers(new TracingHandler(AWSXRay.getGlobalRecorder())).build();

  @Override
  public ApplicationLoadBalancerResponseEvent handleRequest(final ApplicationLoadBalancerRequestEvent requestEvent,
      final Context context) {
    final var logger = context.getLogger();
    final var traceId = context.getAwsRequestId();

    final var config = Config._INSTANCE;

    final var response = new ApplicationLoadBalancerResponseEvent();
    try (final var bis = new ByteArrayInputStream(requestEvent.getBody().getBytes())) {
      final var fileName = "cw1-request/" + traceId + ".xml";
      s3.putObject(config.getBucket(), fileName, bis, null);
      logger.log("uploaded to " + fileName);

      final var expiration = new Date();
      expiration.setTime(Instant.now().toEpochMilli() + (4320 * 60 * 1000));

      final var preSignedUrl = s3.generatePresignedUrl(config.getBucket(),
          fileName, expiration);

      final var data = "{\"dataUrl\":\"" + preSignedUrl + "\"}";

      final var queryString = "?" +
          requestEvent.getQueryStringParameters().entrySet().stream()
              .map(e -> String.format("%s=%s", e.getKey(),
                  e.getValue()))
              .collect(Collectors.joining("&"));
      final var targetUrl = config.getTargetUrl() + (queryString.length() == 1 ? ""
          : queryString);
      logger.log("target: " + targetUrl + " ,about to send data " + data);

    } catch (final Exception e) {
      response.setStatusCode(500);
      response.setBody(e.getMessage());
      return response;
    }

    response.setStatusCode(200);
    return response;
  }

  private static class Config {
    private static Config _INSTANCE = new Config();

    private String bucket;
    private String targetUrl;

    public String getBucket() {
      return bucket;
    }

    public String getTargetUrl() {
      return targetUrl;
    }

    private Config() {
      this.bucket = System.getenv("AWS_S3_BUCKET");
      this.targetUrl = System.getenv("TARGET_URL");
    }

  }

  // public static void main(String[] args) {
  // AwsCw1XMLDispatcher dispatcher = new AwsCw1XMLDispatcher();
  // ApplicationLoadBalancerRequestEvent requestEvent = new
  // ApplicationLoadBalancerRequestEvent();
  // requestEvent.setBody("");
  // Context context = new Context() {
  // @Override
  // public String getAwsRequestId() {
  // return "";
  // }

  // @Override
  // public String getLogGroupName() {
  // return "";
  // }

  // @Override
  // public String getLogStreamName() {
  // return "";
  // }

  // @Override
  // public String getFunctionName() {
  // return "";
  // }

  // @Override
  // public String getFunctionVersion() {
  // return "";
  // }

  // @Override
  // public String getInvokedFunctionArn() {
  // return "";
  // }

  // @Override
  // public CognitoIdentity getIdentity() {
  // return null;
  // }

  // @Override
  // public ClientContext getClientContext() {
  // return null;
  // }

  // @Override
  // public int getRemainingTimeInMillis() {
  // return 0;
  // }

  // @Override
  // public int getMemoryLimitInMB() {
  // return 0;
  // }

  // @Override
  // public LambdaLogger getLogger() {
  // return new LambdaLogger() {
  // public void log(String message) {
  // System.out.print(message);
  // }

  // public void log(byte[] message) {
  // try {
  // System.out.write(message);
  // } catch (IOException e) {
  // // NOTE: When actually running on AWS Lambda, an IOException would never
  // happen
  // e.printStackTrace();
  // }
  // }
  // };
  // }

  // };
  // dispatcher.handleRequest(requestEvent, context);
  // }
}
