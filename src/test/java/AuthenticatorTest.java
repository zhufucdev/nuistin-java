import com.javamasters.model.Account;
import com.javamasters.model.ISP;
import com.javamasters.net.Authenticator;
import org.junit.jupiter.api.Test;

import java.net.NetworkInterface;
import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatorTest {

    @Test
    void login() throws SocketException {
        var server = System.getenv("SERVER");
        var id = System.getenv("ID");
        var password = System.getenv("PASSWORD");
        var isp = ISP.valueOf(System.getenv("ISP"));
        var nicName = System.getenv("NIC");

        var auth = new Authenticator(server, NetworkInterface.getByName(nicName));
        assertTrue(auth.login(new Account(id, password, isp)).blockingGet());
        assertEquals(auth.getState().blockingFirst(), Authenticator.State.Online);
    }
}