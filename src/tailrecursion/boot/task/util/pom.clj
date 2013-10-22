;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.boot.task.util.pom
  (:import
    [org.apache.maven.model Model Repository Dependency Exclusion]
    org.apache.maven.model.io.xpp3.MavenXpp3Writer))

(defmacro dotoseq [obj seq-exprs & body]
  `(let [o# ~obj] (doseq ~seq-exprs (doto o# ~@body)) o#))

(defn ^Model set-repositories! [^Model model repositories]
  (dotoseq model [repo repositories]
    (.addRepository
     (doto (Repository.)
       (.setUrl repo)))))

(defn extract-ids [sym]
  (let [[group artifact] ((juxt namespace name) sym)]
    [(or group artifact) artifact]))

(defn ^Model set-dependencies! [^Model model dependencies]
  (dotoseq model
    [[project version & {:keys [exclusions]}] dependencies
     :let [[group artifact] (extract-ids project)]]
    (.addDependency
     (doto (Dependency.)
       (.setGroupId group)
       (.setArtifactId artifact)
       (.setVersion version)
       (.setExclusions
        (for [e exclusions :let [[group artifact] (extract-ids e)]]
          (doto (Exclusion.)
            (.setGroupId group)
            (.setArtifactId artifact))))))))

(defn ^Model build-model [project version repositories dependencies directories]
  (let [[group artifact] (extract-ids project)]
    (doto (Model.)
      (.setGroupId group)
      (.setArtifactId artifact)
      (.setVersion version)
      (set-repositories! repositories)
      (set-dependencies! dependencies))))

(defn make-pom [& args]
  (let [model (apply build-model args)]
    (with-out-str (.write (MavenXpp3Writer.) *out* model))))
