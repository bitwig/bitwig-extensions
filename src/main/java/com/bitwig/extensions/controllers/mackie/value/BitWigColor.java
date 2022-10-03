package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.controller.api.SettableColorValue;

public enum BitWigColor {
   BROWN(0.6392157f, 0.4745098f, 0.2627451f), //
   LIGHT_BROWN(0.7764706f, 0.62352943f, 0.4392157f), //
   PLUM(0.34117648f, 0.38039216f, 0.7764706f), //
   PURPLE(0.58431375f, 0.28627452f, 0.79607844f), //
   PINK_PURPLE(0.7372549f, 0.4627451f, 0.9411765f), //
   RED(0.8509804f, 0.18039216f, 0.14117648f), //
   PINK(0.8509804f, 0.21960784f, 0.44313726f), //
   ORANGE(1.0f, 0.34117648f, 0.023529412f), //
   AMBER(0.8509804f, 0.6156863f, 0.0627451f), //
   OLIVE(0.8509804f, 0.6156863f, 0.0627451f), //
   GREEN(0.0f, 0.6156863f, 0.2784314f), //
   GREEN_BLUE(0.0f, 0.6509804f, 0.5803922f), //
   DARK_BLUE(0.0f, 0.6f, 0.8509804f), //
   LIGHT_BLUE(0.26666668f, 0.78431374f, 1.0f), //
   AQUA(0.2627451f, 0.8235294f, 0.7254902f), //
   LIGHT_PINK(0.88235295f, 0.4f, 0.5686275f), //
   YELLOW(0.89411765f, 0.7176471f, 0.30588236f), //
   GRAY(0.5f, 0.5f, 0.5f);
   private final float red;
   private final float green;
   private final float blue;
   private final int lookupIndex;

   BitWigColor(final float red, final float green, final float blue) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      final int rv = (int) Math.floor(red * 255);
      final int gv = (int) Math.floor(green * 255);
      final int bv = (int) Math.floor(blue * 255);
      lookupIndex = rv << 16 | gv << 8 | bv;
   }

   public void set(final SettableColorValue color) {
      color.set(red, green, blue);
   }

   public int getLookupIndex() {
      return lookupIndex;
   }

}
