# a space project
Welcome to A Space Project. A project involving space...duh.
* Enjoy Galactic Space Exploration in a sate-of-the-art, hyper-realistic physics simulation of the entire universe!
* Get a realistic sense of the cosmic scale; there's literally dozens of planets and traveling between them could take up to minutes!
* You want Faster Than Light Travel, you got it. (yeah it's real bro cuz like quantum anti-dark matter n' stuff yo)
* During your explorations you could find various astronomical bodies including:
    * Unique planetary systems
    * Binary star systems, Trinary star systems, and even quadri... quatro? quadrino-ary? as-many-as-you-want star systems!
    * Discover lonely rogue planets who lost their sun. :(
* Pilot different spaceships and combat dumb AI written by an even dumber human
* Then when your bored of that you can land on a planet I guess. The worlds are flat (Ha! take that round Earthers)


![screenshot](/Capture.PNG?raw=true)


## Current Status
Ok, seriously: The are two main game states. Flying In Space & exploring Worlds.
Space is a finite galaxy, generated by a set of points. Each point is a collection of stars and/or planets.
Worlds are also finite. If you move continuously in one direction you will eventually return to where you started. Wrapped via a 4D torus.
**This game is still in pre-alpha prototype phase. There's not much content yet but I have plans. Stay tuned.**
Work in progress: scaffolding for what will hopefully one day be a game. The code is bit rough in some places, littered with todo's, half-baked features, and of course the occasional bug. Also, forgive my programmer graphics.


## Features
* Feature Creep and Unrealistic Scope!
* Explore a random Galaxy in a spaceship
* Discover various planetary systems and celestial bodies
* Land on planets and explore
* Fly different ships
* Fight against other ship (in progress)
* Destroy asteroids (todo)
* Mining and base-building on planets (todo)
* Controller support (works in game but not menus)
* Multiplayer
  * nope! would love to one day but out of scope for now...
* Unit Tests
  * pfft...jk. my code is perfect, the first time. every time.
* Cross-Platform Desktop and Mobile Support
  * Windows, OSX, Linux, Android, IOS
* Developer Tools (in progress)


### Controls
| Control                        | Desktop       | Controller/Gamepad    | Mobile (iOS, Android)                   |
|------------------------------- | ------------  | ------------------    | ----------------------------------------|
| Movement                       | WASD          | Left Joystick + L1/R1 | Left Joystick                           |
| Aim                            | Mouse         | Left Joystick         | Left Joystick                           |
| Attack: Shoot                  | Right-Click   | A                     | bottom right button                     |
| Defense: Shield                | Shift         | B                     | todo: needs button                      |
| Defense: Dodge (Barrel Roll)   | Space + A/D   | Right/Left Bumper     | todo: swipe gesture?                    |
| Toggle HyperDrive              | 1             | D-Pad Up              | todo: needs button                      |
| Land/Take Off                  | T             | D-Pad Down            | top center when over planet             |
| Enter/Exit vehicle             | G             | Y                     | bottom right small button when in/near vehicle |
| Zoom                           | Scroll Wheel  | Right JoyStick        | todo: Pinch Zoom                        |
| Reset Zoom                     | Middle-Click  | Click in Right stick  | todo: double tap                        |
| Toggle Map State               | M             |                       | top left corner button                  |
| Toggle HUD                     | H             |                       |                                         |
| Full screen                    | F11           |                       |                                         |
| Menu (Pause)                   | Escape        | Start                 | top right corner button                 |
| Vsync                          | F8            |                       |                                         |
| ECS Debug Viewer               | F9            |                       |                                         |
| Misc debug keys I am too lazy to document rn and won't be permanent anyway |                                    |


## License
   Apache 2.0: see [LICENSE.md](https://github.com/0XDE57/SpaceProject/blob/master/LICENSE.md)
   
   Credit appreciated, but not required.

## Libraries
- [libGDX](https://github.com/libgdx/libgdx)
- [Ashley](https://github.com/libgdx/ashley/wiki)
- [OpenSimplexNoise](https://gist.github.com/KdotJPG/b1270127455a94ac5d19)
- [VisUI](https://github.com/kotcrab/vis-ui)


## Building
**General**
* Set up your [Development Environment](https://libgdx.badlogicgames.com/documentation/gettingstarted/Setting%20Up.html)
* Make sure Android SDK is installed.
* Import project in IDE of choice using gradle.
* If a "File not found" error occurs, check the working directory. Append "android\assets" to the working directory in run configurations.


**Android Studio**
* Desktop
  * create Run Configuration
  * main class = com.spaceproject.desktop.DesktopLauncher
  * use classpath of module 'desktop'
  * working directory = ...\SpaceProject\android\assets
  * (must ensure working directory includes assets so data like fonts, particles, shaders, configs can be loaded)
  * build and run!
* Android
  * enable dev options, enable usb debugging
  * connect phone, android studio should detect it
  * build and run!
* IOS
  * https://libgdx.com/dev/import_and_running/#ios

