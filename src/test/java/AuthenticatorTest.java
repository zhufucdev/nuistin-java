import com.javamasters.model.Account;
import com.javamasters.model.ISP;
import com.javamasters.net.Authenticator;
import org.junit.jupiter.api.Test;

import java.net.NetworkInterface;
import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticatorTest {
    String server = System.getenv("SERVER");
    String id = System.getenv("ID");
    String password = System.getenv("PASSWORD");
    ISP isp = ISP.valueOf(System.getenv("ISP"));
    String nicName = System.getenv("NIC");

    @Test
    void login_logout() throws SocketException {
        var auth = new Authenticator(server, NetworkInterface.getByName(nicName));
        assertTrue(auth.login(new Account(id, password, isp)).blockingGet());
        assertEquals(Authenticator.State.Online, auth.getState().blockingLast());

        assertTrue(auth.logout().blockingGet());
    }

    @Test
    void state() throws SocketException {
        var auth = new Authenticator(server, "google.com", NetworkInterface.getByName(nicName));
        assertNotEquals(Authenticator.State.Unspecified, auth.getState().blockingFirst());
    }
}