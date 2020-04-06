import java.io.FileNotFoundException;
import java.io.PrintWriter;

import com.ibm.eserver.zos.racf.IRRPassTicket;
import com.ibm.eserver.zos.racf.IRRPassTicketGenerationException;
import com.ibm.os390.security.PlatformThread;

public class PtGen {
    public static void main(String args[]) {
        IRRPassTicket passTicketService;
        String userid = args[0];
        String applid = args[1];

        try {
            System.out.println("activeUserid=" + PlatformThread.getUserName());
            passTicketService = new IRRPassTicket();
            System.out.println("userid=" + userid + " applid=" + applid);
            String passTicket = passTicketService.generate(userid, applid);
            System.out.println("New PassTicket: " + passTicket);
            String passTicket2 = passTicketService.generate(userid, applid);
            System.out.println("New PassTicket 2: " + passTicket2);

            try (PrintWriter out = new PrintWriter(".passticket")) {
                out.print(passTicket);
            }
        } catch (IRRPassTicketGenerationException | FileNotFoundException e) {
            System.out.println("Generation failed: " + e);
        }
    }
}
