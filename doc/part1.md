# Part 1: LWJGL and GLFW

Currently, the only JVM library that provides Vulkan bindings is [LWJGL][], so
the first thing you'll need to do is add LWJGL to your dependencies. I'm going
to use [Boot][] instead of [Leiningen][], because it has a slick way to add
dependencies at runtime:

```clojure
(require '[boot.core :as boot])

(let [lwjgl-version "3.0.0"]
  (boot/merge-env!
   :dependencies
   [['org.lwjgl/lwjgl lwjgl-version]
    ['org.lwjgl/lwjgl-platform lwjgl-version :classifier "natives-linux"]
    ['org.lwjgl/lwjgl-platform lwjgl-version :classifier "natives-windows"]]))
```

I didn't include the OS X natives because OS X doesn't currently support Vulkan.

LWJGL also provides bindings to [GLFW][], a utility library that we'll be using
for windowing and input. But before we get into that, we need to talk about
syntax.

## Static import

LWJGL has several classes, such as [`MemoryUtil`][memoryutil],
[`GLFW`][glfw javadoc], and [`VK10`][vk10], that contain many static fields and
methods whose names are prefixed in such a way that writing both the class name
and the prefix is redundant. For these members, Java's static import feature is
quite beneficial for both writability and readability.

Clojure, however, does not have static imports. Not built-in, at least. But with
the power of macros, we can add that feature ourselves! As it turns out, there's
already a library called [`imports`][imports] that does part of what we need. Go
ahead and add it to your dependencies:

```clojure
(boot/merge-env! :dependencies '[[org.baznex/imports "1.4.0"]])
```

The macro we want is [`org.baznex.imports/import-static`][import-static]:

```clojure
(require '[org.baznex.imports :as imports])

(imports/import-static org.lwjgl.system.MemoryUtil NULL)

NULL
;=> 0
```

