;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.cljs.compiler
  (:refer-clojure :exclude [compile])
  (:require
    [tailrecursion.boot.file  :as f]
    [cljs.env                 :as env]
    [cljs.closure             :as cljs]
    [clojure.string           :refer [split join blank?]]
    [clojure.pprint           :refer [pprint]]
    [clojure.java.io          :refer [input-stream output-stream file delete-file make-parents]] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord CljsSourcePaths [paths]
  cljs/Compilable
  (-compile [this opts]
    (mapcat #(cljs/-compile % opts) paths) ))

(def cljs-env (env/default-compiler-env))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn compile [src-paths flib-out lib-out ext-out inc-out {:keys [output-to] :as opts}]
  (assert output-to "No :output-to option specified.")
  (let [files #(filter (fn [x] (.isFile x)) (file-seq %))
        paths #(mapv (fn [x] (.getPath x)) (files %))
        cat   #(join "\n" (mapv slurp %))
        srcs  (CljsSourcePaths. (filter #(.exists (file %)) src-paths))
        exts  (paths ext-out)
        incs  (cat (sort (files inc-out))) ]
    (env/with-compiler-env cljs-env
      (cljs/build srcs (update-in opts [:externs] into exts)) )
    (spit output-to (str incs "\n" (slurp output-to))) ))
