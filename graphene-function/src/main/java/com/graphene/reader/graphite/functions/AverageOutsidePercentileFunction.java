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
import java.util.Collections;
import java.util.List;

/**
 * @author Andrei Ivanov
 */
public class AverageOutsidePercentileFunction extends GrapheneFunction {


    public AverageOutsidePercentileFunction(String text) {
        super(text, "averageOutsidePercentile");
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

        //todo: optimize
        List<Double> averages = new ArrayList<>();
        for(TimeSeries ts : processedArguments) {
            averages.add(CollectionUtils.average(Arrays.asList(ts.getValues())));
        }

        Double lowPercentile = CollectionUtils.percentile(averages, Math.min(100 - threshold, threshold), false);
        Double highPercentile = CollectionUtils.percentile(averages, Math.max(100 - threshold, threshold), false);

        if (lowPercentile == null || highPercentile == null) return Collections.emptyList();

        List<TimeSeries> result = new ArrayList<>();

        for(TimeSeries ts : processedArguments) {
            Double average = CollectionUtils.average(Arrays.asList(ts.getValues()));

            if (average != null && !(average > lowPercentile && average < highPercentile)) {
                result.add(ts);
            }
        }

        return result;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() != 2) throw new InvalidArgumentException("averageOutsidePercentile: number of arguments is " + arguments.size() + ". Must be one.");
        if (!(arguments.get(0) instanceof Target)) throw new InvalidArgumentException("averageOutsidePercentile: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
        if (!(arguments.get(1) instanceof Double)) throw new InvalidArgumentException("averageOutsidePercentile: argument is " + arguments.get(1).getClass().getName() + ". Must be a number");
    }
}