Great! One thing to keep in mind is that this macro was written before
[Clojure 1.3 added `^:const`][const], so access to static fields will be slower
than [Clojure's built-in Java interop mechanisms][interop]. If this turns out to
be a problem for you, you could modify the `import-static` macro to add
`^:const` to the field vars it creates.

Now, what we really need is a way to import *all* the static fields and methods
in a class. One way to do this is to assemble an `import-static` form with all
the public fields and methods of a class. The `import-static` macro will
automatically weed out non-static members, making our job easier:

```clojure
(defmacro import-static-all [& classes]
  {:pre [(every? symbol? classes)
         (not-any? namespace classes)]}
  `(do
     ~@(for [csym classes
             :let [c (Class/forName (name csym))]]
         `(imports/import-static
           ~csym
           ~@(map (comp symbol (memfn getName))
                  (concat (.getFields c) (.getMethods c)))))))
```

This solution isn't great, but it's good enough for our purposes.

## Example

Now let's translate [LWJGL's "Hello World!" example][example] into Clojure:

```clojure
(import (org.lwjgl Version)
        (org.lwjgl.glfw GLFWErrorCallback
                        GLFWKeyCallback)
        (org.lwjgl.opengl GL))

(import-static-all org.lwjgl.glfw.Callbacks
                   org.lwjgl.glfw.GLFW
                   org.lwjgl.opengl.GL11
                   org.lwjgl.system.MemoryUtil)

(defn hello-world []
  (println (str "Hello LWJGL " (Version/getVersion) "!"))
  (try
    (.set (GLFWErrorCallback/createPrint System/err))
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
```

If you run that example, you should see "Hello LWJGL 3.0.0 build 90!" printed in
the REPL and get a nice square red window titled "Hello World!"

```clojure
(hello-world)
```

Hit Escape in the window to close it and terminate GLFW. This example
demonstrates some of the things you'll need to keep in mind when using LWJGL and
GLFW, so let's go through it step by step.

## Version

```clojure
(Version/getVersion)
;=> "3.0.0 build 90"
```

The [`Version`][version] class is used, oddly enough, to query the LWJGL
version. Here we got a string representation of the version using
[`getVersion`][getversion], but it offers a more programmatic API as well:

```clojure
(require '[clojure.string :as str])

(defn lwjgl-version []
  {:type (keyword (str/lower-case Version/BUILD_TYPE))
   :major Version/VERSION_MAJOR
   :minor Version/VERSION_MINOR
   :revision Version/VERSION_REVISION})

(lwjgl-version)
;=> {:type :stable, :major 3, :minor 0, :revision 0}
```

There are several ways to get the GLFW version. You can get it as a string:

```clojure
(first (str/split (glfwGetVersionString) #" "))
;=> "3.2.0"
```

You can also get a more structured representation through a few static fields:

```clojure
(defn glfw-version []
  {:major GLFW_VERSION_MAJOR
   :minor GLFW_VERSION_MINOR
   :revision GLFW_VERSION_REVISION})

(glfw-version)
;=> {:major 3, :minor 2, :revision 0}
```

GLFW also provides a way to query its version dynamically using the
[`glfwGetVersion`][glfwgetversion] function. But if you look at the signature of
that function, you'll see that it takes three *pointers*. Java (and therefore
Clojure) has no native concept of pointers. So how is this function translated
into LWJGL?

## Pointers

For various reasons which are explained pretty well [in the wiki][faq], LWJGL
maps pointers to direct [`java.nio` buffers][buffer]. In this case, the native
`glfwGetVersion` function takes `int*`s, so the
[LWJGL method][glfwgetversion buffer] takes [`IntBuffer`][intbuffer]s. LWJGL
provides a handy [`BufferUtils`][bufferutils] class to allocate direct buffers:

```clojure
(import (org.lwjgl BufferUtils))

(defn glfw-version []
  (let [[major minor revision :as buffers]
        (repeatedly 3 #(BufferUtils/createIntBuffer 1))]
    (glfwGetVersion major minor revision)
    (zipmap [:major :minor :revision] (map #(.get % 0) buffers))))

(glfw-version)
;=> {:major 3, :minor 2, :revision 0}
```

The memory allocated by these buffers will be automatically freed after the
buffer itself is garbage collected. If you're feeling adventurous, you can also
use the lower-level memory management methods in [`MemoryUtil`][memoryutil],
which can be used to allocate memory that must be freed explicitly:

```clojure
(defn glfw-version []
  (let [[major minor revision :as buffers] (repeatedly 3 #(memAllocInt 1))]
    (glfwGetVersion major minor revision)
    (let [result (zipmap [:major :minor :revision] (map #(.get % 0) buffers))]
      (run! #(memFree %) buffers)
      result)))

(glfw-version)
;=> {:major 3, :minor 2, :revision 0}
```

Finally, for some functions
([including `glfwGetVersion`][glfwgetversion array]), LWJGL provides a method
that takes arrays instead of buffers:

```clojure
(defn glfw-version []
  (let [[major minor revision :as arrays] (repeatedly 3 #(int-array 1))]
    (glfwGetVersion major minor revision)
    (zipmap [:major :minor :revision] (map #(get % 0) arrays))))

(glfw-version)
;=> {:major 3, :minor 2, :revision 0}
```

## Error callback

For each required type of callback, LWJGL provides a functional interface with
an `invoke` method of the appropriate signature (e.g.
[`GLFWErrorCallbackI`][glfwerrorcallbacki],
[`GLFWKeyCallbackI`][glfwkeycallbacki]) and a utility class that implements that
interface (e.g. [`GLFWErrorCallback`][glfwerrorcallback],
[`GLFWKeyCallback`][glfwkeycallback]). In our "Hello World!" example, we used
the [`createPrint`][createprint] convenience method to create a GLFW error
callback that prints errors to [`System/err`][system/err]. Incidentally, if
you're using [CIDER][], [this won't work properly][println question], because
CIDER redirects [`*err*`][err] but not `System/err`. In order to see GLFW error
messages in the REPL, we must instead create a [`PrintStream`][printstream]
object that points to `*err*`, which is a [`PrintWriter`][printwriter].
Interestingly, Java doesn't have a built-in way to do that, but
[Apache Commons IO][commons io] provides a
[`WriterOutputStream`][writeroutputstream] class that we can use:

