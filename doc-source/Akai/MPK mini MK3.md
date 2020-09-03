# AKAI MPK mini MK3

## Overview

This controller extension adds support for the MPK mini MK3:
 - keys are working, you have a dedicated note input
 - pads are working, you have a dedicated note input
 - knobs are working, and the name of the parameter being controlled should be displayed on the screen

## Device Setup

There is no configuration required on the device; Bitwig Studio will send a custom "program" to it.

 - Do not use "PROG SELECT". That would interfer with Bitwig Studio custom program
 - Go into the synchronization settings and send the MIDI Clock, Start/Stop and SPP to synchronize the arpeggiator and note repeat
