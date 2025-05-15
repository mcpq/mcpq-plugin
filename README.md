# Minecraft Protobuf Queries (MCPQ) Server Plugin

This Minecraft Java server plugin is designed to allow other *client libraries* to control the server it is running on. 
These client libraries can be written in other programming languages thus allowing for interoperability between the Java plugin ecosystem and other languages.
The plugin is design to be run on [Paper](https://papermc.io/) Minecraft servers (alternatively [Spigot](https://www.spigotmc.org/) servers).

This plugin is heavily inspired by [RaspberryJuice](https://github.com/zhuowei/RaspberryJuice) (and its client library [MCPI](https://github.com/martinohanlon/mcpi)) and attempts a more modern approach for communication between server and client that also works for more modern versions of Minecraft.

This plugin uses [Protocol Buffers](https://github.com/mcpq/mcpq-proto) and the [gRPC](https://grpc.io/) library and protocols to communicate with clients written in other programming languages and is itself written in [Kotlin](https://kotlinlang.org/).

## Usage

* Download [Paper](https://papermc.io/downloads/paper) or another Minecraft server that implements its API (alternatively [Spigot](https://www.spigotmc.org/))
* Download the compiled and bundled jar depending on the [table in versions](#versions) (but probably [latest](https://github.com/mcpq/mcpq-plugin/releases/latest))
* Put the `mcpq-<version>.jar` plugin in the server's `plugins` folder:
  ```
  server
  │   paper-<mcversion>.jar
  └───plugins
      │   mcpq-<version>.jar
      │   ...
  ```
* Choose and install a supported [client library](#client-libraries), for example, [Python](https://github.com/mcpq/mcpq-python)
* You're done!

## Versions

| Major (Protocol) | Minor (Plugin) |     Paper     |    Spigot     | Minecraft Compatible |                       Release (Plugin Download)                       |
|-----------------:|:---------------|:-------------:|:-------------:|:--------------------:|:---------------------------------------------------------------------:|
|                1 | 0              |       ✅       |       ✅       |   1.18.2 - 1.21.4    | [mcpq-1.0.jar](https://github.com/mcpq/mcpq-plugin/releases/tag/v1.0) |
|                2 | 0              | ✅<sup>1</sup> | ✅<sup>1</sup> |       1.20.1+        | [mcpq-2.0.jar](https://github.com/mcpq/mcpq-plugin/releases/tag/v2.0) |
|                2 | 1              |       ✅       |       ❌       |       1.20.1+        | [mcpq-2.1.jar](https://github.com/mcpq/mcpq-plugin/releases/tag/v2.1) |

> Version Notes: <br>
> <sup>1</sup>: Limited output for blocking commands

* The plugin's *major version* reflects the version number of the [protobuf protocol version](https://github.com/mcpq/mcpq-proto) and thus which types of communication is possible with the server.
* The *minor version* is incremented with patches and additional functionality of this plugin.
* The plugin is compatible with certain *Minecraft versions* depending on the *type and version of API the plugin uses*.
  * E.g. the plugin `mcpq-2.0.jar` would require Minecraft Version 1.20.1 or newer for both Paper and Spigot (Spigot API)

#### Paper and Spigot Compatibility

TLDR; if you use Paper, then use a plugin that is build against Paper API (not Spigot compatible)

Originally the plugin was built against the *Spigot/Bukkit API*, which meant that [Spigot](https://www.spigotmc.org/) as well as all forks,
like [Paper](https://papermc.io/), were automatically supported.
However, [Paper decided to hard fork Spigot](https://forums.papermc.io/threads/the-future-of-paper-hard-fork.1451/),
which means that starting from Minecraft version 1.21.4 and onwards Paper will no longer be a fork of Spigot
and thus the Paper API will slowly diverge from the Spigot/Bukkit API.

For us this means the following:
* The [main branch](https://github.com/mcpq/mcpq-plugin/tree/main) will be built against the Paper API and *will no longer be compatible with Spigot*!
* A separate [main-spigot branch](https://github.com/mcpq/mcpq-plugin/tree/main-spigot) will be maintained for Spigot compatibility.
  * Provisionally, support for Spigot will remain in place for major releases and essential bug fixes.

This setup allows us to use the more powerful Paper API for plugin versions that are built against it,
so if you are using Paper use a plugin that is build against that API instead!

#### Protobuf Compatibility

TLDR; download the newest version of the plugin that supports your Minecraft version and your server (see table above)
and choose a client version that supports that version (or older)

A plugin *should* be compatible with *older* client versions as backwards compatibility should be kept, which
is enabled by the protobuf protocol. (Breaking changes of the protocol are recorded [here](https://github.com/mcpq/mcpq-proto?tab=readme-ov-file#version-changes))

> E.g., a *client* build against protocol v1 should also be able to communicate wtih a *plugin* built against protocol v2

However, using *newer client* versions with older plugin versions will very likely *not* work and is not supported.

> E.g., a *client* build against protocol v2 *is not* compatible with a *plugin* built against protocol v1

## Client Libraries

A client library allows for communication with the server plugin.
Due to its design [gRPC](https://grpc.io/) allows for implementations in a [large number of programming languages](https://grpc.io/docs/languages/).

The following official client implementations exist:

* Python: [mcpq-python](https://github.com/mcpq/mcpq-python)

> If you implement your own client for the [protobuf](https://github.com/mcpq/mcpq-proto) interface, please let me know so that I can add a link to this list!

## Configuration

The plugin comes with a config file that will be generated/read from `plugins/mcpq/config.yml` in which the following can be configured:

* host: localhost - hostname or ip address from which connections to the plugin are accepted. The default `localhost` does only allow connections from the same device the server is running on, while `0.0.0.0` would allow connections from anywhere.
* port: 1789 - the port on which a MCPQ client can connect to the plugin
* debug: false - whether to print additional debug information especially for gRPC

## Build Instructions

Before building make sure to download the correct version of the [protobuf protocol version](https://github.com/mcpq/mcpq-proto),
which is integrated at `src/main/proto` as a git submodule, using one of the two methods below.
You can checkout the version of the protocol that you want to build for.

When cloning the repository, either clone it with:

```bash
# clone the repository and all its submodules
git clone --recurse-submodules https://github.com/mcpq/mcpq-plugin
```

or, if you already cloned the repository:

```bash
# update and clone into the repository all submodules
git submodule update --init --recursive
```

To build the plugin use `./gradlew build` or use the `build` target with the gradle plugin for *IntelliJ IDEA* (Community), which I also recommend for development.

The final plugin with all dependencies included will be a bundled `jar` at `build/libs/mcpq-<version>.jar`.

## License

[LGPLv3](LICENSE)

> Note: The *intent* behind the chosen license, is to allow the licensed software to be *used* (as is) in any type of project, even commercial or closed-source ones.
> However, changes or modifications *to the licensed software itself* must be shared via the same license openly.
> Checkout [this blog](https://fossa.com/blog/open-source-software-licenses-101-lgpl-license/) for an in-depth explanation.
