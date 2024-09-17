# Minecraft Protobuf Queries (MCPQ) Server Plugin

This Minecraft Java server plugin is designed to allow other *client libraries* to control the server it is running on. 
These client libraries can be written in other programming languages thus allowing for interoperability between the Java plugin ecosystem and other languages.
The plugin is design to be run on [Spigot](https://www.spigotmc.org/) or [Paper](https://papermc.io/) Minecraft servers.

This plugin is heavily inspired by [RaspberryJuice](https://github.com/zhuowei/RaspberryJuice) (and its client library [MCPI](https://github.com/martinohanlon/mcpi)) and attempts a more modern approach for communication between server and client that also works for more modern versions of Minecraft.

This plugin uses [Protocol Buffers](https://github.com/mcpq/mcpq-proto) and the [gRPC](https://grpc.io/) library and protocols to communicate with clients written in other programming languages and is itself written in [Kotlin](https://kotlinlang.org/).

## Versions

You can download the compiled and bundled jars in the [release](https://github.com/mcpq/mcpq-plugin/releases) section of the repository and put it into your server's `plugins` folder:

```
server
│   paper/spigot-<version>.jar
└───plugins
    │   mcpq-<version>.jar
    │   ...
```

The plugin's major version reflects the version number of the [protobuf protocol version](https://github.com/mcpq/mcpq-proto) and thus which types of communication is possible with the server.
The minor version is incremented with patches and additional functionality of this plugin.
The plugin is compatible with certain **Minecraft versions** depending on the version of the Bukkit API the plugin uses, this may change with time, so checkout the table below for compatibility:

| Major (Protocol) | Minor (Plugin) | Minecraft Compatible |                       Release (Plugin Download)                       |
|-----------------:|:---------------|:--------------------:|:---------------------------------------------------------------------:|
|                1 | 0              |       1.18.2+        | [mcpq-1.0.jar](https://github.com/mcpq/mcpq-plugin/releases/tag/v1.0) |

> E.g. the plugin mcpq-1.0.jar would require Minecraft Version 1.18.2 or newer

Usually the plugin can be used with *newer Minecraft versions* with only minor limitations in functionality at most.
Additionally, the plugin *should* be compatible with *older* client versions as backwards compatibility should be kept (which is also supported by the protocol buffers).
However, using *newer client* versions with older plugin versions will most likely only work over *minor versions* if that.

TLDR; download the newest version of the plugin that supports your Minecraft version (see table above)

## Client Libraries

A client library allows for communication with the server plugin.
Due to its design [gRPC](https://grpc.io/) allows for implementations in a [large number of programming languages](https://grpc.io/docs/languages/).

The following client implementations exist for the following languages:

* Python: [mcpq-python](https://github.com/mcpq/mcpq-python)

> If you implement your own client for the [protobuf](https://github.com/mcpq/mcpq-proto) interface, please let me know so that I can add a link to this list!

## Configuration

The plugin comes with a config file that will be generated/read from `plugins/MCPQ/config.yml` in which the following can be configured:

* host: localhost - hostname or ip address from which connections to the plugin are accepted. The default `localhost` does only allow connections from the same device the server is running on, while `0.0.0.0` would allow connections from anywhere.
* port: 1789 - the port on which a MCPQ client can connect to the plugin
* debug: false - whether or not to print additional debug information especially for gRPC

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

To build the plugin use `./gradlew shadowJar` or use the `shadowJar` target with the gradle plugin for *IntelliJ IDEA* (Community), which I also recommend for development.

The final plugin with all dependencies included will be a bundled `jar` at `build/libs/mcpq-<version>-all.jar`.

## License

[LGPLv3](LICENSE)

> Note: The *intent* behind the chosen license, is to allow the licensed software to be *used* (as is) in any type of project, even commercial or closed-source ones.
> However, changes or modifications *to the licensed software itself* must be shared via the same license openly.
> Checkout [this blog](https://fossa.com/blog/open-source-software-licenses-101-lgpl-license/) for an in-depth explanation.
