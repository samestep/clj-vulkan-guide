# clj-vulkan-guide

This is a guide to [Vulkan], written in the context of [Clojure] and [LWJGL].

You'll need a GPU that supports Vulkan, some familiarity with the Clojure
language, and a Clojure development environment with which you're comfortable.

Vulkan is fairly new and I'm still learning it myself, so this guide is not
authoritative and will probably have mistakes.

## [Part 0: Setup][part 0]

Check your system for compatible hardware, install the necessary drivers and
SDK, and run the [LWJGL Vulkan demos][demos].

## [Part 1: LWJGL and GLFW][part 1]

Learn the basics of LWJGL and [GLFW] by taking apart
[LWJGL's Getting Started Guide][guide].

[clojure]: http://clojure.org/
[demos]: https://github.com/LWJGL/lwjgl3-demos/tree/master/src/org/lwjgl/demo/vulkan
[glfw]: http://www.glfw.org/
[guide]: https://www.lwjgl.org/guide
[lwjgl]: https://www.lwjgl.org/
[part 0]: doc/part0.md
[part 1]: doc/part1.md
[vulkan]: https://www.khronos.org/vulkan/
