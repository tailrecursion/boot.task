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

(def cljs-env (env/default-compiler-env))

(defn compile
  [src-paths libs exts prelude {:keys [output-to] :as opts}]
  (assert output-to "No :output-to option specified.")
  (env/with-compiler-env cljs-env
    (cljs/build
      (CljsSourcePaths. (filter #(.exists (file %)) src-paths))
      (-> opts
        (update-in [:externs] into exts)
        (update-in [:libs] into libs))))
  (spit output-to (str (slurp prelude) "\n" (slurp output-to))))
