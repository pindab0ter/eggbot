FROM openjdk:11.0.10-jre-slim
RUN apt-get update && apt-get install -y --no-install-recommends openjfx 'libgtk2.0-0' 'libxtst6' && rm -rf /var/lib/apt/lists/* && mkdir /opt/EggBot
WORKDIR /opt/EggBot
COPY build/install/EggBot ./
CMD ["bin/EggBot"]