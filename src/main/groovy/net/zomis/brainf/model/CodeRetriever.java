package net.zomis.brainf.model;

public interface CodeRetriever {

    BrainfuckCommand getCommand(int commandIndex);

    default int getCommandLength(int commandIndex) {
        return 1;
    }

    int capacity();
}
