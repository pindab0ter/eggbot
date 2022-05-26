# EggBot
A Discord bot for Egg, Inc. co-op communities.

## Description
To facilitate teamwork for Egg, Inc. communities this bot creates team rosters out of all registered players for any given active contract, allows checking up on the progress of all teams or a detailed view of an individual team and enables setting goals for individual players. This bot is being used daily by a private community of over 70 people.

It is able to predict whether a team will reach their contract goals by running a simulation on each of the member's latest known game state, taking into account all relevant variables from current amount of chickens and performed research to the time since last backup and the transport and habitat capacity.

<img alt="Co-op info command output example" src="https://user-images.githubusercontent.com/5128166/100468354-8b1bff80-30d4-11eb-845b-b13f977c67c8.jpeg" width=240>

## How to run

1. Install the dependencies listed below
2. Use the supplied `eggbot.example.yaml` and `example.env` in the folder you're running the bot from.
3. Test using `heroku local`

## Deployment dependencies

* Java Runtime Environment 11
* PostgreSQL
* Heroku CLI
  
## Reading guide
A few technically interesting features are highlighted here:

### Applied techniques

* Model View Controller structuring 
* Multithreaded/asynchronous processing of heavy workloads
* Leverage of Java interoperability to combine Java and Kotlin libraries and frameworks
* First deployed on a VPS, later on Heroku
* Multi-tenancy using directory based database sharding

### [Recursive simulation with nested data structures](src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/Simulate.kt)
The [simulation](src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/Simulate.kt#L8-40) calculates each passing minute until either the final goal will be reached within the time limit or one year has passed.
The calculation is performed recursively and stateless; all relevant information is passed in the form of [data classes](src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/Farmer.kt), increasing reliability and separating concerns.
[Co-op states](src/main/kotlin/nl/pindab0ter/eggbot/model/simulation/CoopContractStatus.kt) use Kotlin's sealed classes in order to simplify [the view](src/main/kotlin/nl/pindab0ter/eggbot/view/CoopsInfo.kt#L110-L146) (i.e. table formatting) while still accounting for all possible states by leveraging [Kotlin's ability to use that specific sealed class object’s variables](src/main/kotlin/nl/pindab0ter/eggbot/view/CoopsInfo.kt#L118-L127) as determined by the `when` case.

### [Table generation DSL](src/main/kotlin/nl/pindab0ter/eggbot/model/Table.kt)
In order to produce consistently formatted tables that can be easily iterated upon a custom DSL was made.
The DSL enables [concise, readable and flexible](src/main/kotlin/nl/pindab0ter/eggbot/view/CoopInfo.kt#L90-122) HTML-like code for generating tables.

<img alt="Co-op info command output example" src="https://user-images.githubusercontent.com/5128166/100460892-40948600-30c8-11eb-8760-98395c30ee3a.png" width=480>

### [Asynchronous progress bar](src/main/kotlin/nl/pindab0ter/eggbot/model/ProgressBar.kt)
Since fetching the backup for each player can take a while even when performed asynchronously, [a progress bar is drawn](src/main/kotlin/nl/pindab0ter/eggbot/controller/CoopInfo.kt#L53-L64) to indicate progress of the parallel fetching and simulating of player's farm state.
The [progress bar runs a loop](src/main/kotlin/nl/pindab0ter/eggbot/controller/CoopInfo.kt#L53-L64) that only edits the message once per second and only when a change has occurred.

<img alt="Progress bar example" src="https://user-images.githubusercontent.com/5128166/100465520-c23be200-30cf-11eb-9dba-e39e83e6a0bb.png" width=480>

### [Leveraging Kotlin's extension methods](src/main/kotlin/nl/pindab0ter/eggbot/helpers)
Kotlin offers an incredible flexibility by allowing extension methods for any class. This project uses many of these ‘helper methods’, ordered by which dependency they augment.
As an example: to aid a functional programming style when handling data [these methods](src/main/kotlin/nl/pindab0ter/eggbot/helpers/Kotlin.kt) add the ability to map over cartesian products (`[a, b, c], [1, 2, 3]` → `[a1, b1, c1, a2, b2…]`), interleaving lists (`[a, b, c], [1, 2, 3]` → `[a, 1, b, 2, c, 3]`) and mapping over collections asynchronously.
