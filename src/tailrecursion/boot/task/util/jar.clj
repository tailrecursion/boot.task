(ns tailrecursion.boot.task.util.jar 
  (:require 
    [tailrecursion.boot.task.util.file  :as f]
    [tailrecursion.boot.task.util.pom   :refer [extract-ids]]
    [clojure.pprint                     :refer [pprint]]
    [clojure.java.io                    :refer [input-stream output-stream file delete-file make-parents]])
  (:import
    [java.io File]
    [java.util.jar JarOutputStream JarEntry Manifest Attributes Attributes$Name]))

(def dfl-manifest
  {"Created-By"  "boot"
   "Built-By"    (System/getProperty "user.name")
   "Build-Jdk"   (System/getProperty "java.version")})

(defn- make-manifest [main extra-attributes]
  (let [extra-attr  (merge-with into dfl-manifest extra-attributes)
        manifest    (Manifest.)
        attributes  (.getMainAttributes manifest)]
    (.put attributes Attributes$Name/MANIFEST_VERSION "1.0")
    (when-let [m (and main (.replaceAll (str main) "-" "_"))] 
      (.put attributes Attributes$Name/MAIN_CLASS m))
    (doseq [[k v] extra-attr] (.put attributes (Attributes$Name. k) v))
    manifest))

(defn- write! [^JarOutputStream target ^File src]
  (let [buf (byte-array 1024)] 
    (with-open [in (input-stream src)]
      (loop [n (.read in buf)]
        (when-not (= -1 n)
          (.write target buf 0 n)
          (recur (.read in buf)))))))

(defn- add! [^JarOutputStream target ^File base ^File src]
  (let [rel #(f/path (f/relative-to %1 %2))
        ent #(doto (JarEntry. (rel %1 %2)) (.setTime (.lastModified %2)))]
    (if (f/dir? src)
      (doseq [f (.listFiles src)] (add! target base f))
      (doto target (.putNextEntry (ent base src)) (write! src) (.closeEntry)))))

(defn create-jar! [project version src-dirs output-dir tmp-dir & {:keys [main manifest pom]}]
  (let [[group-id artifact-id] (extract-ids project)
        pom-xml  ((if (f/file? pom) slurp str) pom) 
        manifest (make-manifest main manifest)
        jar-name (str (if artifact-id (str artifact-id "-" version) "out") ".jar") 
        jar-file (file output-dir jar-name)
        pom-path ["META-INF" "maven" group-id artifact-id "pom.xml"] 
        pom-file (doto (apply file tmp-dir pom-path) make-parents)]
    (with-open [j (JarOutputStream. (output-stream jar-file) manifest)]
      (when pom-xml
        (spit pom-file pom-xml)
        (add! j tmp-dir tmp-dir))
      (doseq [d src-dirs] (add! j d d)))))
