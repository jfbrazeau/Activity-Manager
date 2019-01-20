FROM openjdk:8u181-jdk-alpine
ARG version
WORKDIR /app
RUN apk add ttf-dejavu
RUN apk add libc6-compat
ADD target/products/activitymgr-web-server-$version-linux.gtk.x86_64.tar.gz ./
CMD [ "/app/activitymgr-web-server/activitymgr-web-server" ]