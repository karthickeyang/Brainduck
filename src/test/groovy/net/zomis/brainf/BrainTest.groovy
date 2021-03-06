package net.zomis.brainf

import net.zomis.brainf.analyze.IndexCounters
import net.zomis.brainf.analyze.MemoryCell
import net.zomis.brainf.analyze.analyzers.CommandCountAnalysis
import net.zomis.brainf.analyze.analyzers.IOAnalysis
import net.zomis.brainf.analyze.analyzers.MemoryValues
import net.zomis.brainf.analyze.analyzers.ReadWriteAnalysis
import net.zomis.brainf.analyze.analyzers.WhileLoopAnalysis
import net.zomis.brainf.model.BrainF
import net.zomis.brainf.model.classic.BrainFCommand
import net.zomis.brainf.model.BrainfuckMemory
import net.zomis.brainf.model.BrainfuckRunner
import net.zomis.brainf.model.run.StepContinueStrategy
import net.zomis.brainf.model.run.StepOutStrategy
import org.junit.Test

import java.util.concurrent.TimeUnit

public class BrainTest extends BrainfuckTest {

    @Test
    public void gotoCorrectEndWhile() {
        source.addCommands(">+>[-]+   ")
            .addCommands("++[-->++]-->   Find next 254 and go one step beyond it")
            .addCommands("            Loop through all 254s")
            .addCommands("+++[---         Make sure that we are not at 253 (end)")
            .addCommands("++[--<++]--	");

        assert brain.step() == BrainFCommand.NEXT
        assert brain.step() == BrainFCommand.ADD
        assert brain.step() == BrainFCommand.NEXT
        assert brain.step() == BrainFCommand.WHILE

        assert brain.code.commandIndex == 6
        assert brain.step() == BrainFCommand.ADD
    }

    @Test(timeout = 10000L)
    public void namedLoops() {
        String commands = '''
        $ nextLoop 'before'
        +++[-]
        >+[-
            $ loop 'inside'
        ]
        $ nextLoops 'nested'
        >+[++[-]+-]
        >+[-]
        $ lastLoop 'after'
        >++++[-
            $ loop 'duplicate'
        ]
        '''
        source.addCommands(commands)
        analyze(new WhileLoopAnalysis())
        analyze.print()
        println context.getLoopNames()
        cellTagsContains(analyze.cell(0), 'before')
        cellTagsContains(analyze.cell(1), 'inside')
        assert analyze.cell(2).resolveTags(context).entrySet().stream().filter({
            it.key.contains('nested') && it.key.contains('loop-begin')
        }).count() == 2 // expect two kinds of 'loop-begin nested' tags
        cellTagsContains(analyze.cell(3), 'after')
        cellTagsContains(analyze.cell(4), 'duplicate')
        assert analyze.cell(4).resolveTags(context).entrySet().stream().filter({
            it.key.contains('duplicate')
        }).mapToInt({it.key.count('duplicate')}).max().orElse(0) == 1 // do not expect any duplicate tag names
    }

    @Test(timeout = 1000L)
    public void userInputTag() {
        String commands = '++++[->,<]'
        brain = BrainF.createFromCodeAndInput(30, commands, 'INPUT')
        analyze(new IOAnalysis())
        cellTagsContains(analyze.cell(1), 'userInput')
    }

    private void cellTagsContains(MemoryCell cell, String text) {
        assert cell.toString(context).contains(text)
/*        assert cell.resolveTags(context).entrySet().stream().filter({
        }).findAny().isPresent()*/
    }

    @Test
    public void simpleLoopMultiplication() {
        source.addCommands("++[>+++<-]>>>");
        brain.run();
        assert [ 0, 6, 0, 0, 0, 0, 0, 0, 0, 0 ] == brain.getMemory().getMemoryArray(0, 10)
    }

    @Test
    public void analyzeLoops() {
        source.addCommands("++[ > +++++[>+>+++<<-]>[>+<-]<[+-+-]> +++ << -]");
        analyze(new WhileLoopAnalysis())
        IndexCounters counts = analyze.get(WhileLoopAnalysis).getWhileLoopCounts()
        assert counts.size() == 4
        assert counts[2] == [2]
        assert counts[11] == [5, 5]
        assert counts[23] == [5, 8]
        assert counts[30] == [0, 0]
    }

    @Test
    public void loopOnce() {
        source.addCommands("+[-]");
        analyze(new WhileLoopAnalysis())
        IndexCounters counts = analyze.get(WhileLoopAnalysis).getWhileLoopCounts();
        assert counts.size() == 1
        assert counts[1] == [1]
    }

    @Test
    public void printAlphabet() {
        source.addCommands("++++++[>++++++++++>++++<<-]>+++++>++[-<.+>]");
        brain.run();
        assert brain.getOutput() == "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    @Test
    public void unbalanced1() {
        source.addCommands("++[->+<] ]")
        analyze(new WhileLoopAnalysis())
        assert !analyze.get(WhileLoopAnalysis).bracketsMatching
    }

    @Test
    public void unbalanced2() {
        source.addCommands("++ [ [->+<]")
        analyze(new WhileLoopAnalysis())
        assert !analyze.get(WhileLoopAnalysis).bracketsMatching
    }

    @Test
    public void fizzBuzz() {
        source.addCommands(BrainfuckRunner.classLoader.getResource('fizzbuzz.bf').text);
        long start = System.nanoTime()
        analyzeAll()
        long stop = System.nanoTime()
        long ms = TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS)
        println "FizzBuzz analyze took $ms ms"
        assert analyze.get(CommandCountAnalysis).getActionsForCommand(BrainFCommand.WRITE) == brain.output.length()
        assert brain.output == fizzBuzzString(100)
    }

