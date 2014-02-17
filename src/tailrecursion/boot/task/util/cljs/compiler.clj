(ns tailrecursion.boot.task.util.cljs.compiler
  (:refer-clojure :exclude [compile])
  (:require 
    [tailrecursion.boot.file  :as f]
    [cljs.env                 :as env]
    [cljs.closure             :as cljs]
    [clojure.string           :refer [split join blank?]]
    [clojure.pprint           :refer [pprint]]
    [clojure.java.io          :refer [input-stream output-stream file
                                      delete-file make-parents]]))

(defrecord CljsSourcePaths [paths]
  cljs/Compilable
  (-compile [this opts]
    (mapcat #(cljs/-compile % opts) paths)))

(def cljs-env (atom {}))

(defn compile
  [src-paths flib-out lib-out ext-out inc-out {:keys [output-to] :as opts}]
  (assert output-to "No :output-to option specified.")
  (let [files #(filter (fn [x] (.isFile x)) (file-seq %))
        paths #(mapv (fn [x] (.getPath x)) (files %))
        cat   #(join "\n" (mapv slurp %)) 
        srcs  (CljsSourcePaths. (filter #(.exists (file %)) src-paths))
        exts  (paths ext-out)
        incs  (cat (sort (files inc-out)))]
    (env/with-compiler-env cljs-env
      (cljs/build srcs (update-in opts [:externs] into exts)))
    (spit output-to (str incs "\n" (slurp output-to)))))
