# Play Remote Configuration


[![Latest release](https://img.shields.io/badge/latest_release-18.04-orange.svg)](https://github.com/play-rconf/play-rconf/releases)
[![JitPack](https://jitpack.io/v/play-rconf/play-rconf.svg)](https://jitpack.io/#play-rconf/play-rconf-http)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/play-rconf/play-rconf/master/LICENSE)

Remote configuration for Play Framework
*****

## About this project
In production, it is not always easy to manage the configuration files of a
Play Framework application, especially when it running on multiple servers.
The purpose of this project is to provide a simple way to use a remote
configuration with a Play Framework application.



## Available providers

| Provider    | Classpath                          | Repository                 |
|-------------|------------------------------------|----------------------------|
| CoreOS etcd | io.playrconf.provider.EtcdProvider | [play-rconf/play-rconf-etcd](https://github.com/play-rconf/play-rconf-etcd) |
| HTTP        | io.playrconf.provider.HttpProvider | [play-rconf/play-rconf-http](https://github.com/play-rconf/play-rconf-http) |



## Add play-rconf to your project

#### build.sbt

```sbtshell
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.play-rconf" %% "play-rconf" % "release~YY.MM"
```



## How to use

You can use this project in two ways. The first is to simply set the right
application loader in your configuration file. The second, if you have an
existing application loader, is to extend it with the class provided in this
project : _io.playrconf.ApplicationLoaderJava_ or _io.playrconf.ApplicationLoaderScala_.


#### application.conf (first way)

```hocon
play {
  application {

    ## Application Loader
    # https://www.playframework.com/documentation/latest/JavaDependencyInjection
    # https://www.playframework.com/documentation/latest/ScalaDependencyInjection
    # ~~~~~
    loader = "io.playrconf.ApplicationLoaderJava"
    #loader = "io.playrconf.ApplicationLoaderScala"
  }
}
```


#### ApplicationLoader.java (second way)

```java
import io.playrconf.ApplicationLoaderJava;

public class ApplicationLoader extends ApplicationLoaderJava {

    @Override
    public GuiceApplicationBuilder builder(final Context context) {
        final GuiceApplicationBuilder newInitialBuilder = super.builder(context);
        // Your custom code
        return newInitialBuilder;
    }
}
```



## Configuration

```hocon
## Remote configuration
# ~~~~~
# Allows usage of remote configuration. Configuration is fetched at
# the application start and merged into the the local configuration
# file
remote-configuration {

  # Providers to use. If you specify more than one, they will all
  # be used one after the other. The variable could be a single
  # string or a list of string containing the classpath of all
  # providers to use.
  providers = [
  ]
  providers = ${?REMOTECONF_PROVIDERS}
}
```



## License
This project is released under terms of the [MIT license](https://raw.githubusercontent.com/play-rconf/play-rconf/master/LICENSE).