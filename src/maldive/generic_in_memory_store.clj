(ns maldive.generic-in-memory-store)

(def seqs {:generic-entity (atom 1)
           :memo (atom 1)})

(def storage {:generic-entity (atom {})
              :memo (atom {})})

(defn store
  [type obj]
  (let [store (get storage type) id (or (get obj "id") (swap! (:generic-entity seqs) inc))]
    (swap! store assoc id (assoc obj "id" id))))

(defn docs-by-type
  [key]
  (map second (into [] @(get storage key))))

(defn get-entity
  [type id]
  (get @(get storage type) id))
