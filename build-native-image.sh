#bin/sh

#Remove older versions
rm -rf native
rm -rf lambda-native.zip

#Build the native image via Docker
docker build . -t graalvm-lambda-builder --progress=plain --no-cache

#Extract the resulting native image
docker run --rm --entrypoint cat graalvm-lambda-builder ./target/native > native

chmod 775 native
chmod 775 bootstrap

zip lambda-native native bootstrap

aws s3 cp lambda-native.zip s3://lambda-upload-testing/artifacts/

aws lambda update-function-code --function-name s3-put-http-testing-function --s3-bucket lambda-upload-testing --s3-key artifacts/lambda-native.zip