package wikiedits;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TemperatureStream {

    public static DataStream<String> getDataStream(
            StreamExecutionEnvironment env, final ParameterTool params) {
        DataStream<String> temperatureStream = null;
        if (params.has("input")) {
            System.out.println("Executing Forest temperature values with file input");
            temperatureStream = env.readTextFile(params.get("input"));
        } else if (params.has("host") && params.has("port")) {
            System.out.println("Executing Forest temperature with socket stream");
            temperatureStream = env.socketTextStream(
                    params.get("host"), Integer.parseInt(params.get("port")));
        } else {
            System.out.println("Use --host and --port to specify socket");
            System.out.println("Use --input to specify file input");
        }

        return temperatureStream;
    }
}
