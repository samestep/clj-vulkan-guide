(ns clj-vulkan-guide.part1
  (:require [clojure.reflect :as reflect]
            [clojure.string :as str]
            [org.baznex.imports :as imports])
  (:import (java.io PrintStream)
           (org.apache.commons.io.output WriterOutputStream)
           (org.lwjgl BufferUtils
                      Version)
           (org.lwjgl.glfw GLFWErrorCallback
                           GLFWKeyCallback)
           (org.lwjgl.opengl GL)))

(defmacro import-static-all
  "Creates a private var or macro for every public static field or method,
  respectively, in every class in classes. Every class symbol must be fully
  qualified."
  [& classes]
  `(do
     ~@(for [c classes]
         `(imports/import-static
           ~c
           ~@(map :name (:members (reflect/type-reflect c)))))))

(import-static-all org.lwjgl.glfw.Callbacks
                   org.lwjgl.glfw.GLFW
                   org.lwjgl.opengl.GL11
                   org.lwjgl.system.MemoryUtil)

(defn hello-world
  "Prints the LWJGL version and creates a blank window that closes when the
  Escape key is pressed."
  []
  (println (str "Hello LWJGL " (Version/getVersion) "!"))
  (try
    (.set (GLFWErrorCallback/createPrint  System/err))
    (when-not (glfwInit)
      (throw (IllegalStateException. "Unable to initialize GLFW")))
    (glfwDefaultWindowHints)
    (glfwWindowHint GLFW_VISIBLE GLFW_FALSE)
    (glfwWindowHint GLFW_RESIZABLE GLFW_TRUE)
    (let [width 300
          height 300
          window (glfwCreateWindow width height "Hello World!" NULL NULL)]
      (when (= window NULL)
        (throw (RuntimeException. "Failed to create the GLFW window")))
      (glfwSetKeyCallback
       window
       (proxy [GLFWKeyCallback] []
         (invoke [window keycode _ action _]
           (when (= [keycode action] [GLFW_KEY_ESCAPE GLFW_RELEASE])
             (glfwSetWindowShouldClose window true)))))
      (let [vidmode (glfwGetVideoMode (glfwGetPrimaryMonitor))]
        (glfwSetWindowPos
         window
         (-> (.width vidmode) (- width) (/ 2))
         (-> (.height vidmode) (- height) (/ 2))))
      (glfwMakeContextCurrent window)
      (glfwSwapInterval 1)
      (glfwShowWindow window)
      (GL/createCapabilities)
      (glClearColor 1 0 0 0)
      (while (not (glfwWindowShouldClose window))
        (glClear (bit-or GL_COLOR_BUFFER_BIT GL_DEPTH_BUFFER_BIT))
        (glfwSwapBuffers window)
        (glfwPollEvents))
      (glfwFreeCallbacks window)
      (glfwDestroyWindow window))
    (finally
      (glfwTerminate)
      (.free (glfwSetErrorCallback nil)))))

(defn lwjgl-version
  "Returns a map from :type, :major, :minor, and :revision to the build state,
  major version number, minor version number, and revision number of LWJGL,
  respectively."
  []
  {:type (keyword (str/lower-case Version/BUILD_TYPE))
   :major Version/VERSION_MAJOR
   :minor Version/VERSION_MINOR
   :revision Version/VERSION_REVISION})

(defn glfw-version
  "Returns a map from :major, :minor, and :revision to the major version number,
  minor version number, and revision number of GLFW, respectively."
  []
  (let [[major minor revision :as arrays] (repeatedly 3 #(int-array 1))]
    (glfwGetVersion major minor revision)
    (zipmap [:major :minor :revision] (map #(get % 0) arrays))))

(defn init-error
  "Attempts to call a GLFW function before initializing the library, while the
  error callback is set to print to *err*."
  []
  (.set (GLFWErrorCallback/createPrint
         (-> *err* WriterOutputStream. PrintStream.)))
  (glfwDefaultWindowHints)
  (.free (glfwSetErrorCallback nil)))

(defn window-error
  "Attempts to create a 0x0 GLFW window. Returns true if NULL is received."
  []
  (glfwInit)
  (let [result (glfwCreateWindow 0 0 "" NULL NULL)]
    (glfwTerminate)
    (= result NULL)))

(defn empty-window
  "Creates an empty window."
  [width height title]
  (glfwInit)
  (let [window (glfwCreateWindow width height title NULL NULL)]
    (while (not (glfwWindowShouldClose window))
      (glfwWaitEvents))
    (glfwDestroyWindow window))
  (glfwTerminate))

(defn utf8-buffer
  "Returns a direct ByteBuffer with a null-terminated UTF-8 encoding of s."
  [s]
  (let [buffer (BufferUtils/createByteBuffer (memLengthUTF8 s true))]
    (memUTF8 s true buffer)
    buffer))
