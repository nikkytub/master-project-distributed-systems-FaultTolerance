package wikiedits;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

public class ForestTemperatureWithCheckpoints {
    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);

        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);
        env.enableCheckpointing(3000);
        env.getCheckpointConfig().setCheckpointingMode(
                CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.setStateBackend(new FsStateBackend("file:///tmp/flink/checkpoints"));
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                3, // number of restart attempts
                5000 // time in milliseconds between restarts
        ));
        //DataStream<String> dataStream = env
        //       .socketTextStream("localhost", 9999);
        DataStream<String> dataStream = TemperatureStream.getDataStream(env, params);
        if (dataStream == null) {
            System.exit(1);
            return;
        }
        DataStream<String> averageTemperatureStream = dataStream
                .map(new Temperature())
                .keyBy(0)
                .flatMap(new AverageTemperature());
        averageTemperatureStream.print();
        env.execute("Forest Temperature");
    }



    public static class Temperature implements MapFunction<String, Tuple2<Integer, Double>> {

        public Tuple2<Integer, Double> map(String temp)
                throws Exception {
            try {
                return Tuple2.of(1, Double.parseDouble(temp));
            } catch (Exception excep) {
                System.out.println(excep);
            }

            return null;
        }
    }

    public static class AverageTemperature
            extends RichFlatMapFunction<Tuple2<Integer, Double>, String> {

        private transient ValueState<Tuple2<Integer, Double>> countSum;

        public void flatMap(Tuple2<Integer, Double> input, Collector<String> out)
                throws Exception {
            Tuple2<Integer, Double> currentCountSum = countSum.value();

            //To print if temperature exceeds the critical limit
            if (input.f1 >= 50) {
                out.collect(String.format(
                        "DANGER!!! The average temperature of the last %s "
                                + "reading(s) was %s degree celsius, now temperature is %s degree celsius",
                        currentCountSum.f0,
                        currentCountSum.f1 / currentCountSum.f0,
                        input.f1));
                countSum.clear();
                currentCountSum = countSum.value();
            } else {
                out.collect("Temperature is under critical limit!");
            }
            currentCountSum.f0 += 1;
            currentCountSum.f1 += input.f1;
            countSum.update(currentCountSum);
        }

        public void open(Configuration config) {
            ValueStateDescriptor<Tuple2<Integer, Double>> descriptor =
                    new ValueStateDescriptor<Tuple2<Integer, Double>>(
                            //state name
                            "forestAverageTemperature",
                            TypeInformation.of(
                                    new TypeHint<Tuple2<Integer, Double>>() { }),
                            Tuple2.of(0, 0.0));
            countSum = getRuntimeContext().getState(descriptor);
        }
    }
}
