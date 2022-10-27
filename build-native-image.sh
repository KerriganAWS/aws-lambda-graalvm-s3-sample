#!/bin/bash

./mvnw clean install -P native-image

aws s3 cp lambda-native.zip s3://lambda-upload-testing/artifacts/

aws lambda update-function-code --function-name s3-put-http-testing-function --s3-bucket lambda-upload-testing --s3-key artifacts/lambda-native.zip