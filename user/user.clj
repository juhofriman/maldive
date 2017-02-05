(ns user
  (:require [ring.adapter.jetty :refer :all]
            [maldive.handler :as handler]))


(defn start!
  []
  (run-jetty #'handler/app {:port 3000}))
