# boot.task

This project contains a number of generally useful tasks for the [boot][2]
Clojure build tool.

### Dependency

Artifacts are published on Clojars. This version is compatible with
[boot.core][4] version [2.0.0][3] or later.

[![latest version][5]][1]

### Tasks

| Task        | Description                                                   |
|-------------|---------------------------------------------------------------|
| cljs        | Compile ClojureScript source files.                           |
| install     | Create and place a jar in the local maven repository.         |
| jar         | Create Java Archive distribution from project.                |
| uberwar     | Create Web Application ARchive from project and dependencies. |
| war         | Create Web Application ARchive from project.                  |

For more info about a task do `boot [help <task>]`.

### Usage

```clojure
#!/usr/bin/env boot

;; build.boot file

#tailrecursion.boot.core/version "..."

(set-env!
  :project ...
  :version ...
  :dependencies [[tailrecursion/boot.task "..."] ...]
  ...)

(require '[tailrecursion.boot.task :refer :all] ...)
```

## License

Copyright © 2013 Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure.

[1]: https://clojars.org/tailrecursion/boot.task
[2]: https://github.com/tailrecursion/boot
[3]: https://github.com/tailrecursion/boot.core/tree/2.0.0
[4]: https://github.com/tailrecursion/boot.core
[5]: https://clojars.org/tailrecursion/boot.task/latest-version.svg?bustcache=2
