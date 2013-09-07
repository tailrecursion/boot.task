;;
;; # try it: run the following shell cmd then modify/create/rm files in ./test/
;; $ boot watch debug
;;
{:project tailrecursion/tasktest
 :version "0.1.0-SNAPSHOT"
 :dependencies [[tailrecursion/boot.task "0.1.0-SNAPSHOT"]]
 :directories #{"test"}
 :tasks
 {:watch
  {:main [tailrecursion.boot.task/watch]}
  :cljs
  {:main [tailrecursion.boot.task/cljs]}
  :jar
  {:main [tailrecursion.boot.task/jar]}
  :debug
  {:main [tailrecursion.boot.task/debug]}}}