```clojure
(boot/merge-env! :dependencies '[[commons-io "2.5"]])

(import (java.io PrintStream)
        (org.apache.commons.io.output WriterOutputStream))

(defn init-error []
  (.set (GLFWErrorCallback/createPrint
         (-> *err* WriterOutputStream. PrintStream.)))
  (glfwDefaultWindowHints)
  (.free (glfwSetErrorCallback nil)))
```

The [`set`][glfwerrorcallback set] method that we call here is equivalent to the
static [`glfwSetErrorCallback`][glfwseterrorcallback] method. Then, after we
call `glfwDefaultWindowHints`, we take advantage of the fact that
`glfwSetErrorCallback` returns the previously set callback, allowing us to
easily [`free`][free] it. Always remember to free your callbacks!

If you call `init-error`, you should see a
[`GLFW_NOT_INITIALIZED`][glfw_not_initialized] error printed in the REPL:

```clojure
(init-error)
```

Note that no exceptions were thrown. If you want GLFW errors to be thrown as
exceptions, you could use the [`createThrow`][createthrow] method instead, but
obviously you wouldn't want to use either of these approaches in a real program.
In the future, I'm mostly going to ignore this sort of error handling, so you'll
need to keep that in mind and handle it yourself where necessary.

## GLFW initialization and termination

If you want to do anything interesting with GLFW, you first need to initialize
the library using [`glfwInit`][glfwinit], which will either return `true` (if it
succeeds) or `false` (if it fails). Since you're allowed to call
`glfwSetErrorCallback` before `glfwInit`, you can use that callback to get more
information about the error. In the event that initialization succeeds, you must
call [`glfwTerminate`][glfwterminate] before your program exits.

Some GLFW functions (such as `glfwGetVersion` and `glfwGetVersionString`) can be
called from any thread, but most (including `glfwInit` and `glfwTerminate`) must
only be called from the main thread. By "main thread", I mean the thread from
which the JVM initially called `main`. So if you've been following this guide in
your favorite REPL, chances are good that you haven't been running any of this
code from the main thread.

Why, then, has everything worked so far? Well, remember back at the beginning
when I pointed out that OS X doesn't currently support Vulkan? As it turns out,
this requirement that GLFW functions be called from the main thread only exists
because of OS X. In Windows, you need to call them from *the same* thread (for
the lifetime of a particular initialization of the GLFW library), but that
thread doesn't need to be the main thread. In Linux, of course, you can do
anything from any thread.

Now, obviously, violating the GLFW API like this is a terrible idea. The only
reason I'm doing it is that neither Leiningen nor Boot gives you control over
the main thread. So for now, you can pretend that there is no concept of a
special main thread, but when you write and package your app, be sure that the
thread you use for GLFW is the main thread.

## Window creation and destruction

You can create a window with [`glfwCreateWindow`][glfwcreatewindow] and destroy
it with [`glfwDestroyWindow`][glfwdestroywindow]. Because the
[`GLFWwindow`][glfwwindow] struct is opaque, LWJGL represents a `GLFWwindow*` as
just a `long`. This works fine because the parameter or return type of
everything in GLFW that deals with windows is `GLFWwindow*`, so you can mostly
ignore the type of a GLFW window.

If window creation fails for whatever reason, `glfwCreateWindow` will (in
addition to triggering your error callback, of course) return [`NULL`][null]:

```clojure
(defn window-error []
  (glfwInit)
  (let [result (glfwCreateWindow 0 0 "" NULL NULL)]
    (glfwTerminate)
    (= result NULL)))

(window-error)
;=> true
```

