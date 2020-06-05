# Kenton KillaMix Mini

## Introduction

This extension offers two modes to control Bitwig Studio. The first one is a simple mixer mode, the other one is for device control.
You can switch between the two modes with button 9.

Before you can use this extension, you will have to set the Kenton KillaMix up, see section _Setup_.

## Device Mode
      
This mode allows you to navigte tracks, devices and device-pages in Bitwig Studio. Knobs 1-8 control the currently
selected device/page macros. Knob 9 always adjusts the volume of the current track. The buttons 1-6 allow you to navigate in the following way:
* Buttons 1 & 2: select previous/next track
* Buttons 3 & 4: select previous/next device
* Buttons 5 & 6: select previous/next device-page     

The button light indicates if the previous/next step for navigation is available. The joystick is freely mappable. In the Studio I/O panel on the right
side of the Bitwig Studio window, you can select between displaying the parameter with or without applied modulations on the encoder ring.

## Mixer Mode
In this mode the knobs control the track volumes of 8 consecutive tracks. Use the joystick left/right to navigate the tracks. The buttons toggle
between mute/unmute for the respective tracks. Knob 9 controls the master volume.

## Setup
In order for this integration to work, the Kenton KillaMix controller needs to be setup in a way that allows
Bitwig to communicate back controller values. This is achieved in the following way:</p>
* Enable relative knob updates: while plugging in the USB cable, press <b>buttons</b> 6 and 8. Hold this for several seconds, then release the buttons. Button 9 (and potentially others) will illuminate to indicate that the configuation mode has been entered. Press <b>button</b> 1 several times until the <b>encoder ring</b> above it shows two illuminated segments ( Ableton signed 7 bit mode ). Proceed with the next step.
* Enable CC updates: Press <b>button</b> 3 until the <b>encoder ring</b> above it shows one lit LED (receive CC mode). Press <b>button</b> 9 to save and exit this configuration mode.
* Enable button toggle behavior: while the controller is on, press and hold <b>knob</b> 9 while pressing <b>knob</b> 8. Release both knobs, all encoder rings will light up to show that the button configuration mode has been entered. Press each <b>button</b> several times until it starts flashing. Do this for all buttons. To exit this configuration mode, press any <b>knob</b>. 
