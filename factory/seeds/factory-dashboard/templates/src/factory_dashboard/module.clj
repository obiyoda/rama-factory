(ns {{app_ns}}.factory-dashboard.module
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.aggs :as aggs]))

(defrecord FactoryEvent
  [event-id event-type run-id work-id role persona-id persona-name phase artifact status message occurred-at])

(defn normalize-event
  [{:keys [event-id event-type run-id work-id role persona-id persona-name phase artifact status message occurred-at]}]
  (->FactoryEvent (or event-id (str (java.util.UUID/randomUUID)))
                  (name event-type)
                  (or run-id "default-run")
                  (or work-id "default-work")
                  (or role "system")
                  (or persona-id "system")
                  (or persona-name "System")
                  (or phase "unassigned")
                  (or artifact "")
                  (or status "recorded")
                  (or message "")
                  (long (or occurred-at (System/currentTimeMillis)))))

(defmodule FactoryDashboardModule
  [setup topologies]
  (declare-depot setup *factory-events-depot (hash-by :run-id))

  (let [s (stream-topology topologies "factory-dashboard")]
    (declare-pstate s $$runs
                    {String
                     (fixed-keys-schema {:run-id String
                                         :status String
                                         :message String
                                         :updated-at Long})})
    (declare-pstate s $$events-by-run
                    {String
                     (map-schema String
                                 (fixed-keys-schema {:event-type String
                                                     :work-id String
                                                     :role String
                                                     :persona-id String
                                                     :persona-name String
                                                     :phase String
                                                     :artifact String
                                                     :status String
                                                     :message String
                                                     :occurred-at Long})
                                 {:subindex? true})})
    (declare-pstate s $$handoffs-by-role
                    {String
                     (map-schema String
                                 (fixed-keys-schema {:run-id String
                                                     :work-id String
                                                     :persona-id String
                                                     :persona-name String
                                                     :phase String
                                                     :status String
                                                     :message String
                                                     :occurred-at Long})
                                 {:subindex? true})})
    (declare-pstate s $$artifacts-by-run
                    {String
                     (map-schema String
                                 (fixed-keys-schema {:role String
                                                     :persona-id String
                                                     :persona-name String
                                                     :phase String
                                                     :artifact String
                                                     :status String
                                                     :occurred-at Long})
                                 {:subindex? true})})
    (declare-pstate s $$event-counts {String Long})

    (<<sources s
      (source> *factory-events-depot :> {:keys [*event-id
                                                *event-type
                                                *run-id
                                                *work-id
                                                *role
                                                *persona-id
                                                *persona-name
                                                *phase
                                                *artifact
                                                *status
                                                *message
                                                *occurred-at]})
      (local-transform> [(keypath *run-id)
                         (multi-path [:run-id (termval *run-id)]
                                     [:status (termval *status)]
                                     [:message (termval *message)]
                                     [:updated-at (termval *occurred-at)])]
                        $$runs)
      (local-transform> [(keypath *run-id *event-id)
                         (termval {:event-type *event-type
                                   :work-id *work-id
                                   :role *role
                                   :persona-id *persona-id
                                   :persona-name *persona-name
                                   :phase *phase
                                   :artifact *artifact
                                   :status *status
                                   :message *message
                                   :occurred-at *occurred-at})]
                        $$events-by-run)
      (|hash *role)
      (local-transform> [(keypath *role *event-id)
                         (termval {:run-id *run-id
                                   :work-id *work-id
                                   :persona-id *persona-id
                                   :persona-name *persona-name
                                   :phase *phase
                                   :status *status
                                   :message *message
                                   :occurred-at *occurred-at})]
                        $$handoffs-by-role)
      (|hash *run-id)
      (local-transform> [(keypath *run-id *event-id)
                         (termval {:role *role
                                   :persona-id *persona-id
                                   :persona-name *persona-name
                                   :phase *phase
                                   :artifact *artifact
                                   :status *status
                                   :occurred-at *occurred-at})]
                        $$artifacts-by-run)
      (|hash *event-type)
      (+compound $$event-counts {*event-type (aggs/+sum 1)})
      (ack-return> *event-id))))
