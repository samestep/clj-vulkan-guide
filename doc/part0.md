# Part 0: Setup

## Hardware

If you want to do any Vulkan development, you first need a GPU that supports
Vulkan. The [Vulkan Wikipedia page][wikipedia] lists some compatibility
information, but you'll probably have to Google around to determine whether your
specific hardware currently supports Vulkan.

## Drivers

As with the hardware, you'll probably have to Google around to find which
drivers for your GPU support Vulkan. For instance, I'm using an NVIDIA GeForce
GT 750M on Ubuntu 16.04, so my two options are [NVIDIA 364.19][364.19] and
[NVIDIA 367.18][367.18], both of which are in the
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

Download the Vulkan SDK from the [LunarXchange site][lunarxchange]. Once you've
done so, you can put it wherever you like; I put mine in `~/vulkan`:

```sh
mkdir ~/vulkan
cd ~/vulkan
```

If you're using Linux, you need to mark the installer as executable and run it.
This will create a `VulkanSDK` directory in the current working directory:

```sh
cp ~/Downloads/vulkansdk-linux-x86_64-1.0.13.0.run .
chmod +x vulkansdk-linux-x86_64-1.0.13.0.run
./vulkansdk-linux-x86_64-1.0.13.0.run
```

Finally, you need to set a few environment variables to make your new local
Vulkan SDK installation visible to the apps you're going to be writing. Add
these lines to the end of your `~/.bashrc` file and restart your shell:

```sh
VULKAN_SDK=~/vulkan/VulkanSDK/1.0.13.0/x86_64
export PATH=$PATH:$VULKAN_SDK/bin
export LD_LIBRARY_PATH=$VULKAN_SDK/lib
export VK_LAYER_PATH=$VULKAN_SDK/etc/explicit_layer.d
```

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

[364.19]: http://www.nvidia.com/download/driverResults.aspx/101818/en-us
[367.18]: http://www.nvidia.com/download/driverResults.aspx/102879/en-us
[demos]: https://github.com/LWJGL/lwjgl3-demos/tree/master/src/org/lwjgl/demo/vulkan
[lunarxchange]: https://vulkan.lunarg.com/signin
[ppa]: https://launchpad.net/~graphics-drivers/+archive/ubuntu/ppa
[wikipedia]: https://en.wikipedia.org/wiki/Vulkan_%28API%29#Hardware_compatibility
