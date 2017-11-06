FROM openjdk:8

RUN mkdir /ffmpeg

RUN wget -O /tmp/ffmpeg.tar.xz https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-64bit-static.tar.xz
#RUN ["/bin/bash", "if [ ! -d /ffmpeg ] ; then mkdir -p /ffmpeg; fi"]
RUN tar xvf /tmp/ffmpeg.tar.xz -C /ffmpeg
#CMD if [[ ! -f /usr/bin/ffmpeg ]] ; then ln -sf /opt/ffmpeg/ffmpeg-3.4-64bit-static/ffmpeg /usr/bin/ffmpeg; fi
#CMD if [[ ! -f /usr/bin/ffprobe ]] ; then ln -sf /opt/ffmpeg/ffmpeg-3.4-64bit-static/ffprobe /usr/bin/ffprobe; fi

RUN mkdir /app
ADD tsdbot-1.0-SNAPSHOT.jar /app/tsdbot.jar
ADD prod.yml /app/prod.yml
CMD java -jar /app/tsdbot.jar server /app/prod.yml
EXPOSE 8080