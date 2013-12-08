# boot.task

This project contains a number of generally useful tasks for the [boot][2]
Clojure build tool.

### Dependency

Artifacts are published on [Clojars][1]. This version is compatible with
[boot.core][4] version [1.0.0][3] or later.

```clojure
[tailrecursion/boot.task "1.0.0"]
```

```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>boot.task</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Tasks

| Task  | Description                                                         |
|-------|---------------------------------------------------------------------|
| env   | Print the boot configuration.                                       |
| debug | Print the event map.                                                |
| watch | Monitor source files for changes and rebuild when necessary. This task must come before other tasks. |
| sync  | Sync directories, similar to `rsync --delete`.                      |
| cljs  | Compile ClojureScript source files.                                 |
| jar   | Create a jar file from project.                                     |
| repl  | Start a repl in project.                                            |

For more info about a task do `boot [help <task>]`.

### Usage

```clojure
;; boot.edn
{:project ...
 :version ...
 :dependencies [[tailrecursion/boot.core "1.0.0"]
                [tailrecursion/boot.task "1.0.0"]
                ...]
 :require-tasks #{[tailrecursion.boot.task :refer :all] ...}
 ...
 ...}
```

## License

Copyright © 2013 Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure.

[1]: https://clojars.org/tailrecursion/boot.task
[2]: https://github.com/tailrecursion/boot
[3]: https://github.com/tailrecursion/boot.core/tree/1.0.0
[4]: https://github.com/tailrecursion/boot.core

