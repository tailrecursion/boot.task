(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require 
    [tailrecursion.boot.task.util.file  :as f]
    [tailrecursion.boot.task.util.cljs  :refer [compile-cljs]]
    [tailrecursion.boot.task.util.pom   :refer [make-pom]]
    [tailrecursion.boot.task.util.jar   :refer [create-jar!]]
    [tailrecursion.boot.core            :refer [deftask make-event]]
    [tailrecursion.boot.deps            :refer [deps]]
    [tailrecursion.boot.tmpregistry     :refer [mk! mkdir!]]
    [clojure.stacktrace                 :refer [print-stack-trace print-cause-trace]]
    [clojure.pprint                     :refer [pprint]]
    [clojure.java.io                    :refer [file delete-file make-parents]]
    [clojure.set                        :refer [difference intersection union]]))

(defmacro print-time [msg ok fail expr]
  `(let [start# (System/currentTimeMillis)]
     (try
       (printf ~msg)
       (flush)
       (printf ~ok (do ~expr (double (/ (- (System/currentTimeMillis) start#) 1000))))
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

;; Process event if watched directories have modified files in them ;;;;;;;;;;;

(deftask watch
  "Watch :directories and rebuild when files change."
  [boot & {:keys [type msec]}]
  (let [dirs      (:directories @boot)
        watchers  (map f/make-watcher dirs)
        since     (atom 0)]
    (comp
      (auto boot msec)
      (fn [continue]
        (fn [event]
          (let [info  (reduce (partial merge-with union) (map #(%) watchers))]
            (if-let [mods (seq (info (or type :time)))] 
              (let [path  (f/path (first mods))
                    xtr   (when-let [c (and (next mods) (count (next mods)))]
                            (format " and %d other%s" c (if (< 1 c) "s" "")))
                    msg   (format "Building %s%s ... " path (str xtr))
                    ok    "done. %.3f sec - 00:00:00 "
                    fail  (format "dang!\n\n%%s\nBuilding %s ... fail. %%.3f sec - 00:00:00 " path)]
                (when (not= 0 @since) (println) (flush)) 
                (reset! since (:time event))
                (print-time msg ok fail (continue (assoc event :watch info))))
              (let [diff  (long (/ (- (:time event) @since) 1000))
                    pad   (apply str (repeat 9 "\b"))
                    s     (mod diff 60)
                    m     (mod (long (/ diff 60)) 60)
                    h     (mod (long (/ diff 3600)) 24)]
                (printf "%s%02d:%02d:%02d " pad h m s)
                (flush)))))))))

;; Sync (copy) files between directories ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask sync
  "Copy/sync files between directories."
  [boot dst srcs]
  #((pass-thru-wrap f/sync) % (file dst) (map file srcs)))

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
        src-paths   (:directories @boot)
        tmp         (get-in @boot [:system :tmpregistry])
        depjars     (deps boot)
        output-dir  (mkdir! tmp ::output-dir)
        flib-out    (mkdir! tmp ::flib-out)
        lib-out     (mkdir! tmp ::lib-out)
        ext-out     (mkdir! tmp ::ext-out)
        inc-out     (mkdir! tmp ::inc-out)
        x-opts      (->> {:output-to  (f/path output-to)
                          :output-dir output-dir}
                      (merge base-opts opts))]
    #((pass-thru-wrap compile-cljs) % src-paths depjars flib-out lib-out ext-out inc-out x-opts)))

;; Build jar files ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask jar
  "Build a jar file."
  [boot & {:keys [output-dir main manifest]}]
  (assert (and (:project @boot) (:version @boot))
          "Both :project and :version must be defined.")
  (let [{:keys [project version repositories dependencies directories]} @boot
        directories (map file directories)
        output-dir  (doto (file (or output-dir (:target @boot))) (.mkdirs))
        tmp         (get-in @boot [:system :tmpregistry])
        tmp-dir     (mkdir! tmp ::jar-tmp-dir)
        pom-xml     (make-pom project version repositories dependencies directories)]
    #((pass-thru-wrap create-jar!) % project version directories output-dir tmp-dir :main main :manifest manifest :pom pom-xml)))