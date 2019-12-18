# LaunchPad PRO

This controller extension brings the following features to the controller:
 - Clip launcher via session mode
 - Keyboard and Drum play
    - Selection of modes and root key
 - Drum sequencer
 - Step sequencer
 - Basic mixer controls
    - Arm/Solo/Mute and Track select
    - Volume/Pan/Sends control

## Global Functions

|Function|How|
|---|---|
|Toggle Metronome|Press **Click**|
|Tap Tempo|Hold **Shift** and tap **Click**|
|Stop all clips|Hold **Shift** and press **Stop Clip**|
|Toggle Play|Press **Double**|
|Toggle Arranger Record|Hold **Shift** and press **Double**|
|Duplicate|Press **Duplicate**|
|Undo|Press **Undo**|
|Redo|Press **Redo**|

## Session Mode

To activate the session mode, press **Session**.
Then you'll see the clips on the pads and the scenes launchers on the right.
Note that if you change the color of a scene, it will update the scene launcher button accordingly.

|Function|Shortcut|
|---|---|
|Launch a clip|Press the pad|
|Launch a scene|Press the scene button (on the right)|
|Stop a clip|Press **Stop Clip** and choose the track to stop in the bottom row|
|Stop all clips|Hold **Shift** and press **Stop Clip**|
|Move the clip launcher window|Use the arrows on top left, hold **Shift** to scroll by page|
|Select a clip|hold **Shift** and press the pad|
|Delete a clip|hold **Delete** and press the pad|
|Quantize a clip|hold **Quantize** and press the pad|

## Play Mode

Activate the *Play Mode* by pressing the **Note** button.

The *Play Mode* lets you play notes via a few different layouts.
You can select the different layouts by pressing a scene buttons.

|Index|Layout Name|
|---|---|
|1|Guitar|
|2|Line/3|
|3|Line/7|
|4|Piano|
|5|64 Drums|
|6|*Unused*|
|7|*Unused*|
|8|Root Key and Mode chooser|

## Drum Sequencer Mode

Activate the Drum Sequencer Mode by pressing the **Device** button.

The *Drum Sequencer* needs a clip to be selected first.

The grid is divided in two parts, the 32 upper pads are for the steps, while the 32 others are for drums, performances and data.

### Steps

The 32 upper pads displays 32 steps. The light will be bright if there is a note on at this steps, dimmed if there is a sustained note or off if there is nothing.

The four scenes on the right lets you edit up to 8 bars, each scene buttons will display 2 bars: [1,2], [3,4], [5,6], [7,8].

To set the length of the clip, you can hold **Shift** and press one of the 8 scene buttons, or a step.

### Minor Modes

To select a minor mode, press one of the four scene buttons on the bottom right.
There are four minor modes:

|Index|Description|
|---|---|
|1|Play drums, note repeat, clip operations, select/solo/mute of a pad|
|2|Play drums, performance macros and scene macros|
|3|Edit Velocity, Note Length and Pan **per step**|
|3|Edit Micro Tuning, Timbre and Pressure **per step**|

#### 1. Play drums and basic actions

On the bottom left you'll see a 4x4 grid of pads which you can play.
Playing a pad will select the given drum pad in the sequencer above.
You can select a pad without playing it by pressing **Shift** + **Pad**.

On the bottom right, there will be a bunch of *action* pads. They work by holding them, and then pressing a pad.
You'll have a various different note repeat speed at the top and *Drum Pad Select*, *Mute* and *Solo* at the bottom.
You can un-mute all drum pads by doing **Delete** + **Mute Pad**.
You can un-solo all drum pads by doing **Delete** + **Solo Pad**.

#### 2. Drum Performances and Scenes

To use this minor mode, add two remote control pages to your drum kit:
 - One page with 8 Knobs, and add the tag *drum-perfs*
 - One page with 8 buttons, and add the tag *drum-scenes*

## Step Sequencer Mode

Activate the *Step Sequencer Mode* by pressing the **User** button.

This mode is very similar to the *Drum Sequencer Mode* except that it is intended to sequence notes.
To enter a step, hold the steps and press the pitches. 

## Volume Mode

Activate the *Volume Mode* by pressing the **Volume** button.

## Pan Mode

Activate the *Pan Mode* by pressing the **Pan** button.

## Sends Mode

Activate the *Sends Mode* by pressing the **Sends** button.
