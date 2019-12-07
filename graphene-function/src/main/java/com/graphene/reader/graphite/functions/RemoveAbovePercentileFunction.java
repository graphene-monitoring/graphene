package com.graphene.reader.graphite.functions;

import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.utils.CollectionUtils;
import com.graphene.reader.utils.TimeSeriesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrei Ivanov
 */
public class RemoveAbovePercentileFunction extends GrapheneFunction {


    public RemoveAbovePercentileFunction(String text) {
        super(text, "removeAbovePercentile");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        List<TimeSeries> processedArguments = new ArrayList<>();
        processedArguments.addAll(evaluator.eval((Target) arguments.get(0)));

        if (processedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        double threshold = (Double) arguments.get(1);
        int length = processedArguments.get(0).getValues().length;

        for(TimeSeries ts : processedArguments) {
            Double percentile = CollectionUtils.percentile(Arrays.asList(ts.getValues()), threshold, false);

            if (percentile != null) {
                for (int i = 0; i < length; i++) {
                    if (ts.getValues()[i] != null && ts.getValues()[i] > percentile) {
                        ts.getValues()[i] = null;
                    }
                }
            }
            ts.setName("removeAboveValue(" + ts.getName() + "," + threshold + ")");
        }

        return processedArguments;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() != 2) throw new InvalidArgumentException("removeAbovePercentile: number of arguments is " + arguments.size() + ". Must be one.");
        if (!(arguments.get(0) instanceof Target)) throw new InvalidArgumentException("removeAbovePercentile: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
        if (!(arguments.get(1) instanceof Double)) throw new InvalidArgumentException("removeAbovePercentile: argument is " + arguments.get(1).getClass().getName() + ". Must be a number");
    }
}
