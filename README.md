# EggBot
A Discord bot for Egg, Inc. co-op communities.

## Description
To facilitate teamwork for Egg, Inc. communities this bot creates team rosters out of all registered players for any given active contract, allows checking up on the progress of all teams or a detailed view of an individual team and enables setting goals for individual players. This bot is being used daily by a private community of over 70 people.

It is able to predict whether a team will reach their contract goals by running a simulation on each of the member's latest known game state, taking into account all relevant variables from current amount of chickens and performed research to the time since last backup and the transport and habitat capacity.

<img alt="Co-op info command output example" src="https://user-images.githubusercontent.com/5128166/100468354-8b1bff80-30d4-11eb-845b-b13f977c67c8.jpeg" width=240>

## How to run

1. Install the dependencies listed below
2. Use the supplied `config.properties` in the folder you're running the bot from.

## Dependencies

### Deployment

* Java Runtime Environment 8
  * macOS Homebrew: `brew install java8`
  * Linux Aptitude: `apt-get install openjdk-8-jre-headless`
  * Windows: `choco install jre8` 
* SQLite
  * macOS: `brew install sqlite`
  * Linux Aptitude: `apt-get install sqlite`
  * Windows: `choco install sqlite`

### Development

* Protocol Buffers
  * macOS Homebrew: `brew install protobuf`
  * Linux Aptitude: `apt-get install protobuf-compiler` 
  * Windows Chocolatey: `choco install protoc`
  
## Reading guide
A few technically interesting features are highlighted here:

### Applied techniques

* Model View Controller structuring 
* Multithreaded/asynchronous processing of heavy workloads
* Leverage of Java interoperability to combine Java and Kotlin libraries and frameworks
* Deployed on a cloud hosting service

### [Recursive simulation with nested data structures](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/)
The [simulation](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/Simulate.kt#L42-L72) calculates each passing minute until either the final goal will be reached within the time limit or one year has passed.
The calculation is performed recursively and stateless; all relevant information is passed in the form of [data classes](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/Farmer.kt), increasing reliability and separating concerns.
[Co-op states](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/CoopContractStatus.kt) use Kotlin's strong enum system in order to simplify [the view](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/view/CoopsInfo.kt#L110-L146) (i.e. table formatting) while still accounting for all possible states by leveraging [Kotlin's ability to use that specific Enum instance type's variables](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/view/CoopsInfo.kt#L121-L122) as determined by the `when` case.

### [Table generation DSL](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/model/Table.kt)
In order to produce consistently formatted tables that can be easily iterated upon a custom DSL was made.
The DSL enables [concise, readable and flexible](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/view/CoopInfo.kt#L120-L138) HTML-like code for generating tables.

<img alt="Co-op info command output example" src="https://user-images.githubusercontent.com/5128166/100460892-40948600-30c8-11eb-8760-98395c30ee3a.png" width=480>

### [Asynchronous progress bar](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/model/ProgressBar.kt)
Since fetching the backup for each player can take a while even when performed asynchronously, [a progress bar is drawn](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/controller/CoopInfo.kt#L53-L64) to indicate progress of the parallel fetching and simulating of player's farm state.
The [progress bar runs a loop](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/controller/CoopInfo.kt#L53-L64) that only edits the message once per second and only when a change has occurred.

<img alt="Progress bar example" src="https://user-images.githubusercontent.com/5128166/100465520-c23be200-30cf-11eb-9dba-e39e83e6a0bb.png" width=480>

### Leveraging Kotlin's extension methods
Kotlin offers an incredible flexibility by allowing extension methods for any class. This project uses many of these ‘helper methods’, ordered by which dependency they augment.
As an example: to aid a functional programming style when handling data [these methods](https://github.com/pindab0ter/EggBot/blob/master/src/main/kotlin/nl/pindab0ter/eggbot/helpers/Kotlin.kt#L17-L30) add the ability to map over cartesian products (`[a, b, c], [1, 2, 3]` → `[a1, b1, c1, a2, b2…]`), interleaving lists (`[a, b, c], [1, 2, 3]` → `[a, 1, b, 2, c, 3]`) and mapping over collections asynchronously.
