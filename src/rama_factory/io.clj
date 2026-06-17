(ns rama-factory.io
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint])
  (:import [java.io File]
           [java.nio.file Files StandardCopyOption]))

(defn file
  [& parts]
  (reduce (fn [^File parent part] (io/file parent (str part)))
          (io/file (first parts))
          (rest parts)))

(defn ensure-dir!
  [path]
  (let [f (io/file path)]
    (.mkdirs f)
    f))

(defn ensure-parent!
  [path]
  (when-let [parent (.getParentFile (io/file path))]
    (.mkdirs parent)))

(defn read-edn
  [path]
  (edn/read-string (slurp path)))

(defn write-edn!
  [path value]
  (ensure-parent! path)
  (spit path (with-out-str (pprint/pprint value)))
  path)

(defn write-text!
  [path text]
  (ensure-parent! path)
  (spit path text)
  path)

(defn move!
  [from to]
  (ensure-parent! to)
  (Files/move (.toPath (io/file from))
              (.toPath (io/file to))
              (into-array StandardCopyOption [StandardCopyOption/REPLACE_EXISTING]))
  to)

(defn list-files
  [path]
  (let [f (io/file path)]
    (if (.exists f)
      (->> (.listFiles f)
           (filter #(.isFile ^File %))
           (sort-by #(.getName ^File %)))
      [])))

(defn delete-tree!
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (doseq [child (reverse (file-seq f))]
        (Files/deleteIfExists (.toPath ^File child))))))
