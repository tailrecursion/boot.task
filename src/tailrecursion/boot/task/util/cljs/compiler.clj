(ns tailrecursion.boot.task.util.cljs.compiler
  (:refer-clojure :exclude [compile])
  (:require 
    [clojure.java.io          :as io]
    [cljs.env                 :as env]
    [cljs.closure             :as cljs]))

(defrecord CljsSourcePaths [paths]
  cljs/Compilable
  (-compile [this opts]
    (mapcat #(cljs/-compile % opts) paths)))

(let [stored-env (atom nil)]
  (defn cljs-env [opts]
    (compare-and-set! stored-env nil (env/default-compiler-env opts))
    @stored-env))

(defn compile
  [src-paths libs exts prelude {:keys [output-to] :as opts}]
  (let [opts (-> opts (update-in [:externs] into exts) (update-in [:libs] into libs))]
    (assert output-to "No :output-to option specified.")
    (binding [env/*compiler* (cljs-env opts)]
      (cljs/build (CljsSourcePaths. (filter #(.exists (io/file %)) src-paths)) opts))
    (spit output-to (str (slurp prelude) "\n" (slurp output-to)))))