I didn't destroy the window here, because it failed to be created. Actually, I
wouldn't have to explicitly destroy it even if it succeeded because it would be
destroyed when I called `glfwTerminate`, but you should probably do so anyway.

As I'm sure you've guessed by now, the first two arguments to `glfwCreateWindow`
are the width and height of the new window. They must be greater than zero;
that's why this call returned `NULL`.

The third argument is the window title, which supports Unicode! Hurrah!

```clojure
(defn empty-window [width height title]
  (glfwInit)
  (let [window (glfwCreateWindow width height title NULL NULL)]
    (while (not (glfwWindowShouldClose window))
      (glfwWaitEvents))
    (glfwDestroyWindow window))
  (glfwTerminate))

(empty-window 640 480 "π or τ?")
```

Remember that GLFW is a C library, which means that it uses null-terminated
strings. How, then, can we pass a Java [`String`][string] to `glfwCreateWindow`?
The answer is that, similar to the array version of `glfwGetVersion`, LWJGL
provides a [sugared `glfwCreateWindow`][glfwcreatewindow string], which takes a
[`CharSequence`][charsequence], on top of the
[original `glfwCreateWindow`][glfwcreatewindow buffer], which takes a
[`ByteBuffer`][bytebuffer]. If you want, you can create the null-terminated
UTF-8 string yourself (using the utility methods
[`memLengthUTF8`][memlengthutf8] and [`memUTF8`][memutf8]) and pass it directly:

```clojure
(defn utf8-buffer [s]
  (let [buffer (BufferUtils/createByteBuffer (memLengthUTF8 s true))]
    (memUTF8 s true buffer)
    buffer))

(empty-window 640 480 (utf8-buffer "0123456789↊↋"))
```

And yes, I just abused Clojure's type dynamicity. Sorry.

If you want a full-screen window, you can pass a pointer to a monitor as the
fourth parameter to `glfwCreateWindow`. All our windows so far have been
windowed, so we've passed `NULL`. The fifth parameter is used for sharing
resources between OpenGL contexts, which we don't care about because we're going
to be using Vulkan.

You can further configure your window by setting window creation hints by
calling [`glfwWindowHint`][glfwwindowhint] with each of the hints you want to
set, *before* you call `glfwCreateWindow`. Each hint has a default value, and
all hints are reset to their default values when `glfwInit` or
[`glfwDefaultWindowHints`][glfwdefaultwindowhints] is called. All the hints and
their supported and default values are explained in detail
[in the GLFW documentation][hints]. Note that, in the "Hello World!" example,
the call to `glfwDefaultWindowHints` was unnecessary, because we had just reset
all the window hints using `glfwInit`. Explicitly setting `GLFW_RESIZABLE` to
`GLFW_TRUE` was also unnecessary, because that's its default value.

