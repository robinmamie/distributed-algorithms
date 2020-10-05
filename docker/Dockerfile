FROM ubuntu:18.04

# Update packages and setup timezone
RUN apt-get update && apt-get -y upgrade && \
      apt-get -y install tzdata

ENV TZ=Europe/Zurich
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
      echo $TZ > /etc/timezone
RUN dpkg-reconfigure --frontend=noninteractive tzdata


RUN apt-get -y install file unzip zip xz-utils git \
                         gcc g++ cmake default-jdk \
                         python3 sudo vim

ADD docker/apache-maven-3.6.3-bin.tar.gz /usr/local
RUN ln -s /usr/local/apache-maven-3.6.3/bin/mvn /usr/local/bin
COPY src /root/src
ADD barrier.py /root
ADD validate.py /root
ADD finishedSignal.py /root

RUN /root/src/build.sh
