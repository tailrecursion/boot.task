;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.dist.pom
  (:refer-clojure                   :exclude [name])
  (:require 
    [clojure.java.io                  :refer [output-stream writer]]
    [tailrecursion.boot.task.util     :refer [extract-ids]]
    [tailrecursion.boot.task.util.xml :refer [decelems defelem element]] )
  (:import
    [java.util     Properties]
    [java.util.jar JarEntry JarOutputStream] ))

;;; elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(decelems artifactId connection description dependencies exclusions developerConnection enabled 
  groupId id licenses modelVersion name repositories scope tag url version)

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defelem exclusion [{g :group-id a :artifact-id}]
  (element :exclusion
    (groupId    g)
    (artifactId a) ))

(defelem dependency [{g :group-id a :artifact-id v :version s :scope} elems]
  (element :dependency
    (groupId    g)
    (artifactId a)
    (version    v)
    (scope      s) 
    elems ))

(defelem license [{n :name u :url}]
  (element :license
    (name n)
    (url  u) ))

(defelem releases [{e :enabled}]
  (element :releases
    (enabled e) ))

(defelem repository [{i :id u :url} elems]
  (element :repository
    (id  i)
    (url u) 
    elems ))

(defelem snapshots [{e :enabled}]
  (element :snapshots
    (enabled e) ))

(defelem project [{g :group-id a :artifact-id v :version d :description u :url} elems]
  (element :project 
    :xmlns              "http://maven.apache.org/POM/4.0.0"
    :xmlns:xsi          "http://www.w3.org/2001/XMLSchema-instance"
    :xsi:schemaLocation "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" 
    (modelVersion       "4.0.0")
    (groupId            g)
    (artifactId         a)
    (version            v)
    (name               a)
    (description        d)
    (url                u) 
    elems ))

(defn pom-xml [g a v d u ln lu deps reps]
  (project :group-id g :artifact-id a :version v :description d :url u
    (licenses
      (license :name ln :url lu) )
    (repositories  (for [url reps] 
      (repository :url url 
        (releases :enabled true)
        (snapshots :enabled true) )))
    (dependencies (for [[p v & {es :exclusions s :scope}] deps :let [[g a] (extract-ids p)]]
      (dependency :group-id g :artifact-id a :version v :scope s
        (exclusions (for [[p] es :let [[g a] (extract-ids p)]]
          (exclusion :group-id g :artifact-id a)) ))))))

(defn pom-properties [gid aid vers]
  (doto (Properties.)
    (.setProperty "groupId"    gid)
    (.setProperty "artifactId" aid)
    (.setProperty "version"    vers)  ))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-pom! [group-id artifact-id version pom-xml]
  (println "Creating pom.xml...")
  (println "Creating pom.properties...")
  (let [dir-path ["META-INF" "maven" group-id artifact-id]
        xml-path (apply str (interpose "/" (conj dir-path "pom.xml")))
        prp-path (apply str (interpose "/" (conj dir-path "pom.properties")))
        prp-obj  (pom-properties group-id artifact-id version) ]
    (fn [^JarOutputStream stream]
      (.putNextEntry stream (JarEntry. xml-path))
      (.write stream (.getBytes (pr-str pom-xml))) ;; todo: fix
      (.putNextEntry stream (JarEntry. prp-path))
      (.store prp-obj stream (str group-id "/" artifact-id " " version " property file"))
      (.closeEntry stream) )))

(defn spit-pom! [file pom-xml]
  (with-open [stream (output-stream file)]
    (.write stream (.getBytes (pr-str pom-xml))) ))
