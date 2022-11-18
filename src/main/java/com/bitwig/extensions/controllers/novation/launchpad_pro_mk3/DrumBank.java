package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

public class DrumBank {

    private LaunchpadProMK3Workflow driver;
    private int offset;

    private Boolean updateNoteTableIsInUse = false;
    protected final Integer[] noteTable = new Integer[128];
    protected final Integer[] baseNoteTable = new Integer[128];

    public DrumBank(LaunchpadProMK3Workflow d) {
        driver = d;
        offset = 0;
        initNoteTable();
    }

    private void initNoteTable() {
        for (int i = 0; i < 128; i++) {
            noteTable[i] = i;
            baseNoteTable[i] = i;
        }
    }

    private Boolean scrollAction(String s) {
        if (updateNoteTableIsInUse)
            return false;
        updateNoteTableIsInUse = true;
        int position = driver.mDrumPadBank.scrollPosition().get();
        driver.mHost.println(String.valueOf(position));
        if (s == "UP") {
            if (position >= 0 && position <= 60)
                driver.mDrumPadBank.scrollBy(4);
        }

        if (s == "UP_PAGE") {
            if (position >= 0 && position < 4)
                driver.mDrumPadBank.scrollPosition().set(4);
            else if (position >= 4 && position <= 36)
                driver.mDrumPadBank.scrollBy(16);
            else if (position > 36 && position < 64)
                driver.mDrumPadBank.scrollPosition().set(64);
        }

        if (s == "DOWN") {
            if (position <= 64 && position >= 4)
                driver.mDrumPadBank.scrollBy(-4);
        }

        if (s == "DOWN_PAGE") {
            if (position <= 64 && position > 52)
                driver.mDrumPadBank.scrollPosition().set(52);
            else if (position <= 52 && position >= 20)
                driver.mDrumPadBank.scrollBy(-16);
            else if (position < 20 && position > 0)
                driver.mDrumPadBank.scrollPosition().set(0);
        }
        updateNoteTableIsInUse = false;
        return true;
    }

    public void update(int p) {
        this.setOffset(p - 36);
        if (offset == 0)
            driver.mDrumInput.setKeyTranslationTable(baseNoteTable);
        else if (offset >= -36 && offset <= 28) {
            for (int i = 36; i < 100; i++)
                noteTable[i] = i + getOffset();
            driver.mDrumInput.setKeyTranslationTable(noteTable);
        }
    }

    public void scroll(String string) {
        scrollAction(string);

    }

    public Integer[] getNoteTable() {
        return noteTable;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int o) {
        offset = o;
    }
}
