(ns tailrecursion.boot.task-test
  (:refer-clojure :exclude [sync])
  (:require
    [clojure.test :refer :all] 
    [tailrecursion.boot.task :refer :all]))

(defn main [boot]
  (fn [continue]
    (fn [event]
      (println "ok got here")
      (continue event))))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
