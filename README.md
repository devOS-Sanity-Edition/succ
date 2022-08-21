# Succ™
#### Look at me go!
A Minecraft mod about suction cups. Made for Modfest: Singularity.

![banner](./readme_assets/ModFest_Singularity_Banner.png)

### The Suction Cup
The Suction Cup is a powerful tool. Using it, you can climb walls!
![A player using suction cups to climb a wall](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/on_wall.png?raw=true)
![A player using suction cups to climb a wall, seen through a window](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/through_window.png?raw=true)
![First-person view of climbing](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/first_person.png?raw=true)

## Getting started
To get climbing, you'll need a pair of Suction Cups, and a set of Suction Cup Boots.

![The recipe for one Suction Cup](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/suction_cup_recipe.png?raw=true)

![The recipe for one set of Suction Cup Boots](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/suction_cup_boots_recipe.png?raw=true)

Featuring EMI integration!

![An EMI recipe tree showing how to make Suction Cup Boots](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/emi_compat.png?raw=true)

To start climbing, hold a Suction Cup on each limb. That means you need a Suction Cup
in your main hand and your offhand, and you need a set of Suction Cup Boots equipped.

![A player equipped with proper climbing attire](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/equipment.png?raw=true)

## Controls
Once you're ready to climb, click a wall to start.
Once you're climbing, each Suction Cup is bound to a key. When each key is pressed,
the Suction Cup will toggle between sticking and not sticking.

| Limb      | Default keybind    |
|-----------|--------------------|
| Left arm  | Left mouse button  |
| Right arm | Right mouse button |
| Left leg  | Left shift         |
| Right leg | Space              |

Each key can be rebound in the keybinds menu.

When not stuck to the wall, a Suction Cup can be moved with your movement keys (WASD).
Click again to stick it back to the wall. Repeat until you've reached your destination.

When you want to stop climbing, you have two options.
To stop immediately, release all Suction Cups.

For a more controlled stop, move a Suction Cup over towards a suitable position,
like a ledge. If you hold this direction, you'll stop in 3 seconds.

In this image, the right-hand Suction Cup is off the wall. D is held, so it's
moving to the right.

![A player climbing into a ledge](https://github.com/devOS-Sanity-Edition/succ/blob/1.19/readme_assets/controlled_stop.png?raw=true)

## Development
Succ™ is on Maven.

```groovy
maven { url = "https://mvn.devos.one/snapshots/" }
```
```groovy
modImplementation("one.devos.nautical:succ:<version>")
```

Which items are valid Suction Cups are determined by tags:
`succ:hand_climbing_cups` and `succ:feet_climbing_cups`
