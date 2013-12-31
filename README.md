# boot.task

This project contains a number of generally useful tasks for the [boot][2]
Clojure build tool.

### Dependency

Artifacts are published on [Clojars][1]. This version is compatible with
[boot.core][4] version [1.0.0][3] or later.

![latest version][5]

### Tasks

| Task          | Description                                                 |
|---------------|-------------------------------------------------------------|
| auto          | Rebuild project in an endless loop (probably not useful–used internally). |
| cljs          | Compile ClojureScript source files.                         |
| debug         | Print the event map.                                        |
| env           | Print the boot configuration.                               |
| jar           | Create a jar file from project.                             |
| lein          | Generate a `project.clj` file based on `boot.edn` and run a leiningen task. |
| rebuild-boot  | Create AOT-compiled boot executable in current dir.         |
| repl          | Start a repl in project.                                    |
| sync          | Sync directories, similar to `rsync --delete`.              |
| watch         | Monitor source files for changes and rebuild when necessary. This task must come before other tasks. |

For more info about a task do `boot [help <task>]`.

### Usage

```clojure
;; boot.edn
{:project ...
 :version ...
 :dependencies [[tailrecursion/boot.core "X.X.X"]
                [tailrecursion/boot.task "X.X.X"]
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
[5]: https://clojars.org/tailrecursion/boot.task/latest-version.svg
