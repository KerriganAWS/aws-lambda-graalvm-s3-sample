#bin/sh

#Remove older versions
rm -rf native
rm -rf lambda-native.zip
rm -rf aws-large-request-dispatcher-1.0.0-SNAPSHOT

./mvnw clean package
native-image -jar ./target/aws-large-request-dispatcher-1.0.0-SNAPSHOT.jar --verbose --no-fallback --enable-url-protocols=http

cp ./aws-large-request-dispatcher-1.0.0-SNAPSHOT ./native

chmod 775 native
chmod 775 bootstrap

zip lambda-native native bootstrap

aws s3 cp lambda-native.zip s3://lambda-upload-testing/artifacts/

aws lambda update-function-code --function-name s3-put-http-testing-function --s3-bucket lambda-upload-testing --s3-key artifacts/lambda-native.zip