# boot.task

Various generally useful build tasks.

| Task  | Description                                                         |
|-------|---------------------------------------------------------------------|
| env   | Print the boot configuration.                                       |
| debug | Print the event map.                                                |
| watch | Monitor source files for changes and rebuild when necessary.        |
| sync  | Sync directories, similar to `rsync --delete`.                      |
| cljs  | Compile ClojureScript source files.                                 |
| jar   | Create a jar file from project.                                     |
| repl  | Start a repl in project.                                            |

For more info do `boot help <task>`.

### Example

```clojure
;; boot.edn
{:project ...
 :version ...
 :dependencies [[tailrecursion/boot.task "0.1.2"] ...]
 :require-tasks #{[tailrecursion.boot.task :refer :all] ...}
 ...
 ...}
```

### Dependency

Artifacts are published on [Clojars][1]. This version is compatible with
[boot][2] version [0.1.0][3] or later.

```clojure
[tailrecursion/boot.task "0.1.2"]
```

```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>boot.task</artifactId>
  <version>0.1.2</version>
</dependency>
```

## License

Copyright © 2013 Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure.

[1]: https://clojars.org/tailrecursion/boot.task
[2]: https://github.com/tailrecursion/boot
[3]: https://github.com/tailrecursion/boot/tree/0.1.0

