;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.dist
  (:require 
    [clojure.java.io :refer [file make-parents output-stream]])
  (:import
    [java.util.jar JarEntry JarOutputStream Manifest Attributes$Name] ))

;;; constants ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dfl-attr
  {"Created-By"  "boot"
   "Built-By"    (System/getProperty "user.name")
   "Build-Jdk"   (System/getProperty "java.version") })

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-manifest [main ext-attrs]
  (let [extra-attr  (merge-with into dfl-attr ext-attrs)
        manifest    (Manifest.) 
        attributes  (.getMainAttributes manifest) ]
    (.put attributes Attributes$Name/MANIFEST_VERSION "1.0")
    (when-let [m (and main (.replaceAll (str main) "-" "_"))] 
      (.put attributes Attributes$Name/MAIN_CLASS m) )
    (doseq [[k v] extra-attr] (.put attributes (Attributes$Name. k) v))
    manifest) )

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn spit-dist! [file main man-attrs & dst-fns]
  (println "Creating manifest.mf...")
  (let [manifest (create-manifest main man-attrs)
        file     (doto file make-parents)]
    (with-open [stream (JarOutputStream. (output-stream file) manifest)]
      ((apply comp dst-fns) stream) )))