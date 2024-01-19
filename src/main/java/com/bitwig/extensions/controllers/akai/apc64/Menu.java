package com.bitwig.extensions.controllers.akai.apc64;

import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.controllers.akai.apc64.layer.MainDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Menu {
    private final MainDisplay.Screen screen;
    private final List<MenuItem> items = new ArrayList<>();
    private int itemIndex = 0;
    private boolean onMenu = true;
    private MenuItem currentMenu;

    public record EnumMenuValue(String value, String displayValue) {

    }

    public abstract static class MenuItem {
        private final String name;
        protected Consumer<String> updater;

        protected MenuItem(final String name) {
            this.name = name;
        }

        public void setFocusScreen(final Consumer<String> updater) {
            this.updater = updater;
        }

        public void release() {
            this.updater = null;
        }

        public void update(final String newValue) {
            if (updater != null) {
                updater.accept(newValue);
            }
        }

        public abstract String getCurrentValue();

        public abstract void handleIncrement(final int dir);

        public boolean isMomentary() {
            return false;
        }

        public void handlePressed(final boolean pressed) {
        }
    }

    public static class EnumMenuItem extends MenuItem {
        private final SettableEnumValue value;
        private final List<EnumMenuValue> selection;
        private EnumMenuValue current;

        public EnumMenuItem(final String name, final SettableEnumValue value, final List<EnumMenuValue> selection) {
            super(name);
            value.markInterested();
            this.value = value;
            this.selection = selection;
            this.current = selection.get(0);
            value.addValueObserver(enumValue -> this.update(enumValue));
        }

        public void update(final String newValue) {
            current = selection.stream().filter(v -> v.value.equals(newValue)).findFirst().orElse(null);
            if (updater != null) {
                updater.accept(current.displayValue());
            }
        }

        @Override
        public String getCurrentValue() {
            return current.displayValue();
        }

        public void handleIncrement(final int dir) {
            current = nextValue(value.get(), selection, dir, false);
            value.set(current.value());
        }

    }

    public static class BooleanToggleMenuItem extends MenuItem {
        private final SettableBooleanValue value;

        public BooleanToggleMenuItem(final String name, final SettableBooleanValue value) {
            super(name);
            this.value = value;
            value.addValueObserver(boolValue -> this.update(boolValue ? "On" : "Off"));
        }

        public void handlePressed(final boolean pressed) {
            if (pressed) {
                value.toggle();
            }
        }

        @Override
        public boolean isMomentary() {
            return true;
        }

        @Override
        public String getCurrentValue() {
            return value.get() ? "On" : "Off";
        }

        public void handleIncrement(final int dir) {
            value.toggle();
        }

    }

    public static class HoldMenuItem extends MenuItem {
        private final SettableBooleanValue value;

        public HoldMenuItem(final String name, final SettableBooleanValue value) {
            super(name);
            this.value = value;
            value.addValueObserver(boolValue -> this.update(boolValue ? "On" : "Off"));
        }

        @Override
        public String getCurrentValue() {
            return value.get() ? "On" : "Off";
        }

        @Override
        public void handleIncrement(final int dir) {
        }

        @Override
        public void handlePressed(final boolean pressed) {
            value.set(pressed);
        }

        public boolean isMomentary() {
            return true;
        }
    }

    public Menu(final MainDisplay.Screen screen) {
        this.screen = screen;
        screen.setRow(0, "Bitwig Menu");
    }

    public void addMenuItem(final MenuItem item) {
        this.items.add(item);
    }

    public void init() {
        if (this.items.isEmpty()) {
            return;
        }
        currentMenu = this.items.get(0);
        currentMenu.setFocusScreen(this::updateValue);
        update();
    }

    private void update() {
        final MenuItem menuItem = items.get(itemIndex);
        screen.setRow(1, "%s %s ".formatted(onMenu ? ">" : " ", menuItem.name));
        updateValue(menuItem.getCurrentValue());
    }

    public void handleInc(final int dir) {
        if (onMenu) {
            final int nextIndex = itemIndex + dir;
            if (nextIndex >= 0 && nextIndex < items.size()) {
                items.get(itemIndex).release();
                itemIndex = nextIndex;
                items.get(itemIndex).setFocusScreen(this::updateValue);
                currentMenu = this.items.get(itemIndex);
                update();
            }
        } else {
            final MenuItem menuItem = items.get(itemIndex);
            menuItem.handleIncrement(dir);
            update();
        }
    }

    private void updateValue(final String value) {
        screen.setRow(2, "%s%s ".formatted(!onMenu ? ">" : "", value));
    }

    public void handEncoderClick(final boolean pressed) {
        Apc64Extension.println(" ON menu %s %s", currentMenu.getClass().getName(), onMenu);
        if (currentMenu.isMomentary()) {
            onMenu = true;
            currentMenu.handlePressed(pressed);
            update();
        } else {
            if (pressed) {
                onMenu = !onMenu;
                update();
            }
        }
    }

    public static EnumMenuValue nextValue(final String currentValue, final List<EnumMenuValue> list, final int inc,
                                          final boolean wrap) {
        int index = -1;
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            if (currentValue.equals(list.get(i).value())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            final int next = index + inc;
            if (next >= 0 && next < size) {
                return list.get(next);
            } else if (wrap) {
                index = next < 0 ? size - 1 : next >= size ? 0 : next;
            }
            return list.get(index);
        }
        return list.get(0);
    }
}
