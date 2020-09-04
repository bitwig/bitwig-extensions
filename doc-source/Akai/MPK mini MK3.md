# AKAI MPK mini MK3

## Overview

This controller extension adds support for the MPK mini MK3:
 - Keys provide note input (also with a dedicated note input port)
 - Pads provide note input (also with a dedicated note input port)
 - Knobs follow the script's target, with the targeted parameter name shown on the screen

## Device Setup

There is no configuration required on the device; Bitwig Studio will send a custom "program" to it.

 - Do not use "PROG SELECT"; it will interfere with Bitwig Studio's connection.
 - To synchronize the MPK's arpeggiator and note repeat: go into Bitwig's Synchronization settings (Dashboard > Settings > Synchronization), and enabling sending MIDI "Clock," "Start/Stop," and "SPP" to the MPK mini MK3.
