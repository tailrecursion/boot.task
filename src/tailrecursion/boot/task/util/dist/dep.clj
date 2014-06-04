;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.dist.dep
  (:require
    [tailrecursion.boot.kahnsort   :as k]
    ;[tailrecursion.boot.deps      :as d]
    [tailrecursion.boot.task.util :as u]
    [clojure.java.io              :as io] )
  (:import 
    [java.io       File]
    [java.util.jar JarEntry JarOutputStream] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- write! [^JarOutputStream stream ^File file]
  (let [buf (byte-array 1024)] 
    (with-open [in (io/input-stream file)]
      (loop [n (.read in buf)]
        (when-not (= -1 n)
          (.write stream buf 0 n)
          (recur (.read in buf)) )))))

(defn jar-files [reps deps]
  (require 'cemerick.pomegranate.aether)
  (let [resolve-dependencies (resolve 'cemerick.pomegranate.aether/resolve-dependencies)]
    (->> (resolve-dependencies :repositories (zipmap reps reps) :coordinates deps)
      (k/topo-sort)
      (map #(.getPath (:file (meta %))))
      (filter #(.endsWith % ".jar"))
      (map io/file) )))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-dep! [reps deps]
  (println "Adding jar file dependencies...")
  (let [path "WEB-INF/lib/"
        jars (jar-files reps deps)]
    (fn [^JarOutputStream stream]
      (u/dotoseq stream [jar jars]
        (.putNextEntry (JarEntry. (str path (.getName jar))))
        (write! jar)
        (.closeEntry) ))))