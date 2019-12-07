package com.graphene.reader.graphite.functions;

import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.PathTarget;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.utils.CollectionUtils;
import com.graphene.reader.utils.TimeSeriesUtils;
import com.graphene.reader.beans.TimeSeries;
import com.graphene.common.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Andrei Ivanov
 */
public class StdevFunction extends GrapheneFunction {

    public StdevFunction(String text) {
        super(text, "stdev");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        List<TimeSeries> processedArguments = new ArrayList<>();
        processedArguments.addAll(evaluator.eval((Target) arguments.get(0)));

        if (processedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        int length = processedArguments.get(0).getValues().length;
        int step = processedArguments.get(0).getStep();

        // need to get window in number of data points
        long window;
        if (arguments.get(1) instanceof Double) {
            window = ((Double) arguments.get(1)).longValue();
        } else {
            long offset = Math.abs(DateTimeUtils.INSTANCE.parseTimeOffset((String) arguments.get(1)));
            window = offset / step;
        }

        for (TimeSeries ts : processedArguments) {
            Queue<Double> queue = new LinkedList<>();
            Double[] values = new Double[length];

            for (int i = 0; i < length; i++) {
                if (queue.size() == 0) {
                    values[i] = 0.;
                } else {
                    values[i] = CollectionUtils.stdev(queue);
                }

                if (ts.getValues()[i] != null) {
                    queue.offer(ts.getValues()[i]);
                }

                if (queue.size() > window) {
                    queue.remove();
                }
            }

            ts.setValues(values);
            ts.setName("stdev(" + ts.getName() + "," + window + ")");
        }

        return processedArguments;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() != 2) throw new InvalidArgumentException("stdev: number of arguments is " + arguments.size() + ". Must be two.");
        if (!(arguments.get(0) instanceof PathTarget)) throw new InvalidArgumentException("stdev: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
        if (!(arguments.get(1) instanceof Double) && !(arguments.get(1) instanceof String)) throw new InvalidArgumentException("stdev: argument is " + arguments.get(1).getClass().getName() + ". Must be a number or a string");
    }
}
