#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.5.0"

(apply set-env! (read-string (slurp "config.edn")))
(add-sync! (get-env :out-path) (get-env :rsc-paths))

(require
  '[tailrecursion.boot.task        :refer [dep-tree install jar uberwar war]]
  '[tailrecursion.boot.task.notify :refer [hear]] )

(deftask develop []
  "rebuild and reinstall the library to ~/m2."
  (comp (watch) (hear) (jar) (install)) )
