(defproject org.clojars.alexmemory/clj-spicy "0.1.1-SNAPSHOT"
  :description "Wrapper for ++Spicy. http://www.db.unibas.it/projects/spicy/"
  :url "https://github.com/alexmemory/spicy"
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [org.clojars.alexmemory/clj-util "0.1.0"]
                 [org.clojure/tools.logging "0.3.1"]

                 [commons-logging/commons-logging "1.1.3"]
                 [org.jdom/jdom "1.1.3"]
                 [org.antlr/antlr "3.1.3"]
                 [org.apache.ibatis/ibatis-sqlmap "2.3.4.726"]

                 [org.clojars.alexmemory/incanter-util "0.1.0"]
                 [me.raynes/fs "1.4.6"] ; For globbing, etc.

                 [net.sf.saxon/Saxon-HE "9.5.1-8"]
                 [net.sf.saxon/saxon-xqj "9.x"]
                 [xerces/xercesImpl "2.11.0"]
                 ]
  :aot :all
  :pedantic :warn                       ; Class path warnings
  :java-source-paths ["../src"          ; spicyEngine
                      "../../jep-2.4.1/src" ; Customized jep?
                      ]
  :repositories [;; For saxon-xqj
                 ["WSO2" "http://dist.wso2.org/maven2/"]])

;; never do this
;; 20190814 with lein 2.8.1, a http repo here uses HTTP, which is no longer allowed.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))
