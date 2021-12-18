import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(KursAkcji.class);
        EPRuntime epRuntime = EPRuntimeProvider.getDefaultRuntime(configuration);

        EPDeployment deployment = compileAndDeploy(epRuntime,
                "select istream data, spolka, obrot " +
                        "from KursAkcji(market='NYSE').win:ext_timed_batch(data.getTime(), 7 days) order by obrot desc limit 1 offset 2");

        ProstyListener prostyListener = new ProstyListener();
        for (EPStatement statement : deployment.getStatements()) {
            statement.addListener(prostyListener);
        }

        InputStream inputStream = new InputStream();
        inputStream.generuj(epRuntime.getEventService());
    }

    public static EPDeployment compileAndDeploy(EPRuntime epRuntime, String epl) {
        EPDeploymentService deploymentService = epRuntime.getDeploymentService();
        CompilerArguments args = new CompilerArguments(epRuntime.getConfigurationDeepCopy());
        EPDeployment deployment;

        try {
            EPCompiled epCompiled = EPCompilerProvider.getCompiler().compile(epl, args);
            deployment = deploymentService.deploy(epCompiled);
        } catch (EPCompileException e) {
            throw new RuntimeException(e);
        } catch (EPDeployException e) {
            throw new RuntimeException(e);
        }

        return deployment;
    }
}

//4a
//Ponieważ musiały zostać usunięte z okna dopiero wtedy, gdy pojawiło się zdarzenie, które zmieniało aktualnie przetwarzane okno, tj. zdarzenie z datą 17.10 (usunięte nie mieściły się w oknie 7-dniowym)

//4b
//Brakuje zdarzeń ISTREAM, ponieważ ostatni (trzeci) batch nie został zarejestrowany (okno czasowe 19.09-25.09), z tego wzlędu, że nie pojawiło się nigdy zdarzenie z datą późniejszą jak granica okna (25.09). Z tego też względu jest mniej zdarzeń RSTREAM, ponieważ zdarzenia nie musiały zostać usunięte z okna drugiego batcha.

//5
//select istream data, kursZamkniecia, spolka, max(kursZamkniecia) - kursZamkniecia as roznica from KursAkcji.win:ext_timed_batch(data.getTime(), 1 day)

//6
//select istream data, kursZamkniecia, spolka, max(kursZamkniecia) - kursZamkniecia as roznica from KursAkcji(spolka in ('IBM', 'Honda', 'Microsoft')).win:ext_timed_batch(data.getTime(), 1 day)

//7a
//select istream data, kursZamkniecia, spolka, kursOtwarcia from KursAkcji(kursZamkniecia > kursOtwarcia).win:length(1)

//7b
//select istream data, kursZamkniecia, spolka, kursOtwarcia from KursAkcji(KursAkcji.wzrost(kursZamkniecia,kursOtwarcia)).win:length(1)

//8
//select istream data, spolka, kursZamkniecia, max(kursZamkniecia) - kursZamkniecia as roznica from KursAkcji(spolka in ('PepsiCo', 'CocaCola')).win:ext_timed(data.getTime(), 7 days)

//9
//select istream data, spolka, kursZamkniecia from KursAkcji(spolka in ('PepsiCo', 'CocaCola')).win:ext_timed_batch(data.getTime(), 1 day) having kursZamkniecia = max(kursZamkniecia)

//10
//select istream kursZamkniecia as maksimum from KursAkcji().win:ext_timed_batch(data.getTime(), 7 days) having kursZamkniecia = max(kursZamkniecia)

//11
//select istream c.kursZamkniecia as kursCoc, p.data, p.kursZamkniecia as kursPep from KursAkcji(spolka='CocaCola').win:length(1) as c join KursAkcji(spolka='PepsiCo').win:length(1) as p on c.data=p.data where c.kursZamkniecia < p.kursZamkniecia

//12
//select istream k.data, k.kursZamkniecia as kursBiezacy, k.spolka, k.kursZamkniecia - x.kursZamkniecia as roznica from KursAkcji(spolka in ('CocaCola', 'PepsiCo')).win:length(1) as k join KursAkcji(spolka in ('PepsiCo', 'CocaCola')).std:firstunique(spolka) as x on k.spolka = x.spolka

//13
//select istream k.data, k.kursZamkniecia as kursBiezacy, k.spolka, k.kursZamkniecia - x.kursZamkniecia as roznica from KursAkcji.win:length(1) as k join KursAkcji.std:firstunique(spolka) as x on k.spolka = x.spolka where k.kursZamkniecia > x.kursZamkniecia

//14
//select istream k.data as dataB, x.data as dataA, x.kursOtwarcia as kursA, k.kursOtwarcia as kursB, k.spolka from KursAkcji.win:ext_timed(data.getTime(), 7 days) as k join KursAkcji.win:ext_timed(data.getTime(), 7 days) as x on k.spolka = x.spolka where k.kursOtwarcia - x.kursOtwarcia > 3

//15
//select istream data, spolka, obrot from KursAkcji(market='NYSE').win:ext_timed_batch(data.getTime(), 7 days) order by obrot desc limit 3

//16
//select istream data, spolka, obrot from KursAkcji(market='NYSE').win:ext_timed_batch(data.getTime(), 7 days) order by obrot desc limit 1 offset 2