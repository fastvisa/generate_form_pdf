FROM maven:3.8.5-jdk-11-slim

COPY entrypoint.sh ./entrypoint.sh
RUN apt-get update \
    && apt get install apt-utils \
    && apt-get install dos2unix \
    && dos2unix ./entrypoint.sh \
    && chmod +x ./entrypoint.sh

WORKDIR /app
ENTRYPOINT ["./entrypoint.sh"]
CMD ["bash"]