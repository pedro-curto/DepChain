package depchain.member;

import java.util.List;

import depchain.common.CommonUtils;
import depchain.common.domain.Entity;
import depchain.member.byzantine.*;
import depchain.member.domain.Member;

public class MemberMain {

    public static void main (String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<port> <address> <memberName> [byzantineBehaviour]\"");
            System.out.println("byzantineBehaviour: 0 - no Byzantine behaviour, 1 - ignore messages, 2 - coordinated wrong state, " +
                    "3 - fake signature, 4 - replay signature, 5 - spam messages, 6 - wrong write accept, 7 - perfect link is now byzantine");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String address = args[1];
        String memberName = args[2];
        int byzantineBehaviour = args.length == 4 ? Integer.parseInt(args[3]) : 0;

        try {
            Member member = MemberBuilder.build(memberName, address, port, byzantineBehaviour);
            member.start();
            System.out.printf("Member %s started at port %d with byzantine behaviour: %b%n",
                    memberName, port, byzantineBehaviour);
        } catch (Exception e) {
            System.err.println("Error creating member: " + e.getMessage());
            System.out.println("Member could not be started due to an error during creation.");
        }
    }

}
