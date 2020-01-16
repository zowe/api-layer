import com.ibm.eserver.zos.racf.IRRPassTicket;
import com.ibm.eserver.zos.racf.IRRPassTicketEvaluationException;

public class PtEval {
    public static void main(String args[]) {
        IRRPassTicket passTicketService = new IRRPassTicket();
        String userid = args[0];
        String applid = args[1];
        String passTicket = args[2];

        try {
            System.out.println("userid=" + userid + " applid=" + applid + " passTicket=" + passTicket);
            passTicketService.evaluate(userid, applid, passTicket);
            System.out.println("PassTicket is valid (1st evaluation)");

            passTicketService.evaluate(userid, applid, passTicket);
            System.out.println("PassTicket is valid (2nd evaluation)");
        } catch (IRRPassTicketEvaluationException e) {
            System.out.println("PassTicket evaluation failed: " + e);
        }
    }
}
