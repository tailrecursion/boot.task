(ns tailrecursion.boot.task.util.cljs.compiler
  (:refer-clojure :exclude [compile])
  (:require 
    [cljs.closure             :as cljs]
    [tailrecursion.boot.file  :as f]
    [clojure.string           :refer [split join blank?]]
    [clojure.pprint           :refer [pprint]]
    [clojure.java.io          :refer [input-stream output-stream file
                                      delete-file make-parents]]))

(defrecord CljsSourcePaths [paths]
  cljs/Compilable
  (-compile [this opts]
    (mapcat #(cljs/-compile % opts) paths)))

(defn compile
  [src-paths depjars flib-out lib-out ext-out inc-out {:keys [output-to] :as opts}]
  (println "Compiling ClojureScript...")
  (assert output-to "No :output-to option specified.")
  (let [files #(filter (fn [x] (.isFile x)) (file-seq %))
        paths #(mapv (fn [x] (.getPath x)) (files %))
        cat   #(join "\n" (mapv slurp %)) 
        srcs  (CljsSourcePaths. (filter #(.exists (file %)) src-paths))
        exts  (paths ext-out)
        incs  (cat (sort (files inc-out)))]
    (cljs/build srcs (update-in opts [:externs] into exts))
    (spit output-to (str incs "\n" (slurp output-to)))))
