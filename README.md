# Reactual

Reactual is a low-level library that provides [signal/slot] and
[functional reactive programming]-like primitives. It can serve as the basis for a user interface
toolkit, or any other library that has a model on which clients will listen and to which they will
react.

* [API docs](http://samskivert.github.com/reactual/api/reactual/package.html) are available.

## Building

The library is built using [SBT] or [Maven].

Invoke `sbt publish-local` to build and install the library to your local Ivy repository (i.e.
`~/.ivy2/local`).

Invoke `mvn install` to build and install the library to your local Maven repository (i.e.
`~/.m2/repository`).

## Artifacts

Reactual is published to Maven Central. To add a Reactual dependency to an SBT or Maven project,
add a dependency on `com.samskivert:reactual:1.0`. Reactual is not published as a cross-compiled
artifact; a given Reactual release is built for a particular major Scala release.

  * reactual 1.0 - scala 2.10

If you prefer to download a pre-built binary, that can be found here:

* [reactual-1.0.jar](http://repo2.maven.org/maven2/com/samskivert/reactual/1.0/reactual-1.0.jar)

Distribution
------------

Reactual is released under the New BSD License. The most recent version of the library is available
at http://github.com/samskivert/reactual

[signal/slot]: http://en.wikipedia.org/wiki/Signals_and_slots
[functional reactive programming]: http://en.wikipedia.org/wiki/Functional_reactive_programming
[SBT]: http://github.com/harrah/xsbt/wiki/Setup
[Maven]: http://maven.apache.org/
