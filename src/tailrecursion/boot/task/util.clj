;;;-------------------------------------------------------------------------------------------------
;;; Copyright (c) Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and
;;; distribution terms for this software are covered by the Eclipse Public License 1.0. By using
;;; this software in any fashion, you are agreeing to be bound by the terms of this license.  You
;;; must not remove this notice, or any other, from this software.
;;; http://www.eclipse.org/legal/epl-v10.html
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util
  (:require
    [clojure.java.io         :as io]
    [tailrecursion.boot.file :as f]
    [clojure.set             :refer [difference]] )
  (:import
    [java.io       File]
    [java.util.jar JarEntry JarOutputStream] ))

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn extract-ids [sym]
  (let [[group artifact] ((juxt namespace name) sym)]
    [(or group artifact) artifact] ))

(defmacro let-assert-keys [binding & body]
  "Let expression that throws an exception when any of the expected bindings is missing."
  (let [[ks m] [(butlast binding) (last binding)]
        req-ks (set (map keyword ks)) ]
   `(if-let [dif-ks# (not-empty (difference ~req-ks (set (keys ~m))))]
      (throw (new AssertionError (apply format "missing key(s): %s" dif-ks#)))
      (let [{:keys ~ks} ~m] ~@body) )))

(defmacro dotoseq [obj seq-exprs & body]
  `(let [o# ~obj] (doseq ~seq-exprs (doto o# ~@body)) o#) )

(defn- write! [^JarOutputStream stream ^File file]
  (let [buf (byte-array 1024)]
    (with-open [in (io/input-stream file)]
      (loop [n (.read in buf)]
        (when-not (= -1 n)
          (.write stream buf 0 n)
          (recur (.read in buf)) )))))

(defn add!
  ([^JarOutputStream stream ^File tgt-path ^File src-base ^File src-path]
    (add! stream tgt-path src-base src-path nil) )
  ([^JarOutputStream stream ^File tgt-path ^File src-base ^File src-path filter]
    (let [rel #(.getPath  (f/relative-to %1 %2))
          ent #(doto (JarEntry. (.getPath (io/file %1 (rel %2 %3)))) (.setTime (.lastModified %3))) ]
      (if (f/dir? src-path)
        (dotoseq stream [f (.listFiles src-path) :when (if filter (filter (.getName f)) true)] (add! tgt-path src-base f filter) )
        (doto stream (.putNextEntry (ent tgt-path src-base src-path)) (write! src-path) (.closeEntry)) ))))
