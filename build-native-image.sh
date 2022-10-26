#bin/sh

#Remove older versions
rm -rf native
rm -rf lambda-native.zip

#Build the native image via Docker
docker build . -t graalvm-lambda-builder --progress=plain --no-cache

#Extract the resulting native image
docker run --rm --entrypoint cat graalvm-lambda-builder ./target/native > native

zip lambda-native native bootstrap
