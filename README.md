# EggBot
A Discord bot for Egg, Inc. co-op communities.

## How to run

1. Install the dependencies listed below
2. Launch with the following environment variables:
      * `bot_id=<Discord Bot ID>`
      * `owner_id=<Bot owner's Discord user ID>`

## Dependencies

### Deployment

* Java Runtime Environment 8
  * macOS Homebrew: `brew install java8`
  * Linux Aptitude: `apt-get install openjdk-8-jre-headless`
  * Windows: `choco install jre8` 

### Development

* Protocol Buffers
  * macOS Homebrew: `brew install protobuf`
  * Linux Aptitude: `apt-get install protobuf-compiler` 
  * Windows Chocolatey: `choco install protoc`
* SQLite
  * macOS: `brew install sqlite`
  * Linux Aptitude: `apt-get install sqlite`
  * Windows: `choco install sqlite`