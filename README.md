# scala-progress-bar

A simple console progress bar for Scala, with few dependencies.

## Installation

Currently it's not yet on maven, but it can be referenced from github using `sbt`.

```sbtshell
lazy val root =
  (project in file("."))
    .dependsOn(simpleProgressBar)
    ...

lazy val simpleProgressBar = RootProject(uri("git://github.com/morgen-peschke/scala-progress-bar.git#v1.1.0"))
```

## Usage

```scala
import peschke.console.progressbar.ProgressBar

val progressBar = new ProgressBar(initialCount = 0, totalCount = 100)

progressBar.incrementCount()
progressBar.incrementCount(9)
progressBar.incrementCount(40)
progressBar.incrementCount(5)

progressBar.incrementCount(50)
progressBar.setCount(95)
progressBar.complete()
```

## Note to OSX users

If you are writing a console utility, consider adding this to your shell config files:

```bash
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dapple.awt.UIElement=true"
```

This will prevent OSX from opening an empty Java instance on the dock, which steals focus and switches the active Space, 
and can be very annoying.
