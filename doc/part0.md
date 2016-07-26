# Part 0: Setup

## Hardware

If you want to do any Vulkan development, you first need a GPU that supports
Vulkan. The [Vulkan Wikipedia page][wikipedia] lists some compatibility
information, but you'll probably have to Google around to determine whether your
specific hardware currently supports Vulkan.

## Drivers

As with the hardware, you'll probably have to Google around to find which
drivers for your GPU support Vulkan. For instance, I'm using an NVIDIA GeForce
GT 750M on Ubuntu 16.04, so I want the latest drivers from the
[Proprietary GPU Drivers PPA][ppa]:

```sh
sudo add-apt-repository ppa:graphics-drivers/ppa
sudo apt update
sudo apt install nvidia-367
```

You may need to reboot after installing drivers. If you're using NVIDIA, you can
verify that you're using the right driver by opening NVIDIA X Server Settings:

```sh
nvidia-settings
```

Your NVIDIA driver version should be displayed in the "System Information"
section on the "X Server Information" page.

## SDK

Download the Vulkan SDK from the [LunarXchange site][lunarxchange]. This SDK
requires you to set several environment variables, so you should put any
variable assignment commands you enter here into your `~/.bashrc` file. You can
put the SDK wherever you like; I put mine in `~/vulkan`:

```sh
VULKAN_SDK_ROOT=~/vulkan
mkdir VULKAN_SDK_ROOT
cd VULKAN_SDK_ROOT
```

If you're using Linux, you need to mark the installer as executable and run it.
This will create a `VulkanSDK` directory in the current working directory:

```sh
VULKAN_ARCH=x86_64
VULKAN_VERSION=1.0.21.1
mv ~/Downloads/vulkansdk-linux-$VULKAN_ARCH-$VULKAN_VERSION.run .
chmod +x vulkansdk-linux-$VULKAN_ARCH-$VULKAN_VERSION.run
./vulkansdk-linux-$VULKAN_ARCH-$VULKAN_VERSION.run
```

Finally, as mentioned above, you need to set a few special environment variables
to make your new local Vulkan SDK installation visible to the apps you're going
to be writing:

```sh
VULKAN_SDK=$VULKAN_SDK_ROOT/VulkanSDK/$VULKAN_VERSION/$VULKAN_ARCH
export PATH=$PATH:$VULKAN_SDK/bin
export LD_LIBRARY_PATH=$VULKAN_SDK/lib
export VK_LAYER_PATH=$VULKAN_SDK/etc/explicit_layer.d
```

Then restart your shell.

## Demos

Install Java, Maven, and Git if you haven't already:

```sh
sudo add-apt-repository ppa:webupd8team/java
sudo apt update
sudo apt install oracle-java8-installer maven git
```

Then, to verify that everything's working, try running the various
[LWJGL Vulkan demos][demos]:

```sh
git clone https://github.com/LWJGL/lwjgl3-demos.git
cd lwjgl3-demos
mvn package
cd target
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.ClearScreenDemo
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.TriangleDemo
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.ColoredTriangleDemo
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.ColoredRotatingQuadDemo
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.TwoRotatingTrianglesDemo
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.TwoRotatingTrianglesInvDepthDemo
java -cp lwjgl3-demos.jar org.lwjgl.demo.vulkan.InstancedSpheresDemo
```

You may see some error messages in the terminal, but as long as the actual
rendering looks right, you should be good to go.

[demos]: https://github.com/LWJGL/lwjgl3-demos/tree/master/src/org/lwjgl/demo/vulkan
[lunarxchange]: https://vulkan.lunarg.com/signin
[ppa]: https://launchpad.net/~graphics-drivers/+archive/ubuntu/ppa
[wikipedia]: https://en.wikipedia.org/wiki/Vulkan_%28API%29#Hardware_compatibility
