;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.dist.src
  (:require 
    [tailrecursion.boot.file      :as f]
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

(defn- add! [^JarOutputStream stream ^File tgt-path ^File src-base ^File src-path]
  (let [rel #(.getPath  (f/relative-to %1 %2))
        ent #(doto (JarEntry. (.getPath (io/file %1 (rel %2 %3)))) (.setTime (.lastModified %3))) ]
    (if (f/dir? src-path)
      (u/dotoseq stream [f (.listFiles src-path)] (add! tgt-path src-base f) )
      (doto stream (.putNextEntry (ent tgt-path src-base src-path)) (write! src-path) (.closeEntry)) )))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-src! [src-paths & [tgt-path]]
  (println "Adding source files...")
  (fn [^JarOutputStream stream]
    (u/dotoseq stream [s src-paths :let [src-path (io/file s)]]
      (add! (io/file tgt-path) src-path src-path) )))
