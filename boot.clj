;;
;; # try it: run the following shell cmd then modify/create/rm files in ./test/
;; $ boot watch debug
;;
{:dependencies [[tailrecursion/boot.task "0.1.0-SNAPSHOT"]]
 :directories #{"test"}
 :tasks
 {:watch
  {:main [tailrecursion.boot.task/watch]}
  :debug
  {:main [tailrecursion.boot.task/debug]}}}
