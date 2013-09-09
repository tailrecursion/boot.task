;;
;; # try it: run the following shell cmd then modify/create/rm files in ./test/
;; $ boot watch debug
;;
{:project tailrecursion/tasktest
 :version "0.1.0-SNAPSHOT"
 :dependencies [[tailrecursion/boot.task "0.1.0-SNAPSHOT"]]
 :require-tasks [[tailrecursion.boot.task :as x]]
 :src-paths #{"test"}
 :tasks
 {:heyho
  {:doc "testing 1 2 3..."
   :dependencies [[alandipert/enduro "1.1.2"]]}
  }}
