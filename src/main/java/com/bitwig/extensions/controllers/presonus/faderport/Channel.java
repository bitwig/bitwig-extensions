package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extensions.controllers.presonus.*;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Track;

public class Channel
{
   public Channel(final int index)
   {
      mIndex = index;
      mFader = new Fader(index);
      mSolo = new ToggleButton((index >= 8 ? 0x48 : 0x08) + index);
      mMute = new ToggleButton((index >= 8 ? 0x70 : 0x10) + index);

      int[] selectId = {0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1d, 0x1e, 0x1f, 0x7, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27};

      mSelect = new ToggleButton(selectId[index]);
   }

   public void flush(final Target target, final MidiOut midiOut)
   {
      /*mFader.flush(target, midiOut);
      mSolo.flush(target, midiOut);
      mMute.flush(target, midiOut);
      mSelect.flush(target, midiOut);*/
   }

   public void onMidi(final Target target, final int status, final int data1, final int data2)
   {
      /*mFader.onMidi(target, status, data1, data2);
      mSolo.onMidi(target, status, data1, data2);
      mMute.onMidi(target, status, data1, data2);
      mSelect.onMidi(target, status, data1, data2);*/
   }

   public Fader getFader()
   {
      return mFader;
   }

   public ToggleButton getSolo()
   {
      return mSolo;
   }

   public ToggleButton getMute()
   {
      return mMute;
   }

   public ToggleButton getSelect()
   {
      return mSelect;
   }

   private final int mIndex;
   private final Fader mFader;
   private final ToggleButton mSolo;
   private final ToggleButton mMute;
   private final ToggleButton mSelect;

   public void setTarget(final Track track)
   {
      mFader.setTarget(track.volume());
      mSolo.setBooleanValue(track.solo());
      mSolo.setRunnable(track.solo()::toggle);
      mMute.setBooleanValue(track.mute());
      mMute.setRunnable(track.mute()::toggle);

      //mSelect.setBooleanSupplier(track::addIsSelectedInEditorObserver);
      mSelect.setRunnable(track::selectInEditor);
   }
}
