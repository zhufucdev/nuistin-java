import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.javamasters.cipher.NoCipher;
import com.javamasters.data.AccountJsonProvider;
import com.javamasters.data.io.MemoryIO;
import com.javamasters.model.Account;
import com.javamasters.model.ISP;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountJsonProviderTest {

    @Test
    void getAccountIds() {
        var io = new MemoryIO();
        var cipher = new NoCipher();
        var provider = new AccountJsonProvider(io, cipher);
        var accounts =
                Stream.generate(() -> new Account(UUID.randomUUID().toString(), UUID.randomUUID().toString(), ISP.Campus))
                        .limit(10).toList();
        for (var account : accounts) {
            assertTrue(provider.addAccount(account).blockingGet());
        }
        assertArrayEquals(accounts.stream().map(Account::id).toArray(), provider.getAccountIds().blockingGet().toArray());
    }

    @Test
    void addAccount() {
        var io = new MemoryIO();
        var ciper = new NoCipher();
        var provider = new AccountJsonProvider(io, ciper);
        var account = new Account("123", "456", ISP.Campus);
        assertTrue(provider.addAccount(account).blockingGet());

        var gson = new Gson();
        try (var ips = io.openInputStream()) {
            ArrayList<Account> list = gson.fromJson(new InputStreamReader(ips), new TypeToken<List<Account>>() {}.getType());
            assertEquals(account, list.get(0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}