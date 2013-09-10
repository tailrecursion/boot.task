(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require 
    [tailrecursion.boot.file            :as f]
    [tailrecursion.boot.gitignore       :as g]
    [tailrecursion.boot.task.util.cljs  :refer [install-deps compile-cljs]]
    [tailrecursion.boot.task.util.pom   :refer [make-pom]]
    [tailrecursion.boot.task.util.jar   :refer [create-jar!]]
    [tailrecursion.boot.core            :refer [deftask make-event mk! mkdir!
                                                add-sync! sync! tmpfile? ignored?]]
    [tailrecursion.boot.deps            :refer [deps]]
    [clojure.string                     :refer [blank? join]]
    [clojure.stacktrace                 :refer [print-stack-trace print-cause-trace]]
    [clojure.pprint                     :refer [pprint]]
    [clojure.java.io                    :refer [file delete-file make-parents]]
    [clojure.set                        :refer [difference intersection union]])
  (:import
    [java.io StringWriter]))

(defmacro with-out-err-str [& body]
  `(let [out# (new StringWriter)
         err# (new StringWriter)]
     (binding [*out* out#, *err* err#]
       ~@body
       [(str out#) (str err#)])))

(defmacro print-time [msg ok out fail expr]
  `(let [start# (System/currentTimeMillis)]
     (try
       (printf ~msg)
       (flush)
       (let [printed# (with-out-err-str ~expr)
             end# (double (/ (- (System/currentTimeMillis) start#) 1000))]
         (if (some (complement blank?) printed#)
           (printf ~out (join "\n\n" (remove blank? printed#)) end#)
           (printf ~ok end#)))
       (catch Throwable e#
         (printf ~fail (with-out-str (print-cause-trace e# 10))
                       (double (/ (- (System/currentTimeMillis) start#) 1000))))
       (finally (flush)))))

;; Task builders ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pass-thru-wrap [f]
  (fn [continue & args]
    (fn [event]
      (apply f args)
      (continue event))))

(defn pre-task
  [boot & {:keys [process configure] :or {process identity configure identity}}]
  (locking boot
    (swap! boot configure) 
    (fn [continue]
      (fn [event]
        (continue (process event))))))

(defn post-task
  [boot & {:keys [process configure] :or {process identity configure identity}}]
  (locking boot
    (swap! boot configure) 
    (fn [continue]
      (fn [event]
        (process (continue event))))))

;; A task that does nothing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def identity-task pre-task)

;; Task that just does configuration of boot env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn config-task [boot configure]
  (pre-task boot :configure configure))

;; Print the boot environment ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask env
  "Print the boot configuration."
  [boot]
  (fn [continue]
    (let [env @boot]
      (fn [event]
        (pprint env)
        (flush)
        (continue event)))))

;; Print the event ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask debug
  "Print the event map."
  [boot]
  (fn [continue]
    (fn [event]
      (pprint event)
      (flush)
      (continue event))))

;; Loop every msec ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask auto
  "Run every msec (200) milliseconds."
  [boot & [msec]]
  (if-not (locking boot
            (when-not (:auto @boot)
              (swap! boot assoc :auto true)))
    (identity-task boot)
    (fn [continue]
      (fn [event]
        (continue event)
        (Thread/sleep (or msec 200)) 
        (recur (make-event event))))))

;; Process event if watched src-paths have modified files in them ;;;;;;;;;;;

(deftask watch
  "Watch :src-paths and rebuild when files change."
  [boot & {:keys [type msec]}]
  (let [dirs      (remove (partial tmpfile? boot) (:src-paths @boot)) 
        ign?      (partial ignored? boot)
        watchers  (map f/make-watcher dirs)
        since     (atom 0)]
    (comp
      (auto boot msec)
      (fn [continue]
        (fn [event]
          (let [info  (reduce (partial merge-with union) (map #(%) watchers))]
            (if-let [mods (->> (or type :time) (get info) (map file) (remove ign?) seq)]
              (let [path  (f/path (first mods))
                    xtr   (when-let [c (and (next mods) (count (next mods)))]
                            (format " and %d other%s" c (if (< 1 c) "s" "")))
                    msg   (format "Building %s%s ... " path (str xtr))
                    ok    "okay. %.3f sec - 00:00:00 "
                    out   (format "info:\n\n%%s\nBuilding %s ... okay. %%.3f sec - 00:00:00 " path)
                    fail  (format "dang!\n\n%%s\nBuilding %s ... fail. %%.3f sec - 00:00:00 " path)]
                (when (not= 0 @since) (println) (flush)) 
                (reset! since (:time event))
                (print-time msg ok out fail (continue (assoc event :watch info))))
              (let [diff  (long (/ (- (:time event) @since) 1000))
                    pad   (apply str (repeat 9 "\b"))
                    s     (mod diff 60)
                    m     (mod (long (/ diff 60)) 60)
                    h     (mod (long (/ diff 3600)) 24)]
                (sync! boot)
                (printf "%s%02d:%02d:%02d " pad h m s)
                (flush)))))))))

;; Sync (copy) files between directories ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask sync
  "Copy/sync files between directories."
  [boot dst srcs]
  (apply add-sync! boot dst srcs)
  (identity-task boot))

;; Compile ClojureScript ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask cljs
  "Compile ClojureScript source files."
  [boot & {:keys [output-to opts]}]
  (let [base-opts   {:optimizations :whitespace
                     :warnings      true
                     :externs       []
                     :libs          []
                     :foreign-libs  []
                     :pretty-print  true}
        output-to   (or (file output-to) (file (:public @boot) "main.js"))
        src-paths   (:src-paths @boot)
        depjars     (deps boot)
        output-dir  (mkdir! boot ::output-dir)
        flib-out    (mkdir! boot ::flib-out)
        lib-out     (mkdir! boot ::lib-out)
        ext-out     (mkdir! boot ::ext-out)
        inc-out     (mkdir! boot ::inc-out)
        x-opts      (->> {:output-to  (f/path output-to)
                          :output-dir output-dir}
                      (merge base-opts opts))]
    (make-parents output-to) 
    (f/clean! output-to flib-out lib-out ext-out inc-out) 
    (install-deps src-paths depjars inc-out ext-out lib-out flib-out)
    #((pass-thru-wrap compile-cljs) % src-paths depjars flib-out lib-out ext-out inc-out x-opts)))

;; Build jar files ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask jar
  "Build a jar file."
  [boot & {:keys [output-dir main manifest]}]
  (assert (and (:project @boot) (:version @boot))
          "Both :project and :version must be defined.")
  (let [{:keys [project version repositories dependencies src-paths]} @boot
        src-paths (map file src-paths)
        output-dir  (doto (file (or output-dir (:target @boot))) (.mkdirs))
        tmp-dir     (mkdir! boot ::jar-tmp-dir)
        pom-xml     (make-pom project version repositories dependencies src-paths)]
    #((pass-thru-wrap create-jar!) % project version src-paths output-dir tmp-dir :main main :manifest manifest :pom pom-xml)))
