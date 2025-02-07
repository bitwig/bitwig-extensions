package com.bitwig.extensions.controllers.novation.slmk3.sequencer;

public enum StepInputAdvanceType {
    
    SAME("Same Pg.", false, false),
    NEXT_PAGE("Nxt Pg.", true, false),
    NEXT_PAGE_EXPAND("Nxt+Exp", true, true);
    
    private final String label;
    private final boolean expands;
    private final boolean nextPage;
    
    StepInputAdvanceType(final String label, final boolean nextPage, final boolean expands) {
        this.label = label;
        this.expands = expands;
        this.nextPage = nextPage;
    }
    
    public StepInputAdvanceType increment(final int dir) {
        final int pos = this.ordinal();
        final int next = pos + dir;
        if (next >= 0 && next < StepInputAdvanceType.values().length) {
            return StepInputAdvanceType.values()[next];
        }
        return this;
    }
    
    public boolean isExpands() {
        return expands;
    }
    
    public boolean isNextPage() {
        return nextPage;
    }
    
    public String getLabel() {
        return label;
    }
}
