FROM public.ecr.aws/amazonlinux/amazonlinux:2

RUN yum -y update \
  && yum install -y unzip tar gzip bzip2-devel ed gcc gcc-c++ gcc-gfortran \
  less libcurl-devel openssl openssl-devel readline-devel xz-devel \
  zlib-devel glibc-static libcxx libcxx-devel llvm-toolset-7 zlib-static \
  && rm -rf /var/cache/yum

# Graal VM
ENV JAVA_VERSION java11
ENV GRAAL_VERSION 22.2.0
ENV GRAAL_FOLDERNAME graalvm-ce-${JAVA_VERSION}-${GRAAL_VERSION}
ENV GRAAL_FILENAME graalvm-ce-${JAVA_VERSION}-linux-amd64-${GRAAL_VERSION}.tar.gz
RUN curl -4 -L https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAAL_VERSION}/${GRAAL_FILENAME} | tar -xvz
RUN mv $GRAAL_FOLDERNAME /usr/lib/graalvm
RUN rm -rf $GRAAL_FOLDERNAME

# Maven
ENV MVN_VERSION 3.8.6
ENV MVN_FOLDERNAME apache-maven-${MVN_VERSION}
ENV MVN_FILENAME apache-maven-${MVN_VERSION}-bin.tar.gz
RUN curl -4 -L https://dlcdn.apache.org/maven/maven-3/${MVN_VERSION}/binaries/${MVN_FILENAME} | tar -xvz
RUN mv $MVN_FOLDERNAME /usr/lib/maven
RUN rm -rf $MVN_FOLDERNAME

#Native Image dependencies
RUN /usr/lib/graalvm/bin/gu install native-image
RUN ln -s /usr/lib/graalvm/bin/native-image /usr/bin/native-image
RUN ln -s /usr/lib/maven/bin/mvn /usr/bin/mvn

ENV JAVA_HOME /usr/lib/graalvm

#Build Native Image
WORKDIR project
COPY pom.xml pom.xml
RUN mvn verify clean --fail-never -B
COPY src src
RUN mvn -Pnative -DskipTests package