package com.bitwig.extensions.controllers.novation.slmk3.sequencer;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;

public class NoteStepStore implements NoteStep {
    
    private final int x;
    private final int y;
    private final int channel;
    private final State state;
    private double velocity;
    private double releaseVelocity;
    private double velocitySpread;
    private double duration;
    private double pan;
    private double timbre;
    private double pressure;
    private double gain;
    private NoteOccurrence occurrence;
    private double chance;
    private double transpose;
    private boolean occurrenceEnabled;
    private final boolean selected;
    private boolean chanceEnabled;
    private boolean muted;
    private double repeateVelocityCurve;
    private double repeatVelocityEnd;
    private int repeatCount;
    private double repeatCurve;
    private int recurrenceLength;
    private int recurrenceMask;
    private boolean repeatEnabled;
    
    public NoteStepStore(final NoteStep step) {
        this.x = step.x();
        this.y = step.y();
        this.channel = step.channel();
        this.state = step.state();
        this.velocity = step.velocity();
        this.releaseVelocity = step.releaseVelocity();
        this.velocitySpread = step.velocitySpread();
        this.duration = step.duration();
        this.pan = step.pan();
        this.timbre = step.timbre();
        this.pressure = step.pressure();
        this.gain = step.gain();
        this.occurrence = step.occurrence();
        this.chance = step.chance();
        this.transpose = step.transpose();
        this.occurrenceEnabled = step.isOccurrenceEnabled();
        this.chanceEnabled = step.isChanceEnabled();
        this.selected = step.isIsSelected();
        this.muted = step.isMuted();
        this.repeateVelocityCurve = step.repeatVelocityCurve();
        this.repeatVelocityEnd = step.repeatVelocityEnd();
        this.repeatCount = step.repeatCount();
        this.repeatCurve = step.repeatCurve();
        this.recurrenceLength = step.recurrenceLength();
        this.recurrenceMask = step.recurrenceMask();
        this.repeatEnabled = step.isRepeatEnabled();
    }
    
    @Override
    public int x() {
        return x;
    }
    
    @Override
    public int y() {
        return y;
    }
    
    @Override
    public int channel() {
        return channel;
    }
    
    @Override
    public State state() {
        return state;
    }
    
    @Override
    public double velocity() {
        return velocity;
    }
    
    @Override
    public void setVelocity(final double velocity) {
        this.velocity = velocity;
    }
    
    @Override
    public double releaseVelocity() {
        return releaseVelocity;
    }
    
    @Override
    public void setReleaseVelocity(final double velocity) {
        this.releaseVelocity = velocity;
    }
    
    @Override
    public double velocitySpread() {
        return velocitySpread;
    }
    
    @Override
    public void setVelocitySpread(final double amount) {
        this.velocitySpread = amount;
    }
    
    @Override
    public double duration() {
        return duration;
    }
    
    @Override
    public void setDuration(final double duration) {
        this.duration = duration;
    }
    
    @Override
    public double pan() {
        return this.pan;
    }
    
    @Override
    public void setPan(final double pan) {
        this.pan = pan;
    }
    
    @Override
    public double timbre() {
        return this.timbre;
    }
    
    @Override
    public void setTimbre(final double timbre) {
        this.timbre = timbre;
    }
    
    @Override
    public double pressure() {
        return this.pressure;
    }
    
    @Override
    public void setPressure(final double pressure) {
        this.pressure = pressure;
    }
    
    @Override
    public double gain() {
        return this.gain;
    }
    
    @Override
    public void setGain(final double gain) {
        this.gain = gain;
    }
    
    @Override
    public double transpose() {
        return transpose;
    }
    
    @Override
    public void setTranspose(final double transpose) {
        this.transpose = transpose;
    }
    
    @Override
    public boolean isIsSelected() {
        return this.selected;
    }
    
    @Override
    public double chance() {
        return chance;
    }
    
    @Override
    public void setChance(final double chance) {
        this.chance = chance;
    }
    
    @Override
    public boolean isChanceEnabled() {
        return chanceEnabled;
    }
    
    @Override
    public void setIsChanceEnabled(final boolean isEnabled) {
        this.chanceEnabled = isEnabled;
    }
    
    @Override
    public boolean isOccurrenceEnabled() {
        return this.occurrenceEnabled;
    }
    
    @Override
    public void setIsOccurrenceEnabled(final boolean isEnabled) {
        this.occurrenceEnabled = isEnabled;
    }
    
    @Override
    public NoteOccurrence occurrence() {
        return this.occurrence;
    }
    
    @Override
    public void setOccurrence(final NoteOccurrence condition) {
        this.occurrence = condition;
    }
    
    @Override
    public boolean isRecurrenceEnabled() {
        return this.occurrenceEnabled;
    }
    
    @Override
    public void setIsRecurrenceEnabled(final boolean isEnabled) {
        this.occurrenceEnabled = isEnabled;
    }
    
    @Override
    public int recurrenceLength() {
        return recurrenceLength;
    }
    
    @Override
    public int recurrenceMask() {
        return recurrenceMask;
    }
    
    @Override
    public void setRecurrence(final int length, final int mask) {
        this.recurrenceLength = length;
        this.recurrenceMask = mask;
    }
    
    @Override
    public boolean isRepeatEnabled() {
        return repeatEnabled;
    }
    
    @Override
    public void setIsRepeatEnabled(final boolean isEnabled) {
        this.repeatEnabled = isEnabled;
    }
    
    @Override
    public int repeatCount() {
        return repeatCount;
    }
    
    @Override
    public void setRepeatCount(final int count) {
        this.repeatCount = count;
    }
    
    @Override
    public double repeatCurve() {
        return repeatCurve;
    }
    
    @Override
    public void setRepeatCurve(final double curve) {
        this.repeatCurve = curve;
    }
    
    @Override
    public double repeatVelocityEnd() {
        return repeatVelocityEnd;
    }
    
    @Override
    public void setRepeatVelocityEnd(final double velocityEnd) {
        this.repeatVelocityEnd = velocityEnd;
    }
    
    @Override
    public double repeatVelocityCurve() {
        return repeateVelocityCurve;
    }
    
    @Override
    public void setRepeatVelocityCurve(final double curve) {
        this.repeateVelocityCurve = curve;
    }
    
    @Override
    public boolean isMuted() {
        return muted;
    }
    
    @Override
    public void setIsMuted(final boolean value) {
        this.muted = value;
    }
}
