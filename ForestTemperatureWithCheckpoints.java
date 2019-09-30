package org.apache.flink.edge.examples.wikiedits;

import org.apache.flink.streaming.api.datastream.ResourceClassification;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
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
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;


public class ForestTemperatureWithCheckpoints {
    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);
        boolean checkpoint = false;
        boolean external = false;
        boolean operatorChain = false;
        final String statePath;

        statePath = params.get("fsStatePath", "hdfs://hadoop:9000/flink-checkpoints");

        if (params.has("checkpoint")){
            checkpoint = params.getBoolean("checkpoint");
        }

        if (params.has("external")){
            external = params.getBoolean("external");
        }

        if (params.has("operatorChain")){
            operatorChain = params.getBoolean("operatorChain");
        }

        if(operatorChain) {
            // disable chaining to get more tasks for the scheduling
            env.disableOperatorChaining();
        }

        if (external) {
            env.getConfig().setScheduleExternal(true);
        }

        if (checkpoint) {
            long checkpointInterval = 5000;
            CheckpointingMode checkpointMode = CheckpointingMode.EXACTLY_ONCE;
            env.enableCheckpointing(checkpointInterval, checkpointMode);
            env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);

            if (statePath != null) {
                // setup state and checkpoint mode
                env.setStateBackend(new FsStateBackend(statePath));
            }
            env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);

            env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                    3, // number of restart attempts
                    5000 // time in milliseconds between restarts
            ));
        }

        //DataStream<String> dataStream = env
        //       .socketTextStream("localhost", 9999);

        DataStream<String> dataStream = getDataStream(env, params);
        if (dataStream == null) {
            System.exit(1);
            return;
        }
        dataStream.setRateModificationFactor(1);
        dataStream.setDataModificationFactor(1);
        dataStream.setResourceNeed(ResourceClassification.LOW);


        DataStream<Tuple2<Integer, Double>> splitTemperatureStream = dataStream.map(new Temperature()).name("TemperatureSplitter").keyBy(0);

        splitTemperatureStream.setDataModificationFactor(1);
        splitTemperatureStream.setRateModificationFactor(0.99);
        splitTemperatureStream.setResourceNeed(ResourceClassification.MEDIUM);


        DataStream<String> averageTemperatureStream = splitTemperatureStream.flatMap(new AverageTemperature()).name("TemperatureAverager");

        averageTemperatureStream.setDataModificationFactor(0.001);
        averageTemperatureStream.setRateModificationFactor(1);
        averageTemperatureStream.setResourceNeed(ResourceClassification.HIGH);

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

    public static DataStream<String> getDataStream(
            StreamExecutionEnvironment env, final ParameterTool params) {

        DataStream<String> temperatureStream = null;

        if (params.has("input")) {
            System.out.println("Executing Forest temperature values with file input");
            temperatureStream = env.readTextFile(params.get("input")).name("TemperatureInputSource");

            temperatureStream.setResourceNeed(ResourceClassification.LOW);

        } else if (params.has("host") && params.has("port")) {
            System.out.println("Executing Forest temperature with socket stream");
            temperatureStream = env.socketTextStream(params.get("host"), Integer.parseInt(params.get("port"))).name("TemperatureSocketSource");

            temperatureStream.setResourceNeed(ResourceClassification.LOW);

        } else {
            System.out.println("Executing WindowWordCount example with default input data set.");
            System.out.println("Use --input to specify file input and --host / --port for socket");
            // get default test text data

            temperatureStream = env.fromElements(Temperatures.Temps).name("TemperatureDefaultSource");
            temperatureStream.setOutputItemSize(1000);
            temperatureStream.setOutputRate(10);
        }

        return temperatureStream;
    }
}

