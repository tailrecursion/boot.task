#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.4.0"

(def config (read-string (slurp "config.edn")))
(apply set-env! (:prod config))

(require
  '[tailrecursion.boot.task         :refer [dep-tree install jar war uberwar]] )

(deftask with-profile
  "Setup build for the given profile from `config.edn`."
  [profile]
  (apply set-env! (get config profile))
  (add-sync! (get-env :out-path) (get-env :rsc-paths))
  identity )