After we created the window, we got the [current video mode][glfwgetvideomode]
from the [primary monitor][glfwgetprimarymonitor] and used it to
[set our window's position][glfwsetwindowpos]. The only interesting thing about
this is that, unlike some structs like `GLFWwindow`, the
[`GLFWvidmode`][glfwvidmode] struct is transparent and is represented in LWJGL
by the corresponding [`GLFWVidMode`][glfwvidmode javadoc] class.

## Events

The last bit of customization we did to our window was to give it a key
callback. In this case, there isn't a convenience method we can use to generate
our keyback, so we need to extend the `GLFWKeyCallback` class using
[`proxy`][proxy]. Our callback doesn't need to act as anything else, so the only
thing we put in the vector of classes and interfaces is `GLFWKeyCallback`
itself. We don't need to pass any arguments to the superclass constructor, so
the second vector is empty. The method you need to override for any callback is
called `invoke`.

In this case, we don't care about the scancode and mods, so I followed the
convention of using underscores for those arguments. All this callback does is
check whether a key event was the user releasing the Escape key, and if it was,
mark our window to be closed. Note that this doesn't actually close the window;
rather, we check the flag in every iteration of our loop, and if it's set, we
exit the loop, [free our callbacks][glfwfreecallbacks], and destroy our window.

When the user performs that sort of action on a window, the action gets stored
in a queue somewhere. Calling [`glfwPollEvents`][glfwpollevents] goes through
all the outstanding events events in that queue, processes them, and calls any
callbacks that you've set if necessary. It's important that event processing and
callback handling work this way, because, like most of GLFW, they need to happen
on the main thread.

In our `empty-window` function, we called [`glfwWaitEvents`][glfwwaitevents],
which works almost exactly like `glfwPollEvents`. The difference is that, if the
event queue is empty, `glfwPollEvents` will return immediately, whereas
`glfwWaitEvents` will put the thread to sleep until at least one event is
available. So if all you're doing in the GLFW thread is responding to events,
`glfwWaitEvents` can help you avoid wasting one of your cores. There's also
[`glfwWaitEventsTimeout`][glfwwaiteventstimeout], which automatically wakes up
the thread and returns after a specified amount of time has passed.

---

And that's it! All the rest of the example code is OpenGL stuff, which we don't
care about because we're going to be using Vulkan. If you want to learn more
about GLFW, the [docs][glfw docs] are very clear and comprehensive.

[boot]: http://boot-clj.com/
[buffer]: https://docs.oracle.com/javase/8/docs/api/java/nio/Buffer.html
[bufferutils]: http://javadoc.lwjgl.org/org/lwjgl/BufferUtils.html
[bytebuffer]: http://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html
[charsequence]: http://docs.oracle.com/javase/8/docs/api/java/lang/CharSequence.html
[cider]: https://github.com/clojure-emacs/cider
[commons io]: https://commons.apache.org/proper/commons-io/
[const]: https://github.com/clojure/clojure/blob/master/changes.md#215-const-defs
[createprint]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWErrorCallback.html#createPrint-java.io.PrintStream-
[createthrow]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWErrorCallback.html#createThrow--
[err]: http://clojuredocs.org/clojure.core/*err*
[example]: https://www.lwjgl.org/guide
[faq]: https://github.com/LWJGL/lwjgl3-wiki/wiki/1.5.-Bindings-FAQ
[free]: http://javadoc.lwjgl.org/org/lwjgl/system/Callback.html#free--
[getversion]: http://javadoc.lwjgl.org/org/lwjgl/Version.html#getVersion--
[glfw]: http://www.glfw.org/
[glfw docs]: http://www.glfw.org/docs/latest/index.html
[glfw javadoc]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFW.html
[glfw_not_initialized]: http://www.glfw.org/docs/latest/group__errors.html#ga2374ee02c177f12e1fa76ff3ed15e14a
[glfwcreatewindow]: http://www.glfw.org/docs/latest/group__window.html#ga5c336fddf2cbb5b92f65f10fb6043344
[glfwcreatewindow buffer]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFW.html#glfwCreateWindow-int-int-java.nio.ByteBuffer-long-long-
[glfwcreatewindow string]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFW.html#glfwCreateWindow-int-int-java.lang.CharSequence-long-long-
[glfwdefaultwindowhints]: http://www.glfw.org/docs/latest/group__window.html#gaa77c4898dfb83344a6b4f76aa16b9a4a
[glfwdestroywindow]: http://www.glfw.org/docs/latest/group__window.html#gacdf43e51376051d2c091662e9fe3d7b2
[glfwerrorcallback]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWErrorCallback.html
[glfwerrorcallback set]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWErrorCallback.html#set--
[glfwerrorcallbacki]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWErrorCallbackI.html
[glfwfreecallbacks]: http://javadoc.lwjgl.org/org/lwjgl/glfw/Callbacks.html#glfwFreeCallbacks-long-
[glfwgetprimarymonitor]: http://www.glfw.org/docs/latest/group__monitor.html#ga721867d84c6d18d6790d64d2847ca0b1
[glfwgetversion]: http://www.glfw.org/docs/latest/group__init.html#ga9f8ffaacf3c269cc48eafbf8b9b71197
[glfwgetversion array]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFW.html#glfwGetVersion-int:A-int:A-int:A-
[glfwgetversion buffer]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFW.html#glfwGetVersion-java.nio.IntBuffer-java.nio.IntBuffer-java.nio.IntBuffer-
[glfwgetvideomode]: http://www.glfw.org/docs/latest/group__monitor.html#gafc1bb972a921ad5b3bd5d63a95fc2d52
[glfwinit]: http://www.glfw.org/docs/latest/group__init.html#ga317aac130a235ab08c6db0834907d85e
[glfwkeycallback]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWKeyCallback.html
[glfwkeycallbacki]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWKeyCallbackI.html
[glfwpollevents]: http://www.glfw.org/docs/latest/group__window.html#ga37bd57223967b4211d60ca1a0bf3c832
[glfwseterrorcallback]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFW.html#glfwSetErrorCallback-org.lwjgl.glfw.GLFWErrorCallbackI-
[glfwsetwindowpos]: http://www.glfw.org/docs/latest/group__window.html#ga1abb6d690e8c88e0c8cd1751356dbca8
[glfwterminate]: http://www.glfw.org/docs/latest/group__init.html#gaaae48c0a18607ea4a4ba951d939f0901
[glfwvidmode]: http://www.glfw.org/docs/latest/structGLFWvidmode.html
[glfwvidmode javadoc]: http://javadoc.lwjgl.org/org/lwjgl/glfw/GLFWVidMode.html
[glfwwaitevents]: http://www.glfw.org/docs/latest/group__window.html#ga554e37d781f0a997656c26b2c56c835e
[glfwwaiteventstimeout]: http://www.glfw.org/docs/latest/group__window.html#ga605a178db92f1a7f1a925563ef3ea2cf
[glfwwindow]: http://www.glfw.org/docs/latest/group__window.html#ga3c96d80d363e67d13a41b5d1821f3242
[glfwwindowhint]: http://www.glfw.org/docs/latest/group__window.html#ga7d9c8c62384b1e2821c4dc48952d2033
[hints]: http://www.glfw.org/docs/latest/window_guide.html#window_hints
[import-static]: https://github.com/baznex/imports/blob/v1.4.0/src/org/baznex/imports.clj#L14-L55
[imports]: https://github.com/baznex/imports
[intbuffer]: https://docs.oracle.com/javase/8/docs/api/java/nio/IntBuffer.html
[interop]: http://clojure.org/reference/java_interop
[leiningen]: http://leiningen.org/
[lwjgl]: https://www.lwjgl.org/
[memlengthutf8]: http://javadoc.lwjgl.org/org/lwjgl/system/MemoryUtil.html#memLengthUTF8-java.lang.CharSequence-boolean-
[memoryutil]: http://javadoc.lwjgl.org/org/lwjgl/system/MemoryUtil.html
[memutf8]: http://javadoc.lwjgl.org/org/lwjgl/system/MemoryUtil.html#memUTF8-java.lang.CharSequence-boolean-java.nio.ByteBuffer-
[null]: http://javadoc.lwjgl.org/org/lwjgl/system/MemoryUtil.html#NULL
[println question]: http://stackoverflow.com/q/37414126/5044950
[printstream]: https://docs.oracle.com/javase/8/docs/api/java/io/PrintStream.html
[printwriter]: https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html
[proxy]: http://clojuredocs.org/clojure.core/proxy
[string]: http://docs.oracle.com/javase/8/docs/api/java/lang/String.html
[system/err]: https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#err
[version]: http://javadoc.lwjgl.org/org/lwjgl/Version.html
[vk10]: http://javadoc.lwjgl.org/org/lwjgl/vulkan/VK10.html
[writeroutputstream]: https://commons.apache.org/proper/commons-io/javadocs/api-release/org/apache/commons/io/output/WriterOutputStream.html
