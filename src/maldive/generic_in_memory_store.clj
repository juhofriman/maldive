(ns maldive.generic-in-memory-store)

(def seqs (atom {}))

(def storage (atom {}))

(defn get-storage
  [type]
  (if (contains? @storage type)
    (get @storage type)
    (do
      (swap! seqs assoc type (atom 1))
      (get (swap! storage assoc type (atom {})) type))))

(defn store
  [type obj]
  (let [store (get-storage type)
        id (or (get obj "id") (swap! (get @seqs type) inc))]
     (swap! store assoc id (assoc obj "id" id))))

(defn docs-by-type
  [key]
  (map second (into [] @(get-storage key))))

(defn get-entity
  [type id]
  (get @(get-storage type) id))
