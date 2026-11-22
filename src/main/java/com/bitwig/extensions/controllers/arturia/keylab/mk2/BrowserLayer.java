package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;

/** Centralized preset-browser bindings (modern Arturia pattern). */
class BrowserLayer
{
   private static final Color WHITE = Color.fromRGB(1.f, 1.f, 1.f);
   private static final Color ORANGE = Color.fromRGB(1.f, 0.5f, 0.f);
   private static final Color GREEN = Color.fromRGB(0.f, 1.0f, 0.f);
   private static final Color RED = Color.fromRGB(1.f, 0.0f, 0.f);

   BrowserLayer(final Layer layer, final ArturiaKeylabMkII ext)
   {
      layer.bindPressed(ButtonId.SELECT_MULTI, ext.mPopupBrowser::cancel);
      layer.bind(WHITE, ButtonId.SELECT_MULTI);

      for (int i = 0; i < 4; i++)
      {
         final int number = i;
         final ButtonId selectId = ButtonId.select(i);

         layer.bindPressed(selectId, () -> ext.mPopupBrowser.selectedContentTypeIndex().set(number));
         layer.bind(ORANGE, selectId);
      }

      final CursorBrowserFilterItem categories = (CursorBrowserFilterItem)ext.mPopupBrowser.categoryColumn()
         .createCursorItem();
      final CursorBrowserFilterItem creators = (CursorBrowserFilterItem)ext.mPopupBrowser.creatorColumn()
         .createCursorItem();

      layer.bindPressed(ButtonId.SELECT5, categories.selectPreviousAction());
      layer.bind(GREEN, ButtonId.SELECT5);

      layer.bindPressed(ButtonId.SELECT6, categories.selectNextAction());
      layer.bind(GREEN, ButtonId.SELECT6);

      layer.bindPressed(ButtonId.SELECT7, creators.selectPreviousAction());
      layer.bind(RED, ButtonId.SELECT7);

      layer.bindPressed(ButtonId.SELECT8, creators.selectNextAction());
      layer.bind(RED, ButtonId.SELECT8);

      layer.bindPressed(ButtonId.PRESET_PREVIOUS, ext.mPopupBrowser.cancelAction());
      layer.bindPressed(ButtonId.PRESET_NEXT, ext.mPopupBrowser.commitAction());

      layer.bindToggle(ButtonId.WHEEL_CLICK, ext.mPopupBrowser.commitAction(), ext.mCursorTrack.hasPrevious());
      layer.bind(ext.mWheel, ext.getHost().createAction(() -> ext.mPopupBrowser.selectPreviousFile(), () -> "-"),
         ext.getHost().createAction(() -> ext.mPopupBrowser.selectNextFile(), () -> "+"));

      layer.showText(ext.mBrowserCategory.name(), ext.mBrowserResult.name());
   }
}
