package depchain.member.state;

import java.util.ArrayList;

public class StringChain {

    private ArrayList<String> stringChain;

    public StringChain(ArrayList<String> stringChain) {
        this.stringChain = stringChain;
    }

    public synchronized void appendString(String str) {
        stringChain.add(str);
        System.err.println("[STRINGCHAIN]: " + stringChain);
    }

    public boolean contains(String str) {
        return stringChain.contains(str);
    }
}
