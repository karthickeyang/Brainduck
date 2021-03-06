package net.zomis.brainf.analyze.analyzers

import net.zomis.brainf.analyze.BrainfuckAnalyzer
import net.zomis.brainf.analyze.MemoryCell
import net.zomis.brainf.model.BrainfuckCommand
import net.zomis.brainf.model.BrainfuckRunner
import net.zomis.brainf.model.classic.BrainFCommand

class MemoryIndexAnalysis implements BrainfuckAnalyzer {

    private boolean memoryIndexBelowZero
    private boolean memoryIndexAboveMax
    private int rightmostMemory

    @Override
    void print() {
        if (this.memoryIndexBelowZero) {
            println 'WARNING: Memory index goes below zero'
        }
        if (this.memoryIndexAboveMax) {
            println 'WARNING: Memory index goes above maximum'
        }
        if (this.rightmostMemory > 30_000) {
            println 'WARNING: Memory index goes above 30 000'
        } else if (this.rightmostMemory > 9_999) {
            println 'WARNING: Memory index goes above 9 999'
        }
        println "Rightmost memory accessed is $rightmostMemory"
        println()
    }

    @Override
    void beforePerform(MemoryCell cell, BrainfuckRunner runner, BrainfuckCommand command) {
        if (command == BrainFCommand.PREVIOUS && runner.memory.memoryIndex == 0) {
            this.memoryIndexBelowZero = true
        }
        if (command == BrainFCommand.NEXT && runner.memory.memoryIndex == runner.memory.size - 1) {
            this.memoryIndexAboveMax = true
        }
    }

    @Override
    void afterPerform(MemoryCell cell, BrainfuckRunner runner, BrainfuckCommand command) {
        this.@rightmostMemory = Math.max(this.@rightmostMemory, runner.memory.memoryIndex)
    }

    boolean isMemoryIndexBelowZero() {
        this.@memoryIndexBelowZero
    }

    boolean isMemoryIndexAboveMax() {
        this.@memoryIndexAboveMax
    }

    int getRightmostMemory() {
        this.@rightmostMemory
    }

}
