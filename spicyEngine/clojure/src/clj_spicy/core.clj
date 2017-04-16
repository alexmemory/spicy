(ns clj-spicy.core
  "Some convenience functions and specialized ways of running
  ++Spicy. http://www.db.unibas.it/projects/spicy/"
  (:require
   [incanter.core :as in]
   [clojure.string :as cljs]
   [clojure.java.io :as io]
   [me.raynes.fs :as fs]
   [clojure.tools.logging :as log]
   ;; [clj-psl.core :as pu]
   [clj-util.core :as u]
   [incanter-util.core :as iu]
   )

  (:import
   [it.unibas.spicy.model.datasource.values OID INullValue]
   [it.unibas.spicy.model.mapping SimpleConjunctiveQuery ComplexConjunctiveQuery
    ComplexQueryWithNegations FORule]
   [it.unibas.spicy.model.paths
    PathExpression SetAlias VariableCorrespondence VariablePathExpression
    VariableJoinCondition VariableSelectionCondition]
   [it.unibas.spicy.model.paths.operators GeneratePathExpression]
   [it.unibas.spicy.persistence.xml.operators ExportXMLInstances]))

;;; ====================================
;;; Tuples

(defn tuples-to-relations
  "Converted a list of tuples to a set of relations.  Given a list of
  tuples in logical form, return a map from predicates to lists of
  tuples in that relation."
  [tuples]
  (let [pred-maps
        (for [ato tuples]
          (let [pre (cljs/replace ato #"\(.+\)" "") ; No args
                args (cljs/split  ; Split up variables within atom
                      (cljs/replace ato #".+\((.+)\)" "$1") ; No pred
                      #",")
                ]
            {pre [args]}))
        pred-map (reduce #(merge-with concat %1 %2) pred-maps) ; Merge all maps
        pred-map (into {} (for [[v l] pred-map] [v (sort l)])) ; Sort each list
        ]
    pred-map))

;;; ====================================
;;; DSProxys

(defn dsproxy-args-of-tuple
  "Return a list of representations of the arguments of a given tuple"
  [tup]
  (for [att (.getChildren tup)]
    (let [vals (.getChildren att)
          val (first vals)
          leaf (.getValue val)
          leafVal
          (cond
            (instance? OID leaf)

            ;; (str "(variable id: " leaf ") "
            ;;      (subs (.getSkolemString leaf) 0 10) "...")
            ;; (.getSkolemString leaf)

            ;; Prevents tuple match with noncertain errors and unexplained?
            ;; (str "skol:" leaf)

            leaf
            
            (instance? INullValue leaf)
            ;; (str "(null value: " leaf ") ")
            ;; Prevents tuple match with noncertain errors and unexplained?
            ;; (str "null:" leaf)
            leaf
            
            :else leaf)
          ]
      ;; A single constant for each predicate argument
      (assert (= 1 (count vals)))
      leafVal)))

(defn dsproxy-export
  "Export a dsproxy to a Spicy XML file at a given file path."
  [dsprox to-dir to-file-name]
  (.exportXMLinstances
   (ExportXMLInstances.) 
   (.getInstances dsprox) (.getPath (io/file to-dir)) "Instance")
  (let [exported-file (fs/file (io/file to-dir "targetInstance0.xml"))
        file-to-overwrite (fs/file (io/file to-dir to-file-name))]
    (fs/rename exported-file file-to-overwrite)))

(defn dsproxy-instance-to-tuples
  "Retrieve a set of tuples from a relational instance."
  [inst]
  (let [tuples-by-rel
        (for [rel (.getChildren inst)]
          (for [tup (.getChildren rel)]
            (let [pred (.getLabel tup)  ; The predicate
                  args                  ; The constants
                  (dsproxy-args-of-tuple tup)
                  ]
              (str pred "(" (cljs/join "," args) ")"))))] ; As a string
    (flatten tuples-by-rel)))

(defn dsproxy-instance-to-tuples-with-pred
  "Retrieve a set of tuple-pred pairs from a relational instance."
  [inst]
  (let [tuple-pred-pairs
        (for [rel (.getChildren inst)
              tup (.getChildren rel)]
          (let [pred (.getLabel tup)  ; The predicate
                args                  ; The constants
                (dsproxy-args-of-tuple tup)
                ]
            [(str pred "(" (cljs/join "," args) ")") ; As a string
             pred]))]
    tuple-pred-pairs))

(defn dsproxy-to-tuples
  "Retrieve a set of tuples from a relational data source proxy."
  [dsproxy]
  (let [instances (.getInstances dsproxy)
        inst (first instances)]
    (assert (>= 1 (count instances))
            (str (count instances) " instances")) ; There is a single instance
    (if (= 0 (count instances))
      []
      (dsproxy-instance-to-tuples inst))))

(defn dsproxy-to-tuples-with-pred
  "Retrieve a set of tuple-pred pairs from a relational data source proxy."
  [dsproxy]
  (let [instances (.getInstances dsproxy)
        inst (first instances)]
    (assert (>= 1 (count instances))
            (str (count instances) " instances")) ; There is a single instance
    (if (= 0 (count instances))
      []
      (dsproxy-instance-to-tuples-with-pred inst)
      )))

(defn dsproxy-print
  "Print the contents of a Spicy data source proxy that are a relational
  target instance.  This is a convenience function used during the development
  of the code to generate the creates/2 atoms."
  [dsproxy]
  (let [result dsproxy
        tgt-insts (-> result (.getDataSource) (.getInstances)) ; Ground
        tgt-inst (first tgt-insts) ; Always only one?
        schema (.getLabel  tgt-inst)] ; "Target"
    (assert (= 1 (count tgt-insts))) ; Only one
    (doseq [relationSet (.getChildren tgt-inst)]  ; Each relation
      (dosync                  ; Necessary?
       (println (.getLabel relationSet))
       (doseq [atom (.getChildren relationSet)]  ; Print each atom
         (let [predicate (.getLabel atom)]
           (println (str "  " predicate))
           (doseq [attribute (.getChildren atom)] ; Print each attr
             (let [attributeName (.getLabel attribute)
                   leaves (.getChildren attribute) ; Only one?
                   leaf (.getValue (first leaves)) ; The leaf
                   leafVal ; Value of leaf depends on its type
                   (cond
                     (instance? OID leaf)
                     (str "(variable id: " leaf ") "
                          (subs  (.getSkolemString leaf) 0 30) "...")

                     (instance? INullValue leaf)
                     (str "(null value: " leaf ") ")

                     :else leaf)]
               (assert (= 1 (count leaves))) ; Always one?
               (println (str "    " attributeName " = " leafVal))
               ))))))))

;;; ====================================
;;; MappingTasks

(defn mapping-task-load
  "Read a mapping task XML file"
  [file-path]
  (.loadMappingTask (it.unibas.spicy.persistence.DAOMappingTaskLines.)
                    file-path))

(defn mapping-task-set-tgds
  "Set the active set of tgds to use with a mapping task"
  [mapping-task tgds]
  (.setLoadedTgds mapping-task tgds))

(defn mapping-task-target-dsproxy
  "Given a mapping task, return the target instance."
  [mapping-task]
  (.getTargetProxy mapping-task))

(defn mapping-task-target-instance-generate
  "Generate a target instance for a mapping task with tgds"
  [mapping-task]
  (.generateSolution (it.unibas.spicy.model.mapping.operators.GenerateSolution.)
                     mapping-task))

;;; ====================================
;;; Candidate tgds

(defn tgd-atom-count
  "Count the number of atoms in a tgd"
  [tgd mapping-task]
  (let [sview (.getSimpleSourceView tgd)
        tview (.getTargetView tgd)
        svars (.getVariables sview)
        tvars (.getVariables tview)]
    (+ (count svars) (count tvars))))

(defn tgd-candidate-atoms
  "Given a FORule, return a list of logical atoms for the body and a
  list of logical atoms for the head.  Also, remove the 'Set' at the
  end of predicates.  E.g., for a tgd that would have a logical string
  like 'id:foreach x1:aSet(x1),bSet(x1)->cSet(x1)'
  return [('a(x1)','b(x1)'), ('c(x1)')]."
  [tgd mapping-task]
  (let [ls (.toLogicalString tgd false mapping-task) ; Full logical string
        [bod hea] (map cljs/trim       ; No indents
                       (-> ls
                           (cljs/replace #"\." "") ; No period
                           (cljs/replace #"exist .+:" "") ; No existential
                           (cljs/replace #"Set([^azAZ])" "$1") ; Remove Set endings
                           (cljs/split #"->") ; Split on arrow
                           ))
        
        bod (cljs/trim (u/only (nthrest (cljs/split bod #":") 2))) ; No id, univ.
        bats (map cljs/trim (cljs/split bod #", \n")) ; Body atoms
        hats (map cljs/trim (cljs/split hea #", \n")) ; Head atoms
        ]
    [bats hats]))

(defn tgd-candidate-vmap
  "Given tgd, return a hashmap based on a tgd in logical form where
  the keys are the logical variables and the values are sorted lists
  of strings.  Each string is a predicate name and an argument
  position in which the variable appears."
  [tgd mapping-task]
  (let [[bats hats] (tgd-candidate-atoms tgd mapping-task)
        vmas (for [ats [bats hats]    ; For both body and head
                   ato ats            ; For every atom
                   ]
               (let [pre (cljs/replace ato #"\(.+\)" "") ; No args
                     vars (cljs/split  ; Split up variables within atom
                           (cljs/replace ato #".+\((.+)\)" "$1") ; No pred
                           #", ")
                     vmap (into {} (for [[i v] (map-indexed vector vars)]
                                     [v [(str pre ":" i)]])) ; Into map
                     ]
                 vmap))
        
        vma (reduce #(merge-with concat %1 %2) vmas) ; Merge all maps
        vma (into {} (for [[v l] vma] [v (sort l)])) ; Sort each list
        ]
    vma))

(defn tgd-candidate-vmap-key
  "The input is a hashmap based on a tgd in logical form where the
  keys are the logical variables and the values are sorted lists of
  strings.  Each string is a predicate name and an argument position
  in which the variable appears.  The output is a key --- a string
  combining the values in the hashmap so that equivalent tgds will
  have the same key.  This approach does not work if the tgds reuse
  relations."
  [vma]
  (cljs/join
   ";"                        ; Separate variables by ';'
   (sort                      ; Sort var key parts lexicographically
    (for [[v l] vma]          ; Discard actual variable names
      (cljs/join
       ","                    ; Separate var uses by ','
       (sort l))              ; Make sure positions are sorted 
      ))))

(defn tgd-candidate-scq-relations
  "Given a SimpleConjunctiveQuery from a tgd, list the relations used."
  [scq]
  (for [setalias (.getVariables scq)] ; One for each relation
    (let [vpe (.getBindingPathExpression setalias) 
          steps (.getPathSteps vpe)            ; E.g., [source island]
          ]
      ;; Don't need 'source' or 'target'
      (last steps))))

(defn tgd-candidate-scq-join-conditions
  "Given a SimpleConjunctiveQuery from a tgd, list the join conditions
  in the form of [(from-rel from-att) (to-rel to-att)].  Assumes a
  single join attribute."
  ;; TODO handle multiple join attributes
  [scq]
  (for [jc (.getJoinConditions scq)]      ; Each join condition
    (let [;; From
          fpe (u/only (.getFromPaths jc)) ; VariablePathExpression
          fps (.getPathSteps (.getAbsolutePath fpe)) ; Starts: schema, "Set"
          ;; To
          tpe (u/only (.getToPaths jc))
          tps (.getPathSteps (.getAbsolutePath tpe))]
      [(nthrest fps 2) ; Don't need 'source' or '...Set'
       (nthrest tps 2)])))

(defn tgd-candidates-generate
  "Generate candidate tgds"
  [mapping-task]
  (.generateCandidateTGDs
   (it.unibas.spicy.model.mapping.operators.GenerateCandidateSTTGDs.)
   mapping-task))

(defn tgd-candidates-list-print
  "Print the candidate tgds in the mapping. Takes as a list of
  candidate FORules and the associated MappingTask."
  [candidate-tgds mapping-task]
  (doseq [t
          ;; candidate-tgds
          (sort-by (fn [t] (.toShortString t)) candidate-tgds)
          ]
    (println
     ;; (.toSaveString t "" mapping-task)
     (.toLogicalString t false mapping-task)
     )))

(defn tgd-candidates-table-print
  "Print the mapping task's candidate tgds."
  [rules-ds]
  (doseq [r (vec (:rows (in/sel rules-ds :cols [:formula])))]
    (u/printlnw (:formula r) 80))
  ;; (doseq [r (in/sel rules-ds :cols [:formula])]
  ;;   (u/printlnw r 80))
  )

(defn tgd-candidates-choose-canonical-set
  "Based on rule ID from Spicy, pick a canonical subset of candidate tgds."
  [candidates]
  (let [by-new-id                         ; Map of tgds -- one for each canonical ID
        (into
         {} 
         (for [t
               ;; Reverse sort, so first (lexicographically) is kept
               (sort 
                (comparator 
                 (fn [x y] (compare (.toShortString x) (.toShortString y))))
                candidates)] 
           (let [id (.toShortString t)    ; Old ID
                 ;; Old ID parts
                 [prefix vas vat eqs] (clojure.string/split id #"_")
                 ;; New parts, with vars sorted by number
                 vasn (let [varnums (sort (rest (clojure.string/split vas #"v")))]
                        (clojure.string/join (for [n varnums] (str "v" n))))
                 vatn (let [varnums (sort (rest (clojure.string/split vat #"v")))]
                        (clojure.string/join (for [n varnums] (str "v" n))))
                 ;; New ID
                 idn (clojure.string/join "_" [prefix vasn vatn eqs])]
             [idn t])))
        chosen (vals by-new-id) ; Chosen tgds are the values in the map
        ]
    (sort
     (comparator 
      (fn [x y] (compare (.toShortString x) (.toShortString y))))  
     chosen)))

(defn tgd-candidates-to-table
  "Extract a table of rules, their formulae and atoms, from a Spicy mapping task."
  [tgd-list mapping-task]
  (let [rows
        (for [[i, tgd] (map-indexed vector tgd-list)]
          (let [rulid (.toShortString tgd)
                formula (.toLogicalString tgd mapping-task)
                atoms (tgd-atom-count tgd mapping-task)]
            [rulid formula atoms]))
        rulds (in/dataset [:r :formula :atoms] rows)]
    rulds))

(defn tgd-candidates-generate-and-print
  "Generate then print the mapping task's candidate tgds."
  [mapping-task-atom                    ; Clojure atom wrapping a MappingTask
   mapping-task-path-atom               ; Wrapped path to a MappingTask input file
   tgds-atom                            ; Wrapped list of tgds
   rules-table-atom                     ; Wrapped Dataset of tgds
   ]
  (reset! mapping-task-atom
          (mapping-task-load
           @mapping-task-path-atom))    ; Load the inputs
  (reset! tgds-atom
          (tgd-candidates-generate
           @mapping-task-atom))         ; Candidate tgds
  (reset! rules-table-atom
          (tgd-candidates-to-table
           @tgds-atom @mapping-task-atom))
  (tgd-candidates-table-print @rules-table-atom))
   
(defn tgd-candidates-in-mapping-print
  "Print the tgds in the mapping. Takes as input a DataSet containing
  in/1 atoms (post hardening), a list of candidate FORules and the
  associated MappingTask."
  [in-atom-dataset candidate-tgds mapping-task]
  (let [sel-ds (in/$where {:value 1.0} in-atom-dataset) ; True tgds in PSL mapping
        sel-ids (for [row (:rows sel-ds)] (:r row)) ; Just the tgd IDs
        sel-tgds (for [tgd candidate-tgds
                       :when (.contains sel-ids (.toShortString tgd))]
                   tgd)                     ; Selected tgds, not just ids
        ]
    (doseq [t sel-tgds]
      (println
       (.toLogicalString t false mapping-task)
       ;; (.toSaveString t "" mapping-task)
               ))))

;;; ====================================
;;; Data exchange, specialized

(defn tgd-candidate-ground-body
  "Given a fresh, temporary MappingTask and a single tgd, load the tgd
  and return a list of groundings of the body of a candidate tgd."
  [;; New, temporary MappingTask for this use
   tmp-mt
   ;; A single, selected tgd
   tgd]
  (mapping-task-set-tgds tmp-mt [tgd]) ; Load single tgd

  ;; For the single tgd, retrieve a tree of Spicy algebra operators.  The first
  ;; few layers are unimportant.  The Nest represents head of the tgd, putting
  ;; data into the target schema.  Below the nest is the result of the body's
  ;; conjunctive query.
  (let [compose (.getAlgebraTree (.getMappingData tmp-mt)) ; Top of algebra tree
        merges (.getChildren compose)       ; Normally, one?
        nests (.getChildren (first merges)) ; One for each tgd
        nest (first nests)            ; Head of the tgd; child is body
        src-ds (.getSourceProxy tmp-mt) ; For accessing source data
        src-view (.execute (first (.getChildren nest)) src-ds) ; Result of body
        src-insts (.getInstances src-view) ; Normally, one?
        src-inst (first src-insts)
        src-inst-num 0                  ; Always only one?
        src-gnds (.getChildren src-inst)    ; One for each body grounding
        ]
    (assert (= 1 (count merges)))
    (assert (= 1 (count nests)))
    (assert (= 1 (count src-insts)))
    ;; It seems that the instance num for our inputs is always 0.
    [src-gnds src-inst-num (.getGenerators nest)]))

(defn tgd-candidate-ground-head
  "Given a single grounding of the body of a tgd, translate that into
  a grounding of its head."
  [tmp-mt tgt-sch tgd src-gnd src-inst-num head-generators]
  ;; For each grounding of the tgd's body, generate a target instance
  (dosync                               ; TODO: Necessary?
   (let [node-cache (java.util.HashMap.) ; Used by head of tgd
         result-ds (it.unibas.spicy.model.datasource.DataSource.
                    it.unibas.spicy.utility.SpicyEngineConstants/TYPE_ALGEBRA_RESULT
                    tgt-sch)
         result (it.unibas.spicy.model.mapping.proxies.ConstantDataSourceProxy.
                 result-ds)             ; To receive results of tgd
         ]

     ;; Fill the temporary target instance var-by-var (rel-by-rel)
     (doseq [tgt-var (.getVariables (.getTargetView tgd))]
       (dosync
        (let [visitor
              (it.unibas.spicy.model.algebra.NestSourceViewTupleVisitorStandAlone.
               tgd src-gnd src-inst-num node-cache
               (-> head-generators (.getGeneratorsForVariable tgt-var))
               result tmp-mt)]
          (.accept tgt-sch visitor) ; Fill the target instance with info from source
          )))

     ;; (dsproxy-print result)

     ;; Turn the target instance into a new tgd for querying the target
     ;; (u/println-center "CREATES/2" "_" 45)
     (let [tgt-insts (-> result (.getDataSource) (.getInstances)) ; Ground
           tgt-inst (first tgt-insts) ; Always only one?
           ]
       (assert (= 1 (count tgt-insts))) ; Only one
       tgt-inst))))

(defn tgd-candidate-ground-head-to-query-visitor
  "Return an INode visitor for converting a target instance into a query."
  [verbose]
  (let [
        ;; OIDs are the equivalent of logical vars in relational case
        oids (atom {})                ; OIDs and the first VPE where seen
        ;; SetAliases are used to represent vars, here like atoms (preds?)
        vars (atom {})                ; Variables and their SetAliases
        vjcs (atom [])                ; VariableJoinConditions
        vscs (atom [])                ; VariableSelectionConditions
        atts (atom [])                ; VariablePathExpressions of all atts
        ]
    (reify it.unibas.spicy.model.datasource.operators.INodeVisitor

      (visitSetNode [this node]
        (let [var node                ; Represents a variable, i.e., rel?
              var-name (.getLabel var)
              path-gen (GeneratePathExpression.)
              abs-path                ; Path to this var. to go in SetAlias
              (.generatePathFromRoot path-gen var)

              ;; Check if the var has been seen before (shouldn't have?)
              ;; Create a SetAlias to represent this in the SJQ
              seen (dosync
                    (let [s (contains? @vars var)
                          ;; Does this var (rel) have at least one atom?
                          has-atoms (-> var (.getChildren) count (> 0))]
                      (when (and (not s) ; If the relation has not been seen before...
                                 has-atoms) ; ...and if it has at least one atom
                        (let [set-alias (->
                                         abs-path
                                         (VariablePathExpression.)
                                         (SetAlias.))]
                          (reset! vars (assoc @vars
                                              var {:set-alias set-alias}))))
                      s))]
          (when verbose
            (println "visitSetNode" var-name (str (:set-alias (get @vars var)))))

          ;; Loop over the atoms of this relation
          (doseq [child (.getChildren var)]
            (.accept child this)))
        )

      (visitTupleNode [this node]
        (when verbose (println "visitTupleNode" (.getLabel node))))

      (visitSequenceNode [this node]
        ;; In relational data, represents either schema or an atom
        (when verbose (println "visitSequenceNode" (.getLabel node)
                               (if (.isRoot node) "root" "")))
        (doseq [child (.getChildren node)]
          (.accept child this)))

      (visitUnionNode [this node]
        (when verbose (println "visitUnionNode" (.getLabel node))))

      (visitAttributeNode [this node]
        (let [attribute node          ; An attribute and its value
              attribute-name           ; Equiv. to an arg. name
              (.getLabel attribute)

              var-node  ; The variable (relation)
              (-> attribute
                  (.getFather)
                  (.getFather))

              vpe             ; A VariablePathExpression for this att
              (let [path-gen (GeneratePathExpression.)

                    abs-path  ; PathExpression for this att
                    (.generatePathFromRoot path-gen attribute)

                    root-node ; The variable should be child to root
                    (.getFather var-node)

                    var-set-alias ; SetAlias of the variable
                    (:set-alias (get @vars var-node))]
                ;; A VPE is a PE relative to a variable
                (.generateContextualPathForNode
                 path-gen var-set-alias abs-path root-node))

              leaves (.getChildren attribute)   ; Only one?
              leaf (.getValue (u/only leaves))   ; The leaf

              leafVal            ; Value of leaf depends on its type
              (cond
                ;; If an OID, this is equivalent to a logical relational
                ;; variable.  Keep track of the times it is seen; on all
                ;; times after the first, add a VJC to the SJC
                (instance? OID leaf)
                (let [oid leaf        ; This leaf represents an OID

                      ;; If OID not encountered before, remember this VPE
                      seen
                      (dosync
                       (let [s (contains? @oids oid)]
                         (when (not s)
                           (reset! oids (assoc
                                         @oids oid {:first-vpe ; VPE of 1st-seen
                                                    vpe})))
                         s))]

                  ;; Add a VJC every time it is true this OID seen previously
                  (when seen
                    (let [first-vpe   ; VariablePathExpression of prev-seen OID
                          (:first-vpe (get @oids oid))

                          monodirectional false ; Join is bidirectional?
                          mandatory true ; Join is mandatory?
                          from-oid first-vpe ; From attr where OID first seen
                          to-oid vpe  ; ...to this attr
                          vjc         ; The new join condition
                          (VariableJoinCondition.
                           from-oid to-oid monodirectional mandatory)]
                      ;; Add new join condition to the list
                      (swap! vjcs conj vjc)))

                  ;; Increment the number of NULLs seen
                  ;; (swap! vars
                  ;;        (fn [v]
                  ;;          (let [new-count (inc (get-in v [var-node :ns] 0))]
                  ;;            (assoc-in v [var-node :ns] new-count))))

                  (str "(oid: " leaf ") "
                       (subs  (.getSkolemString leaf) 0 10) "..." vpe))

                (instance? INullValue leaf)
                (do
                  ;; Increment the number of NULLs seen
                  (swap! vars
                         (fn [v]
                           (let [new-count (inc (get-in v [var-node :ns] 0))]
                             (assoc-in v [var-node :ns] new-count))))
                  ;; (u/dbg (get-in @vars [var-node :ns]))
                  (str "(null value: " leaf ") ")) ; To print, if null

                ;; Only other possibility is a ground attribute value?
                :else
                (let [
                      path-gen (GeneratePathExpression.)

                      abs-path  ; PathExpression for this att
                      (.generatePathFromRoot path-gen attribute)

                      root-node ; The variable should be child to root
                      (.getFather var-node)

                      var-set-alias ; SetAlias of the variable
                      (:set-alias (get @vars var-node))

                      ;; Path expression to use in condition
                      vpe (.generateContextualPathForNode
                           path-gen var-set-alias abs-path root-node)

                      vsc      ; Selection condition
                      (-> (StringBuilder.)
                          (.append vpe)
                          (.append " == ")
                          (.append "\"")
                          (.append leaf)
                          (.append "\"")
                          (.toString)
                          (it.unibas.spicy.model.expressions.Expression.) ; JEP expression
                          (VariableSelectionCondition.))
                      ]

                  (swap! vscs conj vsc) ; Add this VSC

                  ;; Just print the value
                  leaf)
                )
              ]
          (assert (= 1 (count leaves))) ; Always one?
          (when verbose (println "visitAttributeNode"
                                 attribute-name
                                 " = " leafVal))

          ;; Record the number of arguments in a relation
          (swap! vars
                 (fn [v]
                   (let [new-count (inc (get-in v [var-node :as] 0))]
                     (assoc-in v [var-node :as] new-count))))

          ;; Add to list of all atts
          (swap! atts conj vpe)))

      (visitMetadataNode [this node]
        (when verbose (println "visitMetadataNode" (.getLabel node))))

      (visitLeafNode [this node]
        (when verbose (println "visitLeafNode" (.getLabel node))))

      (getResult [this]
        ;; This returns what is needed to form a query based on this instance
        (let [variables               ; All atoms
              (for [v (vals @vars)] (get v :set-alias))
              join-conditions @vjcs   ; Joins between some atoms
              sel-conditions @vscs    ; Ground att val conditions on some
              ]
          [join-conditions variables sel-conditions @atts @vars]))
      )))

(defn tgd-candidate-ground-head-to-creates-table
  "Given a per-grounding instance of the target schema based on some tgd, generate creates atoms
  by converting the instance to a query then running that query on the full instance."
  [tgd
   target-instance
   t-t-mapping-task-path]

  (let [verbose false
        vis (tgd-candidate-ground-head-to-query-visitor verbose)
        ti target-instance]
    ;; (u/println-center "instance" "_" 60)
    (.accept ti vis)                  ; Traverse target instance
    (let [[vjcs vars vscs atts vars-info] (.getResult vis) ; Get results of visitor
          scq (SimpleConjunctiveQuery.) ; Initially empty
          scq-t (SimpleConjunctiveQuery.)  ; For the target query
          var-info-ds (in/dataset [:rel :ns :as]
                                  (for [[var info] (seq vars-info)]
                                    [(-> var (.getChildren) first (.getLabel))
                                     (:ns info 0) (:as info)]))
          ]
      ;; (u/dbg var-info-ds)
      ;; (doseq [[var info] (seq vars-info)]
      ;;   (println (.getLabel var) (:ns info))
      ;;   )
      ;; (u/dbg vars-info)
      (doseq [var vars]
        ;; Add variable to the query
        (.addVariable scq var)
        (.addVariable scq-t var))
      (doseq [vjc vjcs]
        ;; Add join to the query
        (.addJoinCondition scq vjc))
      (doseq [vsc vscs]
        ;; Add join to the query
        (.addSelectionCondition scq vsc))

      (let [
            source-view scq           ; Body of tgd
            target-view scq-t         ; Head ot tgd
            tt-tgd (FORule. source-view target-view) ; New tgd
            ]
        ;; Add variable correspondences to copy results to a new target inst.
        (doseq [att atts]
          (let [var-corr              ; A correspondence to itself
                (VariableCorrespondence. att att)]
            (.addCoveredCorrespondence tt-tgd var-corr)))

        ;; (u/println-center "TT-TGD" "_" 40)
        ;; (println (str tt-tgd))

        ;; Run tgd
        (let [tmp-mt (mapping-task-load t-t-mapping-task-path)]
          (mapping-task-set-tgds tmp-mt [tt-tgd]) ; Load single tgd

          ;; (u/println-center "TT-TGD" "_" 40)
          ;; (println (.toLogicalString tt-tgd tmp-mt))
          (let [rulid (.toShortString tgd)
                dsprox (mapping-task-target-instance-generate tmp-mt)
                tups (dsproxy-to-tuples-with-pred dsprox)
                createsds (in/dataset [:t :rel] tups)
                createsds (in/add-column :r
                                         (repeat (in/nrow createsds) rulid)
                                         createsds)
                createsds (iu/dataset-join-natural createsds var-info-ds)
                ]

            ;; (u/println-center "TGT-INST" "_" 40)
            ;; (u/dbg createsds)
            createsds
            ))))))

(defn tgd-candidates-src-gnd-to-creates-atoms
  "Given a grounding of the body of a tgd on the source instance,
  create a table of creates atoms. "
  [mapping-task-tt-path tmp-mt tgt-sch tgd rulid src-inst-num head-generators src-gnd]
  (let [tgt-inst (tgd-candidate-ground-head
                  tmp-mt tgt-sch tgd src-gnd src-inst-num head-generators)

        ;; Tuples after data exchange
        post-exchange-tuples
        (dsproxy-instance-to-tuples-with-pred tgt-inst)

        ;; Convert tuples into creates info
        post-exchange-creates
        (->> post-exchange-tuples
             (in/dataset [:t :rel])
             (in/add-column :r
                            (repeat (count post-exchange-tuples) rulid))
             (in/add-column :ns ; Nulls are immaterial, since literally created
                            (repeat (count post-exchange-tuples) 0))
             (in/add-column :as ; Dummy arity; we don't need actual here
                            (repeat (count post-exchange-tuples) -1))
             )

        ;; Create info after querying the target instance
        post-query-creates (tgd-candidate-ground-head-to-creates-table
                            tgd tgt-inst mapping-task-tt-path)

        ;; TODO Add here only for tuples after query, or just copy from ttup/1?
        post-query-creates (in/add-column
                            :ac
                            (repeat (in/nrow post-query-creates) 1) post-query-creates)
        post-query-creates
        (in/reorder-columns post-query-creates[:t :r :rel :ns :as :ac])

        ;; Predicates (rels) in post-query results indicate which tuples are accepted
        post-query-preds
        (iu/dataset-drop-duplicates (in/sel post-query-creates :cols [:rel]))
        post-query-preds
        (in/add-column :pq (repeat (in/nrow post-query-preds) 1) post-query-preds)

        ;; Column :ac indicating whether a target tuple is accepted
        post-exchange-creates (in/$join [:rel :rel] post-query-preds post-exchange-creates)
        post-exchange-creates (in/add-derived-column :ac [:pq] (fn [p] (if (nil? p) 0 1))
                                                     post-exchange-creates)
        post-exchange-creates (in/sel post-exchange-creates :except-cols :pq)
        post-exchange-creates
        (in/reorder-columns post-exchange-creates[:t :r :rel :ns :as :ac])

        ;; Combine everything into a single table
        createsds (in/conj-rows
                   post-exchange-creates
                   post-query-creates)
        ]
    createsds))

(defn tgd-candidates-creates-info-table
  "Given a list of candidate tgds, calculate a table of information
  about what tuples they generate.  This information is sufficient to
  ground soft creates/2 and 'generous' accepted/1 atoms."
  [candidate-tgds mapping-task-path mapping-task-tt-path]  
  (let [creates-dss
        ;; A table of creates info for each grounding of each tgd
        (for [tgd
              candidate-tgds
              ;; [(first candidate-tgds)]
              ]
          (let [ ;; Temporary MappingTask for one tgd
                tmp-mt (mapping-task-load mapping-task-path)

                ;; Target schema, for result
                tgt-sch (.clone (.getIntermediateSchema
                                 (.getTargetProxy tmp-mt)))

                ;; List of groundings of the body of the tgd
                [src-gnds src-inst-num head-generators]
                (tgd-candidate-ground-body tmp-mt tgd)

                rulid (.toShortString tgd)]
            
            (log/infof "tgd::%s createsds:: ::starting" rulid)
            (log/infof "tgd::%s createsds:: ::groundings %d" rulid
                       (count src-gnds))
            ;; A table of creates atoms for each grounding of the tgd body
            (for [src-gnd
                  src-gnds
                  ;; [(first src-gnds)]
                  ;; (take 3 src-gnds)
                  ]
              (do 
                ;; (u/dbgsym src-gnd)
                (tgd-candidates-src-gnd-to-creates-atoms 
                 mapping-task-tt-path tmp-mt tgt-sch tgd rulid 
                 src-inst-num head-generators src-gnd)))))]

    ;; (u/dbgsym creates-dss)
    ;; Combine into a single table of creates info
    (->
     (for [d (flatten  creates-dss)] (vec (:rows d))) ; Take all rows of all tables
     flatten                        ; Make into one flat list
     in/to-dataset                  ; Convert that list into a dataset
     (in/reorder-columns [:t :r :rel :ns :as :ac]) ; Reorder cols to fit predicate
     ;; Truth value is the percent of args that are non-null
     (->> (in/add-derived-column :value [:ns :as] (fn [n a] (/ (- a n) a)))))))

;;; ====================================
;;; Miscellaneous

(defn string-cleaned 
  "If a string contains characters that should not appear in a 
  database, replace them with safe versions."
  [string]
  (clojure.string/replace string #"'" "::apos::"))

(defn dataset-with-shortened-tuples 
  "Given a dataset containing tuples (by default, in a column
  named :t), return a version of the dataset with the tuples in that
  column shortened so they can be used as IDs in a database, e.g., as
  used by PSL."
  ([dataset col-name]
   (let [old (in/sel dataset :cols col-name)
         new (for [t old] (u/string-shortened-with-hash 
                           t 
                           ;; Amalgam causes error with 255, as of Jan
                           ;; 2017?  Or is this an issue with a string
                           ;; containing an apostrophe?
                           ;; 200
                           ))]
     (in/replace-column col-name new dataset)))
  ;; With default column name
  ([dataset] (dataset-with-shortened-tuples dataset :t)))

(defn dataset-with-clean-string-tuples 
  "Given a dataset containing tuples (by default, in a column
  named :t), return a version of the dataset with the tuples in that
  column cleaned of any unacceptable characters, e.g., for the PSL
  database."
  ([dataset col-name]
   (let [old (in/sel dataset :cols col-name)
         new (for [t old] (string-cleaned t))]
     (in/replace-column col-name new dataset)))
  ;; With default column name
  ([dataset] (dataset-with-clean-string-tuples dataset :t)))

(defn dataset-with-clean-and-shortened-tuples
  "Given a dataset containing tuples that will be put in a database,
  e.g., PSL, first make sure there are no characters that will cause a
  problem, then shorten the tuples.  This is useful if the database
  has a problem holding the tuples."
  [dataset]
  (dataset-with-shortened-tuples
   (dataset-with-clean-string-tuples
    dataset)))
