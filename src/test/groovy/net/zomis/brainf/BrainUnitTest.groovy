package net.zomis.brainf

import net.zomis.brainf.model.BrainF
import net.zomis.brainf.model.BrainfuckRunner
import net.zomis.brainf.model.ListCode
import org.junit.Before
import org.junit.Test

class BrainUnitTest {

    BrainfuckRunner runner

    @Before
    void setup() {
        runner = BrainF.createWithDefaultSize()
    }

    @Test
    void assertingValue() {
        addCommands('''
            ++++
            $ assert value == 4
            $ assert value == 5
        ''')
        expectFailure('value == 5')
    }

    @Test
    void assertingPosition() {
        addCommands('''
            >>
            $ assert position == 2
            $ assert position == 1
        ''')
        expectFailure('position == 1')
    }

    @Test
    void assertingMemoryArray() {
        addCommands('''
             +
            >++
            >+++
            >++++
            >+++++
            >+++++ +
            $ {
                def arr = memory 6 offsetBackward 5
                def exp = values 1 2 3 4 5 6
                assert arr == exp
                assert false
            }
        ''')
        expectFailure('assert false')
    }

    void addCommands(String code) {
        runner.code.source = ListCode.create(code)
    }

    void expectFailure(String expectedContains) {
        try {
            runner.run()
            assert false : 'No assertion error was thrown'
        } catch (AssertionError error) {
            assert error.getMessage().contains(expectedContains)
        }
    }

}
