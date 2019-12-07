package com.graphene.reader.graphite.functions;

import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.PathTarget;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.utils.CollectionUtils;
import com.graphene.reader.utils.TimeSeriesUtils;

import java.util.*;

/**
 * @author Andrei Ivanov
 */
public class UseSeriesAboveFunction extends GrapheneFunction {


    public UseSeriesAboveFunction(String text) {
        super(text, "useSeriesAbove");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        double number = (Double) arguments.get(1);
        if (number <= 0) return Collections.emptyList();

        String search = (String) arguments.get(2);
        String replace = (String) arguments.get(3);

        List<TimeSeries> processedArguments = new ArrayList<>();
        PathTarget target = (PathTarget) arguments.get(0);
        processedArguments.addAll(evaluator.eval(target));

        List<TimeSeries> newProcessedArguments = new ArrayList<>();
        PathTarget newPathTarget = new PathTarget(
                target.getText().replaceAll(search, replace),
                target.getContext(),
                target.getPath().replaceAll(search, replace),
                target.getTenant(),
                target.getFrom(),
                target.getTo());
        newProcessedArguments.addAll(evaluator.eval(newPathTarget));


        if (processedArguments.size() == 0 || newProcessedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        if (!TimeSeriesUtils.checkAlignment(newProcessedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        List<TimeSeries> result = new ArrayList<>();
        Set<String> passedSeries = new HashSet<>();


        for(TimeSeries ts : processedArguments) {
            Double v = CollectionUtils.max(Arrays.asList(ts.getValues()));
            if (v != null && v > number) {
                passedSeries.add(ts.getName().replaceAll(search, replace));
            }
        }

        for(TimeSeries ts : newProcessedArguments) {
            if (passedSeries.contains(ts.getName())) {
                result.add(ts);
            }
        }

        return result;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() != 4) throw new InvalidArgumentException("useSeriesAbove: number of arguments is " + arguments.size() + ". Must be 4.");
        if (!(arguments.get(0) instanceof PathTarget)) throw new InvalidArgumentException("useSeriesAbove: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
        if (!(arguments.get(1) instanceof Double)) throw new InvalidArgumentException("useSeriesAbove: argument is " + arguments.get(1).getClass().getName() + ". Must be a number");
        if (!(arguments.get(2) instanceof String)) throw new InvalidArgumentException("useSeriesAbove: argument is " + arguments.get(2).getClass().getName() + ". Must be a string");
        if (!(arguments.get(3) instanceof String)) throw new InvalidArgumentException("useSeriesAbove: argument is " + arguments.get(3).getClass().getName() + ". Must be a string");
    }
}