    @Test
    public void fizzBuzzMin() {
        source.addCommands(BrainfuckRunner.classLoader.getResource('fizzbuzz-min.bf').text);
        analyzeAll()
        assert analyze.get(CommandCountAnalysis).getActionsForCommand(BrainFCommand.WRITE) == brain.output.length()
        assert brain.output == fizzBuzzString(100)
    }

    @Test
    public void printedMemory() {
        source.addCommands('>.<+++.[-.]')
        analyze(new IOAnalysis())
        assert analyze.cell(0).data(IOAnalysis.CellIO).prints.toString() == '[6, 9 * 3]' // printed by code index 6 once, code index 9 thrice
        assert analyze.cell(1).data(IOAnalysis.CellIO).prints.toString() == '[1]'
    }

    @Test
    public void includeTest() {
        source.addCommands(BrainfuckRunner.classLoader.getResource('include-base.bf').text);
        brain.run()
        assert brain.memory.getMemoryArray(0, 5) == [0, 0, 12, 0, 0] as int[]
    }

    @Test
    public void stepContinueStrategy() {
        source.addCommands("+++[>+<-]-");
        assert brain.step() == BrainFCommand.ADD
        brain.run(new StepContinueStrategy())
        assert brain.step() == BrainFCommand.NEXT
        assert brain.memory.getMemory(0) == 3

        brain.run(new StepContinueStrategy())
        assert brain.code.nextCommand == BrainFCommand.NEXT
        assert brain.memory.getMemory(0) == 2
        assert brain.memory.getMemory(1) == 1

        brain.run(new StepContinueStrategy())
        assert brain.memory.getMemory(0) == 1
        assert brain.code.nextCommand == BrainFCommand.NEXT

        brain.run(new StepContinueStrategy())
        assert brain.memory.getMemory(0) == 0
        assert brain.code.nextCommand == BrainFCommand.SUBTRACT

    }

    @Test
    public void stepOutStrategy() {
        source.addCommands("+++[>+<-]-");
        assert brain.step() == BrainFCommand.ADD
        assert brain.step() == BrainFCommand.ADD
        assert brain.step() == BrainFCommand.ADD
        assert brain.run(new StepOutStrategy()) == 0
        assert brain.step() == BrainFCommand.WHILE
        assert brain.run(new StepOutStrategy()) == 15
        assert brain.memory.getMemory(1) == 3
        assert brain.code.nextCommand == BrainFCommand.SUBTRACT
    }

    static String fizzBuzzString(int max) {
        StringBuilder str = new StringBuilder()
        for (int i = 1; i <= max; i++) {
            if (i % 15 == 0) {
                str.append('FizzBuzz\n')
            } else if (i % 5 == 0) {
                str.append('Buzz\n')
            } else if (i % 3 == 0) {
                str.append('Fizz\n')
            } else {
                str.append("$i\n")
            }
        }
        str.toString()
    }

    @Test
    public void allCharacters() {
        source.addCommands(">>>>+++++++++++++++[<+++++++++++++++++>-]<[->[+>>]+[<<]>]")
        analyzeAll()
        analyze.print()
    }

    @Test
    public void readsAndWrites() {
        source.addCommands(">> +++++ [->[+>>]+[<<]>]")
        // distribute values from 5 downto 1 across the tape
        analyze(new MemoryValues(), new ReadWriteAnalysis())
        analyze.print()
        ReadWriteAnalysis readWrite = analyze.get(ReadWriteAnalysis)
        assert readWrite.maxMemory == 0x0B
        assert readWrite.cellsUsed == 7 // 5 cells gets values + 2 for loop check
        assert readWrite.maxMemory + 1 - readWrite.cellsUsed == 5 // one at start + 4 between the values
        assert analyze.arrayLong(ReadWriteAnalysis.ReadWriteData, {it.readCount}, 0, 12)  ==
            [0, 5, 6, 10, 0, 8, 0, 6, 0, 4, 0, 2] as int[]
        assert analyze.arrayLong(ReadWriteAnalysis.ReadWriteData, {it.writeCount}, 0, 12) ==
            [0, 0, 10, 5, 0, 4, 0, 3, 0, 2, 0, 1] as int[]
    }

    @Test
    public void bfGroovy() {
        source.addCommands('''+++>
$ bf '+' * 3
<---
''')
        brain.run()
        assert brain.memory.getMemory(0) == 0
        assert brain.memory.getMemory(1) == 3
        assert brain.memory.getMemory(2) == 0
    }

    @Test
    public void input() {
        BrainfuckRunner brain = BrainF.createFromCodeAndInput(BrainfuckMemory.DEFAULT_MEMORY_SIZE, "+++,.", "a");
        brain.run();
        assert "a" == brain.getOutput()
    }

    @Test
    public void simpleCommands() {
        source.addCommands("+>++>+++<")
        brain.run();
        assert brain.code.commandCount == brain.code.commandIndex
        assert 1 == brain.memory.memoryIndex
        assert 2 == brain.memory.value
        brain.perform(BrainFCommand.PREVIOUS)
        assert 1 == brain.memory.value
        brain.perform(BrainFCommand.NEXT)
        brain.perform(BrainFCommand.NEXT)
        assert 3 == brain.memory.value
    }

}
