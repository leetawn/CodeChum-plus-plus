FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y gcc g++ expect && rm -rf /var/lib/apt/lists/*

WORKDIR /code