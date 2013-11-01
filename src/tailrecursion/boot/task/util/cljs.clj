;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.boot.task.util.cljs
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

(let [last-counter (atom 0)]
  (defn- dep-counter! []
    (swap! last-counter inc)))

(defn install-deps [src-paths depjars incs exts libs flibs]
  (println "Installing ClojureScript dependencies...")
  (let [match     #(last (re-find #"[^.]+\.([^.]+)\.js$" %))
        dirmap    {"inc" incs "ext" exts "lib" libs "flib" flibs}
        outfile   #(file %1 (str (format "%010d" (dep-counter!)) "_" (f/name %2)))
        write1    #(when-let [d (dirmap (match %1))]
                     (spit (doto (outfile d %1) make-parents) (slurp %2))) 
        write     #(map (partial apply write1) %)
        path-seq  (fn [x] (map f/path (file-seq (file x))))
        dep-files (->> depjars (map second) (mapcat identity))
        src-files (->> src-paths (mapcat path-seq) (keep f/file?))]
    (doall (->> dep-files reverse write))
    (doall (->> src-files sort (map (juxt identity file)) write))))

(defn compile-cljs
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
