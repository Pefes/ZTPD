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
                "select istream data, kursOtwarcia - min(kursOtwarcia) as roznica, spolka " +
                        "from KursAkcji(spolka = 'Oracle').win:length(2) having kursOtwarcia - min(kursOtwarcia) > 0");

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

//24
//select irstream spolka as X, kursOtwarcia as Y from KursAkcji.win:length(3) WHERE spolka = 'Oracle'
//Klauzula WHERE filtruje zdarzenia po stworzeniu strumienia wynikowego

//25
//select irstream data, kursOtwarcia, spolka from KursAkcji.win:length(3) WHERE spolka = 'Oracle'

//26
//select irstream data, kursOtwarcia, spolka from KursAkcji(spolka = 'Oracle').win:length(3)

//27
//select istream data, kursOtwarcia, spolka from KursAkcji(spolka = 'Oracle').win:length(3)

//28
//select istream data, max(kursOtwarcia), spolka from KursAkcji(spolka = 'Oracle').win:length(5)

//29
//select istream data, kursOtwarcia - max(kursOtwarcia) as roznica, spolka from KursAkcji(spolka = 'Oracle').win:length(5)
//Funkcja max wyznacza maksymalną wartość znajdującą się w aktualnie przetwarzanym oknie, natomiast w języku SQL wyznacza ona maksymalną wartość dla całej tabeli/grupy

//30
//select istream data, kursOtwarcia - min(kursOtwarcia) as roznica, spolka from KursAkcji(spolka = 'Oracle').win:length(2) having kursOtwarcia - min(kursOtwarcia) > 0
//Tak, wynik jest poprawny