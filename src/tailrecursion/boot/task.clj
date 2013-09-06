(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require 
    [cljs.closure                   :as cljs]
    [tailrecursion.boot.task.file   :as f]
    [tailrecursion.boot.core        :refer [make-event]]
    [tailrecursion.boot.deps        :refer [deps]]
    [tailrecursion.boot.tmpregistry :refer [mk! mkdir!]]
    [clojure.stacktrace             :refer [print-cause-trace]]
    [clojure.string                 :refer [split join blank?]]
    [clojure.pprint                 :refer [pprint]]
    [digest                         :refer [md5]]
    [clojure.java.io                :refer [file delete-file make-parents]]
    [clojure.set                    :refer [difference intersection union]]))

;; Task builders ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn wrap-debug [continue]
  (fn [event]
    (pprint event)
    (flush)
    (continue event)))

(defn debug [boot]
  #(wrap-debug %))

;; Loop every msec ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-auto [continue & [msec]]
  (fn [event]
    (continue event)
    (Thread/sleep (or msec 200)) 
    (recur (make-event event))))

(defn auto [boot & [msec]]
  (locking boot
    (if (:auto @boot)
      (identity-task boot)
      (do (swap! boot assoc :auto true) #(wrap-auto % msec)))))

;; Process event if watched directories have modified files in them ;;;;;;;;;;;

(defn- make-watcher [dir]
  (let [prev (atom nil)]
    (fn []
      (let [only-file #(filter f/file? %)
            make-info #(vector [% (.lastModified %)] [% (md5 %)])
            file-info #(mapcat make-info %)
            info      (->> dir file file-seq only-file file-info set)
            mods      (difference (union info @prev) (intersection info @prev))
            by        #(->> %2 (filter (comp %1 second)) (map first) set)]
        (reset! prev info)
        {:hash (by string? mods) :time (by number? mods)}))))

(defn- print-time [msg ok fail f]
  (let [now!    #(System/currentTimeMillis)
        time*   #(let [start (now!)] (%) (double (/ (- (now!) start) 1000)))
        trace   #(with-out-str (print-cause-trace %))
        printf* (fn [& s] (apply printf s) (flush))]
    (printf* msg)
    (try (printf* ok (time* f))
      (catch Throwable e (printf* fail (trace e))))))

(defn wrap-watch [continue type dirs]
  (let [watchers (map make-watcher dirs)]
    (fn [event]
      (let [info (reduce (partial merge-with union) (map #(%) watchers))]
        (when-let [mods (seq (info type))] 
          (let [path  (f/path (first mods))
                xtr   (when-let [c (next mods)] (format " and %d others" (count c)))
                msg   (format "Building %s%s..." path (str xtr))
                ok    "done. (%.3f sec)\n"
                fail  "dang!\n%s\n"]
            (print-time msg ok fail #(continue (assoc event :watch info)))))))))

(defn watch [boot & [msec]]
  (let [dirs (:directories @boot)]
    (comp (auto boot msec) #(wrap-watch % :time dirs))))

;; Sync (copy) files between directories ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-sync [continue type dst srcs]
  (let [dst (file dst), srcs (map file srcs)]
    (fn [event]
      (apply f/sync type dst srcs)
      (continue event))))

(defn sync [boot dst srcs]
  #(wrap-sync % dst srcs))

;; Compile ClojureScript ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord CljsSourcePaths [paths]
  cljs/Compilable
  (-compile [this opts]
    (mapcat #(cljs/-compile % opts) paths)))

(let [last-counter (atom 0)]
  (def cljs-counter! #(swap! last-counter inc)))

(defn install-cljs-deps! [src-paths depjars incs exts libs flibs]
  (let [match     #(last (re-find #"[^.]+\.([^.]+)\.js$" %))
        dirmap    {"inc" incs "ext" exts "lib" libs "flib" flibs}
        outfile   #(file %1 (str (format "%010d" (cljs-counter!)) "_" (f/name %2)))
        write1    #(when-let [d (dirmap (match %1))]
                     (spit (doto (outfile d %1) make-parents) (slurp %2))) 
        write     #(map (partial apply write1) %)
        path-seq  (fn [x] (map f/path (file-seq (file x))))
        dep-files (->> depjars (map second) (mapcat identity))
        src-files (->> src-paths (mapcat path-seq) (keep f/file?))]
    (doall (->> dep-files reverse write))
    (doall (->> src-files sort (map (juxt identity file)) write))))

(defn wrap-cljs [continue src-paths depjars flib-out lib-out ext-out inc-out opts]
  (assert (:output-to opts) "No :output-to option specified.")
  (fn [event]
    (f/clean! (:output-to opts) flib-out lib-out ext-out inc-out)
    (install-cljs-deps! src-paths depjars inc-out ext-out lib-out flib-out) 
    (let [{:keys [output-to]} opts
          files #(filter f/file? (file-seq %))
          paths #(mapv f/path (files %))
          cat   #(join "\n" (mapv slurp %)) 
          srcs  (CljsSourcePaths. src-paths)
          exts  (paths ext-out)
          incs  (cat (sort (files inc-out)))]
      (cljs/build srcs (update-in opts [:externs] into exts))
      (spit output-to (str incs "\n" (slurp output-to)))
      (continue event))))

(defn cljs [boot output-to & [opts]]
  (let [base-opts   {:output-dir    nil
                     :optimizations :whitespace
                     :warnings      true
                     :externs       []
                     :libs          []
                     :foreign-libs  []
                     :pretty-print  true}
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
    #(wrap-cljs % src-paths depjars flib-out lib-out ext-out inc-out x-opts)))

